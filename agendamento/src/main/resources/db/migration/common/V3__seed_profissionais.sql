-- Seed de profissionais para facilitar testes manuais via Swagger/Postman.
-- O desafio nao pede CRUD de profissional, entao deixamos um set conhecido.
INSERT INTO profissional (nome, especialidade, registro_conselho)
    VALUES ('Dra. Ana Lima',     'Cardiologia',     'CRM-SP-100001');
INSERT INTO profissional (nome, especialidade, registro_conselho)
    VALUES ('Dr. Bruno Santos',  'Clinico Geral',   'CRM-SP-100002');
INSERT INTO profissional (nome, especialidade, registro_conselho)
    VALUES ('Dra. Carla Mendes', 'Dermatologia',    'CRM-SP-100003');
