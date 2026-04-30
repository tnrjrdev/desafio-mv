-- DECODE retorna NULL para registros CANCELADOS, que ficam fora do indice unico.
CREATE UNIQUE INDEX uk_agendamento_profissional_horario
    ON agendamento (
        DECODE(status, 'CANCELADO', NULL, profissional_id),
        DECODE(status, 'CANCELADO', NULL, data_hora)
    );
