-- H2: indice unico baseado em coluna gerada
--
-- H2 nao suporta expressoes (CASE/funcoes) diretamente em CREATE INDEX como o Oracle.
-- Workaround: criamos uma coluna VIRTUAL (gerada) que vale NULL para registros
-- cancelados e (profissional_id || data_hora) para os ativos. Aplicar UNIQUE
-- nessa coluna garante a mesma semantica do indice parcial usado no Oracle:
-- so existe UM agendamento NAO-CANCELADO por (profissional, horario).
--
-- A coluna nao precisa ser mapeada na entidade JPA (Hibernate validate ignora
-- colunas extras no schema).
ALTER TABLE agendamento
    ADD slot_ativo VARCHAR(60) AS (
        CASE WHEN status <> 'CANCELADO'
             THEN CONCAT(profissional_id, '|', data_hora)
             ELSE NULL
        END
    );

CREATE UNIQUE INDEX uk_agendamento_slot_ativo ON agendamento (slot_ativo);
