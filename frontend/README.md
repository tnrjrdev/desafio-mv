# Frontend - Agendamento Clinico

Interface React + TypeScript + Vite para consumir a API.

## Pre-requisitos

- Node.js 20+
- Backend Spring Boot rodando em `http://localhost:8080`

## Executar

```bash
cd frontend
npm install
npm run dev
```

Abre em http://localhost:5173.

## Estrutura

```
frontend/
├── src/
│   ├── main.tsx              entry point
│   ├── App.tsx               navegacao por abas
│   ├── api.ts                wrappers fetch + tratamento de erro
│   ├── types.ts              tipos espelhando os DTOs do backend
│   ├── styles.css            estilos
│   └── components/
│       ├── PacientesView.tsx       form + listagem de pacientes
│       └── AgendamentosView.tsx    form + filtros + cancelar agendamentos
└── ...
```

## O que cobre

- Cadastrar paciente (com validacao do servidor exibida em campo)
- Listar pacientes
- Criar agendamento (selecionando paciente + profissional via dropdown)
- Listar agendamentos com filtros (status, profissional)
- Cancelar agendamento (modal solicita motivo)
- Mensagens de erro/sucesso amigaveis em cada operacao
- Linhas canceladas aparecem com visual diferenciado
