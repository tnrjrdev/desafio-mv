package com.clinica.agendamento.dto;

import com.clinica.agendamento.domain.TipoAtendimento;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AgendamentoRequest(
        @NotNull(message = "pacienteId e obrigatorio")
        Long pacienteId,

        @NotNull(message = "profissionalId e obrigatorio")
        Long profissionalId,

        @NotNull(message = "dataHora e obrigatoria")
        @Future(message = "dataHora deve estar no futuro")
        LocalDateTime dataHora,

        @NotNull(message = "tipoAtendimento e obrigatorio")
        TipoAtendimento tipoAtendimento
) { }
