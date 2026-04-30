package com.clinica.agendamento.dto;

import com.clinica.agendamento.domain.Agendamento;
import com.clinica.agendamento.domain.StatusAgendamento;
import com.clinica.agendamento.domain.TipoAtendimento;

import java.time.LocalDateTime;

public record AgendamentoResponse(
        Long id,
        Long pacienteId,
        String pacienteNome,
        Long profissionalId,
        String profissionalNome,
        LocalDateTime dataHora,
        TipoAtendimento tipoAtendimento,
        StatusAgendamento status,
        String motivoCancelamento,
        LocalDateTime canceladoEm
) {
    public static AgendamentoResponse from(Agendamento a) {
        return new AgendamentoResponse(
                a.getId(),
                a.getPaciente().getId(),
                a.getPaciente().getNome(),
                a.getProfissional().getId(),
                a.getProfissional().getNome(),
                a.getDataHora(),
                a.getTipoAtendimento(),
                a.getStatus(),
                a.getMotivoCancelamento(),
                a.getCanceladoEm()
        );
    }
}
