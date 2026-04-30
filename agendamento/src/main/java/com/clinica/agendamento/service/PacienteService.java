package com.clinica.agendamento.service;

import com.clinica.agendamento.domain.Paciente;
import com.clinica.agendamento.dto.PacienteRequest;
import com.clinica.agendamento.dto.PacienteResponse;
import com.clinica.agendamento.exception.ConflictException;
import com.clinica.agendamento.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository repository;

    public PacienteService(PacienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PacienteResponse criar(PacienteRequest req) {
        String cpfNormalizado = normalizarCpf(req.cpf());
        if (repository.existsByCpf(cpfNormalizado)) {
            throw new ConflictException("Ja existe paciente cadastrado com este CPF");
        }
        Paciente paciente = new Paciente(
                req.nome().trim(),
                cpfNormalizado,
                req.email(),
                req.telefone(),
                req.dataNascimento()
        );
        return PacienteResponse.from(repository.save(paciente));
    }

    @Transactional(readOnly = true)
    public List<PacienteResponse> listar() {
        return repository.findAll().stream()
                .map(PacienteResponse::from)
                .toList();
    }

    private static String normalizarCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }
}
