# Decisões Técnicas

## Principais decisões

### Stack
- **Spring Boot 3.5 / Java 17** — stack atual, LTS, padrão de mercado e da própria MV.
- **Maven Wrapper** — projeto roda sem Maven instalado.
- **H2 como banco padrão de dev, Oracle compatível via perfil** — atende o requisito mínimo (banco relacional) e o diferencial (Oracle), sem exigir infraestrutura para o avaliador rodar.
- **Flyway** — migrations versionadas. Mostro maturidade e mantém o schema sob controle desde o primeiro commit.
- **Lombok** — aplicado nas entidades (`@Getter`, `@NoArgsConstructor`) para reduzir boilerplate sem prejudicar a clareza. Optei por **manter `equals/hashCode` manuais** porque o `@EqualsAndHashCode` do Lombok, por padrão, usa todos os campos — o que quebra entidades JPA (o hashCode muda quando o `id` é atribuído no flush). O Lombok está configurado como `optional` no Maven e excluído do JAR final via `spring-boot-maven-plugin`.

### Arquitetura
- **Camadas clássicas:** `controller → service → repository → domain`. Para um CRUD pequeno, hexagonal/clean architecture seria overengineering.
- **DTOs separados das entidades JPA** (records). Evita vazar lazy proxies do Hibernate, permite versionar API sem mexer no banco e dá controle exato sobre o JSON.
- **Entidades sem setters públicos.** Toda mudança de estado passa por método de domínio (`agendamento.cancelar(motivo)`). Evita objeto anêmico e centraliza invariantes.
- **`hashCode/equals` baseado em `id`** com null-check — evita o bug clássico de mutação de hashCode quando o JPA atribui o id no flush.

### Regras de negócio
- **Conflito de horário tem defesa em camadas:**
  1. `existsByProfissional...` no service para a mensagem amigável no caso comum.
  2. **Índice único parcial no banco** (em ambos H2 e Oracle, com sintaxes diferentes) para fechar a janela de race condition entre dois POSTs simultâneos.
  3. `try/catch DataIntegrityViolationException` no service mapeia a violação para `409 Conflict`.

  Os três juntos. Validar só no service deixa um buraco; validar só no banco devolve uma mensagem ruim no caso comum.

- **`@Future` no DTO + checagem com `Clock` no service** para "data no passado". O `Clock` é injetado como `@Bean` para que os testes usem `Clock.fixed()` e não dependam do relógio do sistema.

- **Cancelamento via `PATCH`** (não `DELETE`) deixa explícito que o registro é mantido — só muda estado.

- **CPF é normalizado** (remove pontos/traços) antes de salvar, evitando duplicatas por formato diferente.

### Tratamento de erros
- `@RestControllerAdvice` global mapeia exceptions de domínio (`NotFoundException`, `BusinessException`, `ConflictException`) para HTTP semântico (404/422/409). Erros de Bean Validation viram `400` com lista de campos inválidos. Resposta padronizada via record `ApiError`.

### Filtros dinâmicos
- `JpaSpecificationExecutor` + `Specification` para os filtros opcionais (paciente/profissional/status). Mais limpo que escrever 7 métodos `findByXAndY...` ou montar JPQL string.

### Compatibilidade H2 ↔ Oracle
Optei por **migrations totalmente separadas por banco** (`db/migration/h2/` e `db/migration/oracle/`) ao invés de buscar uma sintaxe "comum":

- **V1 (schema):** H2 usa `BIGINT` + `VARCHAR`; Oracle usa `NUMBER(19,0)` + `VARCHAR2`. Tentei inicialmente um único V1 com `BIGINT`, mas o Oracle 21c rejeita `BIGINT` em colunas `IDENTITY` (`ORA-30675`) — embora aceite como coluna comum. A lição: portabilidade SQL entre bancos é uma ilusão em features avançadas (IDENTITY, function-based index).
- **V2 (índice único parcial):** H2 não suporta expressões em `CREATE INDEX`, então uso **coluna gerada** codificando `(profissional_id, data_hora)` para registros não-cancelados; Oracle usa **function-based index** com `DECODE`. Mesma semântica, sintaxe diferente.
- **V3 (seed):** comum, porque `INSERT` é portátil.

---

## O que priorizei

1. **Regras de negócio corretas e testáveis** — todas as 5 cobertas por testes unitários, incluindo cenários de borda (idempotência do cancelamento, race condition).
2. **Documentação clara** — README com instruções, exemplos `curl`, mapeamento HTTP, e este DECISOES.md justificando escolhas.
3. **Código defensivo no ponto certo** — defesa em camadas para conflito de horário, sem virar burocracia em outros lugares.
4. **Diferenciais que conversam com a MV** — perfil Oracle real, índice único Oracle-style, Flyway, Swagger.

## O que ficou de fora (consciente)

- **Autenticação/autorização** — fora do escopo do desafio. Spring Security adicionaria complexidade sem agregar à avaliação.
- **CRUD completo de profissional** — não pedido. Apenas seed com 3 profissionais para facilitar testes manuais.
- **Paginação** — não pedido. `findAll` direto, simples. Em produção, adicionaria `Pageable`.
- **Frontend completo com autenticação** — fiz uma interface React + TypeScript + Vite cobrindo todos os endpoints, mas mantive simples (sem login, sem rotas, sem state management externo). O foco do diferencial era demonstrar consumo da API, não construir um SPA completo.
- **Soft delete vs cancelamento** — propositalmente não implementei "delete" porque o PRD diz que o registro deve ser mantido. PATCH explicita isso melhor.

## Uso de IA

Usei o Claude Code para acelerar partes específicas:

- **Debug do erro `ORA-30675`** — quando descobri que `BIGINT` não funciona em colunas `IDENTITY` no Oracle 21c (apesar de funcionar como tipo comum). A IA me ajudou a entender a causa raiz e a separar as migrations por banco.
- **Configuração do Flyway com `baseline-version=0`** — quando o Flyway estava pulando o V1 no Oracle por causa do baseline padrão começar em 1.
- **Revisão de mensagens de erro** e do próprio texto deste DECISOES.md.
- **Testei manualmente cada endpoint** via Swagger e via o frontend React, além dos 14 testes automatizados, pra garantir que entendo o que está rodando.

**Como validei:**
- Todos os 14 testes rodam verdes localmente.
- Subi a aplicação e exercitei manualmente os endpoints via Swagger UI para validar o fluxo feliz e os casos de erro.
- Conferi visualmente o schema gerado pelo Flyway no H2 Console.
- Reli cada arquivo gerado para garantir que entendo o que está lá — não copio código que não consigo defender em entrevista.
