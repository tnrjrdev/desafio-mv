package com.clinica.agendamento.dto;

import com.clinica.agendamento.domain.Paciente;

import java.time.LocalDate;

public record PacienteResponse(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        LocalDate dataNascimento
) {
    public static PacienteResponse from(Paciente p) {
        return new PacienteResponse(
                p.getId(), p.getNome(), p.getCpf(),
                p.getEmail(), p.getTelefone(), p.getDataNascimento()
        );
    }
}
