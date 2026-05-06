# Desafio MV — Agendamento Clínico

Sistema de agendamento de consultas para uma clínica, desenvolvido como parte do desafio técnico para a vaga de Desenvolvedor Júnior na MV.

O projeto é dividido em dois módulos:

- **[`agendamento/`](agendamento/)** — API REST em Java + Spring Boot
- **[`frontend/`](frontend/)** — Interface web em React + TypeScript + Vite

---

## Stack

**Backend**
- Java 17 + Spring Boot 3.5
- Spring Web, Spring Data JPA, Bean Validation, Lombok
- Oracle 21c (XE) como banco principal
- Flyway para versionamento de schema
- Springdoc OpenAPI (Swagger UI)
- JUnit 5 + Mockito + MockMvc (com H2 in-memory em escopo `test`)
- Maven Wrapper (`mvnw`)

**Frontend**
- React 18 + TypeScript + Vite

---

## Como executar

### Pré-requisitos
- Java 17+
- Node.js 20+
- Docker + Docker Compose

### 1. Subir o banco Oracle

```bash
cd agendamento
docker compose up -d
```

A primeira execução baixa a imagem (~700MB) e leva ~1 minuto. Acompanhe com `docker compose logs -f oracle` até aparecer `DATABASE IS READY TO USE!`.

Conexão:
- **JDBC URL:** `jdbc:oracle:thin:@localhost:1521/XEPDB1`
- **Usuário:** `system` / **Senha:** `oracle`

### 2. Rodar a API

```bash
cd agendamento
./mvnw spring-boot:run
```

Sobe em http://localhost:8080.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

### 3. Rodar o frontend

```bash
cd frontend
npm install
npm run dev
```

Abre em http://localhost:5173. O backend já tem CORS configurado para essa origem.

### 4. Rodar os testes

Os testes usam H2 em memória, **não dependem** do Docker:

```bash
cd agendamento
./mvnw test
```

### 5. Parar o banco

```bash
docker compose down            # mantém os dados
docker compose down -v         # remove os dados
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

Schemas e exemplos completos no Swagger UI.

---

## Regras de negócio

1. **Profissional não pode ter dois agendamentos no mesmo horário** — validação no service + índice único parcial no banco (ignora linhas canceladas). Race condition tratada com `409 Conflict`.
2. **Não permite agendamento no passado** — validado no DTO (`@Future`) e no service (com `Clock` injetado para testabilidade).
3. **Cancelamento exige motivo** — Bean Validation no DTO (3 a 500 caracteres).
4. **Cancelar muda status para `CANCELADO`** — operação `PATCH`, registro permanece com `motivo_cancelamento` e `cancelado_em`.
5. **Listagem aceita filtros combináveis** por paciente, profissional e status — `JpaSpecificationExecutor` + `Specification`.

---

## Códigos HTTP

| Código | Quando |
|---|---|
| `200 OK` | Listagem ou cancelamento |
| `201 Created` | Cadastro de paciente / criação de agendamento |
| `400 Bad Request` | JSON inválido ou erro de Bean Validation |
| `404 Not Found` | Recurso inexistente |
| `409 Conflict` | CPF duplicado ou conflito de horário |
| `422 Unprocessable Entity` | Regra de negócio violada |

---

## Estrutura

```
.
├── agendamento/              backend Spring Boot
│   ├── src/main/java/com/clinica/agendamento/
│   │   ├── domain/           entidades JPA (+ enums)
│   │   ├── dto/              records de request/response
│   │   ├── repository/       Spring Data + Specifications
│   │   ├── service/          regras de negócio
│   │   ├── controller/       endpoints REST
│   │   ├── exception/        exceptions + handler global
│   │   └── config/           OpenAPI, Clock, CORS
│   ├── src/main/resources/db/migration/
│   │   ├── common/           seed
│   │   ├── h2/               schema dos testes
│   │   └── oracle/           schema de dev/produção
│   ├── docker-compose.yml    Oracle XE
│   ├── DECISOES.md           racional das decisões técnicas
│   └── README.md             detalhes do backend
│
└── frontend/                 interface React + TypeScript
    ├── src/
    │   ├── App.tsx
    │   ├── api.ts            wrappers fetch + tratamento de erro
    │   ├── types.ts          tipos espelhando os DTOs
    │   └── components/
    │       ├── PacientesView.tsx
    │       └── AgendamentosView.tsx
    └── README.md             detalhes do frontend
```

---

## Documentação adicional

- **[agendamento/README.md](agendamento/README.md)** — detalhes do backend (exemplos curl, estrutura de pacotes)
- **[agendamento/DECISOES.md](agendamento/DECISOES.md)** — racional das principais decisões técnicas
- **[frontend/README.md](frontend/README.md)** — detalhes do frontend
