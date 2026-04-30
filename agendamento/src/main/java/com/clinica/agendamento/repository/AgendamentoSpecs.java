package com.clinica.agendamento.repository;

import com.clinica.agendamento.domain.Agendamento;
import com.clinica.agendamento.domain.enums.StatusAgendamento;
import org.springframework.data.jpa.domain.Specification;

public final class AgendamentoSpecs {

    private AgendamentoSpecs() { }

    public static Specification<Agendamento> comPaciente(Long pacienteId) {
        if (pacienteId == null) return null;
        return (root, q, cb) -> cb.equal(root.get("paciente").get("id"), pacienteId);
    }

    public static Specification<Agendamento> comProfissional(Long profissionalId) {
        if (profissionalId == null) return null;
        return (root, q, cb) -> cb.equal(root.get("profissional").get("id"), profissionalId);
    }

    public static Specification<Agendamento> comStatus(StatusAgendamento status) {
        if (status == null) return null;
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }
}
