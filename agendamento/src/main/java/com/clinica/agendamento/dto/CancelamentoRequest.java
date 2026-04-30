package com.clinica.agendamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelamentoRequest(
        @NotBlank(message = "motivo e obrigatorio")
        @Size(min = 3, max = 500, message = "motivo deve ter entre 3 e 500 caracteres")
        String motivo
) { }
