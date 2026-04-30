import { useEffect, useState, type FormEvent } from 'react';
import {
  ApiException,
  cancelarAgendamento,
  criarAgendamento,
  listarAgendamentos,
  listarPacientes,
  type FiltroAgendamentos,
} from '../api';
import type {
  Agendamento,
  AgendamentoRequest,
  Paciente,
  StatusAgendamento,
  TipoAtendimento,
} from '../types';

const TIPOS: TipoAtendimento[] = ['CONSULTA', 'RETORNO', 'EXAME'];
const STATUS: StatusAgendamento[] = ['AGENDADO', 'CANCELADO', 'REALIZADO'];

const FORM_INICIAL: AgendamentoRequest = {
  pacienteId: 0,
  profissionalId: 1,
  dataHora: '',
  tipoAtendimento: 'CONSULTA',
};

export function AgendamentosView() {
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([]);
  const [pacientes, setPacientes] = useState<Paciente[]>([]);
  const [form, setForm] = useState<AgendamentoRequest>(FORM_INICIAL);
  const [filtros, setFiltros] = useState<FiltroAgendamentos>({});
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  async function carregar() {
    try {
      const [ags, pcs] = await Promise.all([listarAgendamentos(filtros), listarPacientes()]);
      setAgendamentos(ags);
      setPacientes(pcs);
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  useEffect(() => {
    carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtros]);

  async function submeter(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setSucesso(null);
    setCarregando(true);
    try {
      const criado = await criarAgendamento(form);
      setSucesso(
        `Agendamento criado (id ${criado.id}) para ${criado.pacienteNome} com ${criado.profissionalNome}.`
      );
      setForm({ ...FORM_INICIAL, pacienteId: form.pacienteId });
      await carregar();
    } catch (e) {
      if (e instanceof ApiException) {
        const detalhes = e.body.fieldErrors
          ?.map((fe) => `${fe.field}: ${fe.message}`)
          .join(' | ');
        setErro(detalhes ?? e.body.message);
      } else {
        setErro((e as Error).message);
      }
    } finally {
      setCarregando(false);
    }
  }

  async function cancelar(ag: Agendamento) {
    const motivo = window.prompt(
      `Informe o motivo do cancelamento do agendamento #${ag.id}:`
    );
    if (!motivo || motivo.trim().length < 3) {
      alert('Motivo deve ter pelo menos 3 caracteres.');
      return;
    }
    try {
      await cancelarAgendamento(ag.id, motivo.trim());
      setSucesso(`Agendamento #${ag.id} cancelado.`);
      await carregar();
    } catch (e) {
      if (e instanceof ApiException) {
        setErro(e.body.message);
      } else {
        setErro((e as Error).message);
      }
    }
  }

  return (
    <section className="card">
      <h2>Agendamentos</h2>

      <form onSubmit={submeter} className="form">
        <div className="row">
          <label>
            Paciente *
            <select
              required
              value={form.pacienteId || ''}
              onChange={(e) => setForm({ ...form, pacienteId: Number(e.target.value) })}
            >
              <option value="">-- selecione --</option>
              {pacientes.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nome} (id {p.id})
                </option>
              ))}
            </select>
          </label>
          <label>
            Profissional *
            <select
              required
              value={form.profissionalId}
              onChange={(e) => setForm({ ...form, profissionalId: Number(e.target.value) })}
            >
              <option value={1}>1 - Dra. Ana Lima (Cardiologia)</option>
              <option value={2}>2 - Dr. Bruno Santos (Clinico Geral)</option>
              <option value={3}>3 - Dra. Carla Mendes (Dermatologia)</option>
            </select>
          </label>
        </div>
        <div className="row">
          <label>
            Data e hora *
            <input
              type="datetime-local"
              required
              value={form.dataHora}
              onChange={(e) => setForm({ ...form, dataHora: e.target.value })}
            />
          </label>
          <label>
            Tipo *
            <select
              required
              value={form.tipoAtendimento}
              onChange={(e) =>
                setForm({ ...form, tipoAtendimento: e.target.value as TipoAtendimento })
              }
            >
              {TIPOS.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>
        </div>
        <button type="submit" disabled={carregando || !form.pacienteId}>
          {carregando ? 'Salvando...' : 'Criar agendamento'}
        </button>
      </form>

      {erro && <div className="alert erro">{erro}</div>}
      {sucesso && <div className="alert sucesso">{sucesso}</div>}

      <div className="filtros">
        <strong>Filtrar:</strong>
        <select
          value={filtros.status ?? ''}
          onChange={(e) =>
            setFiltros({
              ...filtros,
              status: (e.target.value || undefined) as StatusAgendamento | undefined,
            })
          }
        >
          <option value="">Todos os status</option>
          {STATUS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <select
          value={filtros.profissionalId ?? ''}
          onChange={(e) =>
            setFiltros({
              ...filtros,
              profissionalId: e.target.value ? Number(e.target.value) : undefined,
            })
          }
        >
          <option value="">Todos os profissionais</option>
          <option value={1}>1 - Dra. Ana Lima</option>
          <option value={2}>2 - Dr. Bruno Santos</option>
          <option value={3}>3 - Dra. Carla Mendes</option>
        </select>
      </div>

      <h3>Listagem ({agendamentos.length})</h3>
      {agendamentos.length === 0 ? (
        <p className="vazio">Nenhum agendamento encontrado.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Paciente</th>
              <th>Profissional</th>
              <th>Data/Hora</th>
              <th>Tipo</th>
              <th>Status</th>
              <th>Motivo cancel.</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            {agendamentos.map((a) => (
              <tr key={a.id} className={a.status === 'CANCELADO' ? 'cancelado' : ''}>
                <td>{a.id}</td>
                <td>{a.pacienteNome}</td>
                <td>{a.profissionalNome}</td>
                <td>{new Date(a.dataHora).toLocaleString('pt-BR')}</td>
                <td>{a.tipoAtendimento}</td>
                <td>
                  <span className={`badge ${a.status.toLowerCase()}`}>{a.status}</span>
                </td>
                <td>{a.motivoCancelamento ?? '-'}</td>
                <td>
                  {a.status === 'AGENDADO' && (
                    <button className="link" onClick={() => cancelar(a)}>
                      Cancelar
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
