# API de Agendamento Clínico

API REST em Java/Spring Boot para controle de agendamentos de consultas em uma clínica.

Desenvolvida como parte do desafio técnico para a vaga de Desenvolvedor Júnior na MV.

---

## Stack

- **Java 17** + **Spring Boot 3.5**
- **Spring Web**, **Spring Data JPA**, **Bean Validation**
- **Flyway** para versionamento de schema
- **H2** em memória (perfil `h2`, padrão) e **Oracle** (perfil `oracle`)
- **Springdoc OpenAPI** (Swagger UI)
- **JUnit 5** + **Mockito** + **MockMvc** para testes
- **Maven Wrapper** (`mvnw`) — não precisa ter Maven instalado

---

## Como executar

### Pré-requisitos
- Java 17 ou superior

### Rodar com H2 (modo padrão — não precisa de banco)
```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **H2 Console:** http://localhost:8080/h2-console
  (JDBC URL: `jdbc:h2:mem:agendamento`, user: `sa`, sem senha)

### Rodar com Oracle
1. Suba um Oracle XE local (ex: via Docker):
   ```bash
   docker run -d -p 1521:1521 -e ORACLE_PASSWORD=oracle gvenzl/oracle-xe:21-slim-faststart
   ```
2. Inicie a aplicação no perfil `oracle`:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=oracle
   ```
   Variáveis disponíveis: `DB_USER` (default `system`), `DB_PASSWORD` (default `oracle`).

### Rodar os testes
```bash
./mvnw test
```

---

## Endpoints

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/api/pacientes` | Cadastra paciente |
| `GET` | `/api/pacientes` | Lista pacientes |
| `POST` | `/api/agendamentos` | Cria agendamento |
| `GET` | `/api/agendamentos?pacienteId=&profissionalId=&status=` | Lista agendamentos com filtros opcionais |
| `PATCH` | `/api/agendamentos/{id}/cancelar` | Cancela agendamento (body com `motivo`) |

A lista completa, com schemas e exemplos, está no Swagger UI.

### Exemplos rápidos

**Cadastrar paciente:**
```bash
curl -X POST http://localhost:8080/api/pacientes \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Souza","cpf":"98765432100","email":"maria@test.com"}'
```

**Criar agendamento** (use um `profissionalId` existente — o seed cria os de id 1, 2 e 3):
```bash
curl -X POST http://localhost:8080/api/agendamentos \
  -H "Content-Type: application/json" \
  -d '{"pacienteId":1,"profissionalId":1,"dataHora":"2026-12-15T10:00:00","tipoAtendimento":"CONSULTA"}'
```

**Cancelar agendamento:**
```bash
curl -X PATCH http://localhost:8080/api/agendamentos/1/cancelar \
  -H "Content-Type: application/json" \
  -d '{"motivo":"Paciente remarcou"}'
```

---

## Regras de negócio implementadas

1. **Profissional não pode ter dois agendamentos no mesmo horário.**
   Defesa em camadas: validação no service + índice único parcial no banco que ignora linhas canceladas. Em caso de race condition, o `DataIntegrityViolationException` do banco é convertido em `409 Conflict`.

2. **Não permite agendamento no passado.**
   Validado no DTO (`@Future`) e no service (com `Clock` injetado para testabilidade).

3. **Cancelamento exige motivo.**
   Bean Validation no DTO (`@NotBlank`, 3 a 500 caracteres).

4. **Cancelar muda status para `CANCELADO` e mantém o registro.**
   Operação `PATCH` (não `DELETE`). O registro continua na base, com `motivo_cancelamento` e `cancelado_em` preenchidos.

5. **Listagem aceita filtros por paciente, profissional e status.**
   Implementado com `JpaSpecificationExecutor` + `Specification` (filtros opcionais e combináveis).

---

## Códigos HTTP

| Código | Quando |
|---|---|
| `200 OK` | Listagem ou cancelamento bem-sucedido |
| `201 Created` | Cadastro de paciente / criação de agendamento |
| `400 Bad Request` | JSON inválido ou erro de Bean Validation |
| `404 Not Found` | Paciente / profissional / agendamento inexistente |
| `409 Conflict` | CPF duplicado ou conflito de horário |
| `422 Unprocessable Entity` | Regra de negócio violada (ex: cancelar já cancelado) |
| `500 Internal Server Error` | Erro inesperado |

---

## Estrutura do projeto

```
src/main/java/com/clinica/agendamento/
├── domain/         entidades JPA + enums
├── dto/            request/response (records)
├── repository/     Spring Data + Specifications
├── service/        regras de negócio
├── controller/     endpoints REST
├── exception/      exceptions de domínio + handler global
└── config/         OpenAPI, Clock

src/main/resources/db/migration/
├── common/         migrations compatíveis (V1 schema, V3 seed)
├── h2/             V2 índice único via coluna gerada
└── oracle/         V2 índice único function-based
```

Veja **[DECISOES.md](DECISOES.md)** para o racional das principais decisões técnicas.

---

## Frontend

Há também uma interface React + TypeScript + Vite em [`../frontend/`](../frontend/) que consome esta API. Para rodar:

```bash
cd ../frontend
npm install
npm run dev
```

Abre em http://localhost:5173. O backend já tem CORS configurado para essa origem.

A interface cobre todos os endpoints: cadastrar/listar pacientes, criar/listar/cancelar agendamentos, com filtros por status e profissional. Detalhes em [frontend/README.md](../frontend/README.md).
