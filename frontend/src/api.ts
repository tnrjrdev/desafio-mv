import type {
  Agendamento,
  AgendamentoRequest,
  ApiError,
  Paciente,
  PacienteRequest,
  StatusAgendamento,
} from './types';

const BASE_URL = 'http://localhost:8080/api';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
  });

  if (!response.ok) {
    let body: ApiError;
    try {
      body = (await response.json()) as ApiError;
    } catch {
      throw new Error(`HTTP ${response.status}`);
    }
    throw new ApiException(body);
  }

  // 204 No Content nao tem body
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export class ApiException extends Error {
  body: ApiError;
  constructor(body: ApiError) {
    super(body.message);
    this.body = body;
  }
}

// ----- Pacientes -----

export const listarPacientes = () => request<Paciente[]>('/pacientes');

export const criarPaciente = (req: PacienteRequest) =>
  request<Paciente>('/pacientes', { method: 'POST', body: JSON.stringify(req) });

// ----- Agendamentos -----

export interface FiltroAgendamentos {
  pacienteId?: number;
  profissionalId?: number;
  status?: StatusAgendamento;
}

export const listarAgendamentos = (filtros: FiltroAgendamentos = {}) => {
  const params = new URLSearchParams();
  if (filtros.pacienteId) params.append('pacienteId', String(filtros.pacienteId));
  if (filtros.profissionalId) params.append('profissionalId', String(filtros.profissionalId));
  if (filtros.status) params.append('status', filtros.status);
  const query = params.toString();
  return request<Agendamento[]>(`/agendamentos${query ? `?${query}` : ''}`);
};

export const criarAgendamento = (req: AgendamentoRequest) =>
  request<Agendamento>('/agendamentos', { method: 'POST', body: JSON.stringify(req) });

export const cancelarAgendamento = (id: number, motivo: string) =>
  request<Agendamento>(`/agendamentos/${id}/cancelar`, {
    method: 'PATCH',
    body: JSON.stringify({ motivo }),
  });
