package com.clinica.agendamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "agendamento")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_atendimento", nullable = false, length = 30)
    private TipoAtendimento tipoAtendimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "criado_em", nullable = false, updatable = false, insertable = false)
    private LocalDateTime criadoEm;

    protected Agendamento() { }

    public Agendamento(Paciente paciente, Profissional profissional,
                       LocalDateTime dataHora, TipoAtendimento tipoAtendimento) {
        this.paciente = paciente;
        this.profissional = profissional;
        this.dataHora = dataHora;
        this.tipoAtendimento = tipoAtendimento;
        this.status = StatusAgendamento.AGENDADO;
    }

    public void cancelar(String motivo) {
        if (this.status == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Agendamento ja esta cancelado");
        }
        this.status = StatusAgendamento.CANCELADO;
        this.motivoCancelamento = motivo;
        this.canceladoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public Profissional getProfissional() { return profissional; }
    public LocalDateTime getDataHora() { return dataHora; }
    public TipoAtendimento getTipoAtendimento() { return tipoAtendimento; }
    public StatusAgendamento getStatus() { return status; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Agendamento that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
