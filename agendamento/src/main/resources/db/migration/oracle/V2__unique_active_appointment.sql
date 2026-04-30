-- Oracle: indice unico baseado em funcao (function-based unique index).
--
-- Mesma ideia da versao H2: garante apenas um agendamento ATIVO por
-- (profissional, horario). Linhas CANCELADAS nao entram no indice porque
-- DECODE retorna NULL e o Oracle nao indexa tuplas com TODAS as colunas NULL.
--
-- Usamos DECODE em vez de CASE WHEN por ser a sintaxe historica e mais
-- portatil em Oracle para indices baseados em funcao.
CREATE UNIQUE INDEX uk_agendamento_profissional_horario
    ON agendamento (
        DECODE(status, 'CANCELADO', NULL, profissional_id),
        DECODE(status, 'CANCELADO', NULL, data_hora)
    );
