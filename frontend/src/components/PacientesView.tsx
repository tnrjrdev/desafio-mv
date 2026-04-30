import { useEffect, useState, type FormEvent } from 'react';
import { ApiException, criarPaciente, listarPacientes } from '../api';
import type { Paciente, PacienteRequest } from '../types';

const FORM_INICIAL: PacienteRequest = {
  nome: '',
  cpf: '',
  email: '',
  telefone: '',
  dataNascimento: '',
};

export function PacientesView() {
  const [pacientes, setPacientes] = useState<Paciente[]>([]);
  const [form, setForm] = useState<PacienteRequest>(FORM_INICIAL);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  async function carregar() {
    try {
      setPacientes(await listarPacientes());
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  async function submeter(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setSucesso(null);
    setCarregando(true);
    try {
      // Limpa campos vazios opcionais antes de enviar
      const payload: PacienteRequest = {
        nome: form.nome,
        cpf: form.cpf,
        ...(form.email ? { email: form.email } : {}),
        ...(form.telefone ? { telefone: form.telefone } : {}),
        ...(form.dataNascimento ? { dataNascimento: form.dataNascimento } : {}),
      };
      const criado = await criarPaciente(payload);
      setSucesso(`Paciente "${criado.nome}" cadastrado (id ${criado.id}).`);
      setForm(FORM_INICIAL);
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

  return (
    <section className="card">
      <h2>Pacientes</h2>

      <form onSubmit={submeter} className="form">
        <div className="row">
          <label>
            Nome *
            <input
              required
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
            />
          </label>
          <label>
            CPF *
            <input
              required
              placeholder="apenas numeros"
              value={form.cpf}
              onChange={(e) => setForm({ ...form, cpf: e.target.value })}
            />
          </label>
        </div>
        <div className="row">
          <label>
            E-mail
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
          </label>
          <label>
            Telefone
            <input
              value={form.telefone}
              onChange={(e) => setForm({ ...form, telefone: e.target.value })}
            />
          </label>
          <label>
            Data de nascimento
            <input
              type="date"
              value={form.dataNascimento}
              onChange={(e) => setForm({ ...form, dataNascimento: e.target.value })}
            />
          </label>
        </div>
        <button type="submit" disabled={carregando}>
          {carregando ? 'Salvando...' : 'Cadastrar paciente'}
        </button>
      </form>

      {erro && <div className="alert erro">{erro}</div>}
      {sucesso && <div className="alert sucesso">{sucesso}</div>}

      <h3>Cadastrados ({pacientes.length})</h3>
      {pacientes.length === 0 ? (
        <p className="vazio">Nenhum paciente cadastrado ainda.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Nome</th>
              <th>CPF</th>
              <th>E-mail</th>
              <th>Telefone</th>
            </tr>
          </thead>
          <tbody>
            {pacientes.map((p) => (
              <tr key={p.id}>
                <td>{p.id}</td>
                <td>{p.nome}</td>
                <td>{p.cpf}</td>
                <td>{p.email ?? '-'}</td>
                <td>{p.telefone ?? '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
