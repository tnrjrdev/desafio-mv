-- H2 nao aceita expressoes em CREATE INDEX, entao usamos coluna gerada como workaround.
ALTER TABLE agendamento
    ADD slot_ativo VARCHAR(60) AS (
        CASE WHEN status <> 'CANCELADO'
             THEN CONCAT(profissional_id, '|', data_hora)
             ELSE NULL
        END
    );

CREATE UNIQUE INDEX uk_agendamento_slot_ativo ON agendamento (slot_ativo);
