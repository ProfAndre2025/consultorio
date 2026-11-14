-- ============================================================
-- Dados de exemplo para as aulas.
--
-- O Hibernate executa este arquivo AUTOMATICAMENTE toda vez que a aplicacao
-- sobe (propriedade hibernate.hbm2ddl.import_files, configurada em
-- DataConfiguration). Como ele roda a cada inicializacao, usamos INSERT IGNORE:
-- se a linha ja existe (mesma chave), o MySQL a ignora em vez de dar erro de
-- chave duplicada. Assim o seed e idempotente -- pode rodar quantas vezes for.
-- ============================================================

INSERT IGNORE INTO produto (id, nome, unidade, preco_unitario, estoque) VALUES
    (1, 'Anestesico Lidocaina 2%', 'tubete',  4.50, 200),
    (2, 'Luva de procedimento',    'par',     1.20, 500),
    (3, 'Resina composta A2',      'g',      28.00,  60),
    (4, 'Broca diamantada',        'un',     12.00,  80),
    (5, 'Sugador descartavel',     'un',      0.80, 400),
    (6, 'Fio de sutura',           'un',      9.50,  50),
    (7, 'Gaze esteril',            'pacote',  3.00, 150),
    (8, 'Fluor gel',               'ml',      0.60, 300);

INSERT IGNORE INTO procedimento (id, nome, descricao, preco_mao_obra, duracao_minutos) VALUES
    (1, 'Limpeza (profilaxia)',  'Remocao de placa e tartaro',    80.00, 30),
    (2, 'Restauracao em resina', 'Restauracao de carie',         150.00, 60),
    (3, 'Extracao simples',      'Extracao de dente irrompido',  200.00, 45),
    (4, 'Aplicacao de fluor',    'Prevencao de carie',            50.00, 20);

INSERT IGNORE INTO procedimento_produto (procedimento_id, produto_id, quantidade) VALUES
    (1, 2, 1), (1, 5, 1), (1, 7, 2),
    (2, 1, 1), (2, 2, 1), (2, 3, 2), (2, 4, 1), (2, 5, 1),
    (3, 1, 2), (3, 2, 1), (3, 5, 1), (3, 6, 1), (3, 7, 3),
    (4, 2, 1), (4, 8, 5);

-- A senha de atendimento dos dois dentistas de exemplo e "1234" (apenas para
-- testes). O valor abaixo e o hash BCrypt dessa senha; a senha em texto puro
-- nunca fica no banco. Troque pelo cadastro do dentista em producao.
INSERT IGNORE INTO dentista (id, nome, cro, especialidade, telefone, email, ativo, senha_atendimento) VALUES
    (1, 'Dra. Helena Martins', 'CRO-SP 45231', 'Clinica geral',   '(11) 98888-1122', 'helena@consultorio.com', TRUE, '$2a$10$X0Aytv1KnxqDVgTy9z/MW.kbKgTSy9BeX.7.iEjIcbyUP/7yzRwuG'),
    (2, 'Dr. Rafael Nogueira', 'CRO-SP 51907', 'Odontopediatria', '(11) 97777-3344', 'rafael@consultorio.com', TRUE, '$2a$10$X0Aytv1KnxqDVgTy9z/MW.kbKgTSy9BeX.7.iEjIcbyUP/7yzRwuG');

INSERT IGNORE INTO disponibilidade (id, dentista_id, dia_semana, hora_inicio, hora_fim) VALUES
    (1, 1, 'MONDAY',    '08:00:00', '12:00:00'),
    (2, 1, 'TUESDAY',   '08:00:00', '12:00:00'),
    (3, 1, 'WEDNESDAY', '13:00:00', '18:00:00'),
    (4, 1, 'THURSDAY',  '08:00:00', '12:00:00'),
    (5, 1, 'FRIDAY',    '08:00:00', '12:00:00'),
    (6, 2, 'MONDAY',    '13:00:00', '18:00:00'),
    (7, 2, 'WEDNESDAY', '08:00:00', '12:00:00'),
    (8, 2, 'FRIDAY',    '13:00:00', '18:00:00');

INSERT IGNORE INTO paciente (id, nome, cpf, data_nascimento, telefone, email, criado_em) VALUES
    (1, 'Marina Souza',   '123.456.789-00', '1998-04-12', '(11) 96666-1010', 'marina@email.com', NOW(6)),
    (2, 'Carlos Ribeiro', '987.654.321-00', '1985-11-30', '(11) 95555-2020', 'carlos@email.com', NOW(6));
