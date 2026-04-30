package com.clinica.agendamento.service;

import com.clinica.agendamento.domain.Agendamento;
import com.clinica.agendamento.domain.Paciente;
import com.clinica.agendamento.domain.Profissional;
import com.clinica.agendamento.domain.enums.StatusAgendamento;
import com.clinica.agendamento.dto.AgendamentoRequest;
import com.clinica.agendamento.dto.AgendamentoResponse;
import com.clinica.agendamento.dto.CancelamentoRequest;
import com.clinica.agendamento.exception.BusinessException;
import com.clinica.agendamento.exception.ConflictException;
import com.clinica.agendamento.exception.NotFoundException;
import com.clinica.agendamento.repository.AgendamentoRepository;
import com.clinica.agendamento.repository.AgendamentoSpecs;
import com.clinica.agendamento.repository.PacienteRepository;
import com.clinica.agendamento.repository.ProfissionalRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final Clock clock;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              PacienteRepository pacienteRepository,
                              ProfissionalRepository profissionalRepository,
                              Clock clock) {
        this.agendamentoRepository = agendamentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.profissionalRepository = profissionalRepository;
        this.clock = clock;
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest req) {
        if (!req.dataHora().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("Data e hora do agendamento devem estar no futuro");
        }

        Paciente paciente = pacienteRepository.findById(req.pacienteId())
                .orElseThrow(() -> new NotFoundException(
                        "Paciente " + req.pacienteId() + " nao encontrado"));

        Profissional profissional = profissionalRepository.findById(req.profissionalId())
                .orElseThrow(() -> new NotFoundException(
                        "Profissional " + req.profissionalId() + " nao encontrado"));

        // Defesa em camadas: existsBy aqui da mensagem amigavel no caso comum;
        // o indice unico no banco (try/catch abaixo) fecha a janela de race condition.
        boolean ocupado = agendamentoRepository
                .existsByProfissionalIdAndDataHoraAndStatusNot(
                        profissional.getId(), req.dataHora(), StatusAgendamento.CANCELADO);
        if (ocupado) {
            throw new ConflictException("Profissional ja possui agendamento neste horario");
        }

        Agendamento agendamento = new Agendamento(
                paciente, profissional, req.dataHora(), req.tipoAtendimento());

        try {
            return AgendamentoResponse.from(agendamentoRepository.saveAndFlush(agendamento));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Profissional ja possui agendamento neste horario");
        }
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(Long pacienteId, Long profissionalId, StatusAgendamento status) {
        Specification<Agendamento> spec = Specification.allOf(
                AgendamentoSpecs.comPaciente(pacienteId),
                AgendamentoSpecs.comProfissional(profissionalId),
                AgendamentoSpecs.comStatus(status)
        );
        return agendamentoRepository.findAll(spec).stream()
                .map(AgendamentoResponse::from)
                .toList();
    }

    @Transactional
    public AgendamentoResponse cancelar(Long id, CancelamentoRequest req) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Agendamento " + id + " nao encontrado"));

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new BusinessException("Agendamento ja esta cancelado");
        }

        agendamento.cancelar(req.motivo().trim());
        return AgendamentoResponse.from(agendamento);
    }
}
