package com.clinica.agendamento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank @Size(max = 150)
        String nome,

        @NotBlank
        @Pattern(regexp = "\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}",
                 message = "CPF deve ter 11 digitos ou estar no formato 000.000.000-00")
        String cpf,

        @Email @Size(max = 150)
        String email,

        @Size(max = 20)
        String telefone,

        @Past(message = "Data de nascimento deve estar no passado")
        LocalDate dataNascimento
) { }
