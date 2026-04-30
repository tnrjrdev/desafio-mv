// Tipos espelhando os DTOs do backend Spring Boot.

export type StatusAgendamento = 'AGENDADO' | 'CANCELADO' | 'REALIZADO';
export type TipoAtendimento = 'CONSULTA' | 'RETORNO' | 'EXAME';

export interface Paciente {
  id: number;
  nome: string;
  cpf: string;
  email?: string | null;
  telefone?: string | null;
  dataNascimento?: string | null;
}

export interface PacienteRequest {
  nome: string;
  cpf: string;
  email?: string;
  telefone?: string;
  dataNascimento?: string;
}

export interface Agendamento {
  id: number;
  pacienteId: number;
  pacienteNome: string;
  profissionalId: number;
  profissionalNome: string;
  dataHora: string;
  tipoAtendimento: TipoAtendimento;
  status: StatusAgendamento;
  motivoCancelamento?: string | null;
  canceladoEm?: string | null;
}

export interface AgendamentoRequest {
  pacienteId: number;
  profissionalId: number;
  dataHora: string;
  tipoAtendimento: TipoAtendimento;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  fieldErrors?: { field: string; message: string }[];
}
