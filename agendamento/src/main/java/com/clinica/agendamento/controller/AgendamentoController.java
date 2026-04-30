package com.clinica.agendamento.controller;

import com.clinica.agendamento.domain.enums.StatusAgendamento;
import com.clinica.agendamento.dto.AgendamentoRequest;
import com.clinica.agendamento.dto.AgendamentoResponse;
import com.clinica.agendamento.dto.CancelamentoRequest;
import com.clinica.agendamento.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
@Tag(name = "Agendamentos", description = "Criacao, consulta e cancelamento de agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria um novo agendamento")
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody AgendamentoRequest req) {
        AgendamentoResponse criado = service.criar(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista agendamentos com filtros opcionais")
    public List<AgendamentoResponse> listar(
            @Parameter(description = "Filtra por id do paciente")
            @RequestParam(required = false) Long pacienteId,

            @Parameter(description = "Filtra por id do profissional")
            @RequestParam(required = false) Long profissionalId,

            @Parameter(description = "Filtra por status (AGENDADO, CANCELADO, REALIZADO)")
            @RequestParam(required = false) StatusAgendamento status) {
        return service.listar(pacienteId, profissionalId, status);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela um agendamento informando o motivo")
    public AgendamentoResponse cancelar(@PathVariable Long id,
                                        @Valid @RequestBody CancelamentoRequest req) {
        return service.cancelar(id, req);
    }
}
