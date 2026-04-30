package com.clinica.agendamento.repository;

import com.clinica.agendamento.domain.Agendamento;
import com.clinica.agendamento.domain.enums.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface AgendamentoRepository
        extends JpaRepository<Agendamento, Long>, JpaSpecificationExecutor<Agendamento> {

    boolean existsByProfissionalIdAndDataHoraAndStatusNot(
            Long profissionalId, LocalDateTime dataHora, StatusAgendamento status);
}
