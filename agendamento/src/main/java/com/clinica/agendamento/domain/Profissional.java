package com.clinica.agendamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "profissional")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 100)
    private String especialidade;

    @Column(name = "registro_conselho", nullable = false, unique = true, length = 30)
    private String registroConselho;

    @Column(name = "criado_em", nullable = false, updatable = false, insertable = false)
    private LocalDateTime criadoEm;

    protected Profissional() { }

    public Profissional(String nome, String especialidade, String registroConselho) {
        this.nome = nome;
        this.especialidade = especialidade;
        this.registroConselho = registroConselho;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEspecialidade() { return especialidade; }
    public String getRegistroConselho() { return registroConselho; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Profissional that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
