import { useState } from 'react';
import { PacientesView } from './components/PacientesView';
import { AgendamentosView } from './components/AgendamentosView';

type Aba = 'pacientes' | 'agendamentos';

export default function App() {
  const [aba, setAba] = useState<Aba>('pacientes');

  return (
    <div className="container">
      <header>
        <h1>Sistema de Agendamento Clinico</h1>
        <p>Desafio MV - Demo da API REST</p>
      </header>

      <nav className="tabs">
        <button
          className={aba === 'pacientes' ? 'tab ativa' : 'tab'}
          onClick={() => setAba('pacientes')}
        >
          Pacientes
        </button>
        <button
          className={aba === 'agendamentos' ? 'tab ativa' : 'tab'}
          onClick={() => setAba('agendamentos')}
        >
          Agendamentos
        </button>
      </nav>

      <main>{aba === 'pacientes' ? <PacientesView /> : <AgendamentosView />}</main>

      <footer>
        <small>API: http://localhost:8080 | Swagger: http://localhost:8080/swagger-ui.html</small>
      </footer>
    </div>
  );
}
