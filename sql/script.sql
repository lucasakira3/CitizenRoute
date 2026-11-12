-- =====================================================================
-- Citizen Life · Global Solution 2026/2 · CitizenTech (FIAP)
-- Script Oracle: criação das tabelas e carga inicial de dados.
--
-- O Citizen Life é um hub com seis pilares da vida na cidade. O pilar de
-- mobilidade (planejar rota e mapa de riscos) é o que está implementado e é
-- o que estas tabelas guardam.
--
-- Como executar (SQL Developer): abra este arquivo e use "Executar Script" (F5).
-- O script pode ser executado várias vezes. O bloco inicial apaga somente
-- as tabelas do projeto (prefixo CL_) antes de recriá-las.
--
-- Requer Oracle 12c ou superior (colunas IDENTITY).
-- Senha de todos os cidadãos de demonstração: Citizen@2026
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Limpeza: remove as tabelas do projeto, se existirem
-- ---------------------------------------------------------------------
BEGIN
    FOR tabela IN (SELECT table_name
                     FROM user_tables
                    WHERE table_name IN ('CL_ENDERECO', 'CL_ROTA_VIA', 'CL_ROTA', 'CL_OCORRENCIA_VIA',
                                         'CL_OCORRENCIA', 'CL_CIDADAO', 'CL_VIA', 'CL_REGIAO'))
    LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || tabela.table_name || ' CASCADE CONSTRAINTS PURGE';
    END LOOP;
END;
/


-- ---------------------------------------------------------------------
-- 2. Tabelas
-- ---------------------------------------------------------------------

-- Zonas da cidade
CREATE TABLE cl_regiao (
    id_regiao  NUMBER(10)   GENERATED ALWAYS AS IDENTITY,
    nome       VARCHAR2(60) NOT NULL,
    CONSTRAINT pk_cl_regiao PRIMARY KEY (id_regiao),
    CONSTRAINT uk_cl_regiao_nome UNIQUE (nome)
);

-- Avenidas, ruas e trechos. Cada via pertence a uma região (1:N).
CREATE TABLE cl_via (
    id_via     NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    nome       VARCHAR2(120) NOT NULL,
    id_regiao  NUMBER(10)    NOT NULL,
    CONSTRAINT pk_cl_via PRIMARY KEY (id_via),
    CONSTRAINT fk_cl_via_regiao FOREIGN KEY (id_regiao) REFERENCES cl_regiao (id_regiao),
    CONSTRAINT uk_cl_via_nome_regiao UNIQUE (nome, id_regiao)
);

-- Quem usa o hub. O modal (como a pessoa se locomove) muda o risco de cada ocorrência.
-- O cadastro pede só nome, e-mail e senha; o modal é opcional e configurado depois, no perfil.
CREATE TABLE cl_cidadao (
    id_cidadao   NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    nome         VARCHAR2(100) NOT NULL,
    email        VARCHAR2(120) NOT NULL,
    senha_hash   VARCHAR2(200) NOT NULL,
    modal        VARCHAR2(10),
    dt_cadastro  DATE          DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_cl_cidadao PRIMARY KEY (id_cidadao),
    CONSTRAINT uk_cl_cidadao_email UNIQUE (email),
    CONSTRAINT ck_cl_cidadao_modal CHECK (modal IN ('CARRO', 'MOTO', 'ONIBUS', 'APP', 'PEDESTRE', 'BIKE'))
);

-- Evento que afeta as vias (1:N cidadão → ocorrências). Se o autor excluir a conta,
-- o relato continua no mapa sem autor (ON DELETE SET NULL).
CREATE TABLE cl_ocorrencia (
    id_ocorrencia     NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    titulo            VARCHAR2(100) NOT NULL,
    descricao         VARCHAR2(500) NOT NULL,
    tipo              VARCHAR2(20)  NOT NULL,
    gravidade         NUMBER(1)     NOT NULL,
    status            VARCHAR2(15)  DEFAULT 'ABERTA' NOT NULL,
    dt_registro       DATE          DEFAULT SYSDATE NOT NULL,
    dt_atualizacao    DATE,
    id_cidadao_autor  NUMBER(10),
    CONSTRAINT pk_cl_ocorrencia PRIMARY KEY (id_ocorrencia),
    CONSTRAINT fk_cl_ocorrencia_cidadao FOREIGN KEY (id_cidadao_autor)
        REFERENCES cl_cidadao (id_cidadao) ON DELETE SET NULL,
    CONSTRAINT ck_cl_ocorrencia_tipo CHECK (tipo IN ('ACIDENTE', 'ALAGAMENTO', 'QUEDA_ARVORE', 'SEMAFORO_APAGADO',
                                                     'DESLIZAMENTO', 'VIA_BLOQUEADA', 'INSEGURANCA')),
    CONSTRAINT ck_cl_ocorrencia_gravidade CHECK (gravidade BETWEEN 1 AND 5),
    CONSTRAINT ck_cl_ocorrencia_status CHECK (status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO',
                                                         'RESOLVIDA', 'DESCARTADA'))
);

-- Associativa N:N entre ocorrência e via
CREATE TABLE cl_ocorrencia_via (
    id_ocorrencia  NUMBER(10)   NOT NULL,
    id_via         NUMBER(10)   NOT NULL,
    nivel_impacto  VARCHAR2(15) NOT NULL,
    CONSTRAINT pk_cl_ocorrencia_via PRIMARY KEY (id_ocorrencia, id_via),
    CONSTRAINT fk_cl_ocvia_ocorrencia FOREIGN KEY (id_ocorrencia)
        REFERENCES cl_ocorrencia (id_ocorrencia) ON DELETE CASCADE,
    CONSTRAINT fk_cl_ocvia_via FOREIGN KEY (id_via) REFERENCES cl_via (id_via),
    CONSTRAINT ck_cl_ocvia_impacto CHECK (nivel_impacto IN ('BLOQUEIO_TOTAL', 'PARCIAL', 'LENTIDAO', 'SEM_INTERDICAO'))
);

-- Rota salva pelo cidadão: saída → destino (1:N cidadão → rotas).
-- X e Y são a posição no plano do mapa (unidades de 25 m), usada para redesenhar o caminho.
CREATE TABLE cl_rota (
    id_rota            NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    id_cidadao         NUMBER(10)    NOT NULL,
    nome               VARCHAR2(60)  NOT NULL,
    origem_descricao   VARCHAR2(120) NOT NULL,
    origem_x           NUMBER(8,2)   NOT NULL,
    origem_y           NUMBER(8,2)   NOT NULL,
    destino_descricao  VARCHAR2(120) NOT NULL,
    destino_x          NUMBER(8,2)   NOT NULL,
    destino_y          NUMBER(8,2)   NOT NULL,
    criterio           VARCHAR2(10)  DEFAULT 'SEGURA' NOT NULL,
    distancia_m        NUMBER(7)     NOT NULL,
    dt_criacao         DATE          DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_cl_rota PRIMARY KEY (id_rota),
    CONSTRAINT fk_cl_rota_cidadao FOREIGN KEY (id_cidadao)
        REFERENCES cl_cidadao (id_cidadao) ON DELETE CASCADE,
    CONSTRAINT uk_cl_rota_nome UNIQUE (id_cidadao, nome),
    CONSTRAINT ck_cl_rota_criterio CHECK (criterio IN ('RAPIDA', 'SEGURA')),
    CONSTRAINT ck_cl_rota_distancia CHECK (distancia_m > 0)
);

-- Associativa N:N entre rota e via: as vias cadastradas que a rota percorre, na ordem
CREATE TABLE cl_rota_via (
    id_rota  NUMBER(10) NOT NULL,
    id_via   NUMBER(10) NOT NULL,
    ordem    NUMBER(3)  NOT NULL,
    CONSTRAINT pk_cl_rota_via PRIMARY KEY (id_rota, id_via),
    CONSTRAINT fk_cl_rotavia_rota FOREIGN KEY (id_rota)
        REFERENCES cl_rota (id_rota) ON DELETE CASCADE,
    CONSTRAINT fk_cl_rotavia_via FOREIGN KEY (id_via)
        REFERENCES cl_via (id_via) ON DELETE CASCADE,
    CONSTRAINT ck_cl_rotavia_ordem CHECK (ordem >= 1)
);

-- Endereços salvos pelo cidadão (1:N cidadão → endereços): casa, trabalho, estudo...
-- Servem de atalho para a saída e o destino na tela de rotas. X e Y como em CL_ROTA.
CREATE TABLE cl_endereco (
    id_endereco  NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    id_cidadao   NUMBER(10)    NOT NULL,
    apelido      VARCHAR2(40)  NOT NULL,
    tipo         VARCHAR2(10)  NOT NULL,
    descricao    VARCHAR2(120) NOT NULL,
    pos_x        NUMBER(8,2)   NOT NULL,
    pos_y        NUMBER(8,2)   NOT NULL,
    dt_criacao   DATE          DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_cl_endereco PRIMARY KEY (id_endereco),
    CONSTRAINT fk_cl_endereco_cidadao FOREIGN KEY (id_cidadao)
        REFERENCES cl_cidadao (id_cidadao) ON DELETE CASCADE,
    CONSTRAINT uk_cl_endereco_apelido UNIQUE (id_cidadao, apelido),
    CONSTRAINT ck_cl_endereco_tipo CHECK (tipo IN ('CASA', 'TRABALHO', 'ESTUDO', 'OUTRO'))
);


-- ---------------------------------------------------------------------
-- 3. Índices nas chaves estrangeiras e filtros mais usados
-- ---------------------------------------------------------------------
CREATE INDEX ix_cl_via_regiao        ON cl_via (id_regiao);
CREATE INDEX ix_cl_ocorrencia_autor  ON cl_ocorrencia (id_cidadao_autor);
CREATE INDEX ix_cl_ocorrencia_status ON cl_ocorrencia (status);
CREATE INDEX ix_cl_ocvia_via         ON cl_ocorrencia_via (id_via);
CREATE INDEX ix_cl_rota_cidadao      ON cl_rota (id_cidadao);
CREATE INDEX ix_cl_rotavia_via       ON cl_rota_via (id_via);
CREATE INDEX ix_cl_endereco_cidadao  ON cl_endereco (id_cidadao);


-- ---------------------------------------------------------------------
-- 4. Documentação das tabelas no dicionário de dados
-- ---------------------------------------------------------------------
COMMENT ON TABLE cl_regiao IS 'Zonas da cidade';
COMMENT ON TABLE cl_via IS 'Avenidas, ruas e trechos ligados às rotas e às ocorrências';
COMMENT ON TABLE cl_cidadao IS 'Quem usa o hub; o modal muda o risco de cada ocorrência';
COMMENT ON TABLE cl_ocorrencia IS 'Evento que afeta vias (alagamento, acidente, queda de árvore...)';
COMMENT ON TABLE cl_ocorrencia_via IS 'Associativa N:N: vias atingidas por cada ocorrência';
COMMENT ON TABLE cl_rota IS 'Rotas salvas pelos cidadãos (saída → destino)';
COMMENT ON TABLE cl_rota_via IS 'Associativa N:N: vias cadastradas que cada rota percorre, na ordem';
COMMENT ON TABLE cl_endereco IS 'Endereços salvos pelo cidadão (casa, trabalho...), atalhos na tela de rotas';
COMMENT ON COLUMN cl_endereco.tipo IS 'CASA e TRABALHO são únicos por cidadão (regra na EnderecoService)';
COMMENT ON COLUMN cl_cidadao.senha_hash IS 'Hash PBKDF2 (pbkdf2$iteracoes$sal$hash). A senha nunca é gravada.';
COMMENT ON COLUMN cl_cidadao.modal IS 'Opcional; sem modal, os alertas calculam o risco para quem anda a pé';
COMMENT ON COLUMN cl_rota.criterio IS 'RAPIDA: menor tempo; SEGURA: desvia das vias com risco moderado ou maior';
COMMENT ON COLUMN cl_rota.origem_x IS 'Posição no plano do mapa (unidades de 25 m a partir do canto noroeste)';
COMMENT ON COLUMN cl_ocorrencia.gravidade IS 'De 1 (leve) a 5 (muito grave)';
COMMENT ON COLUMN cl_ocorrencia.id_cidadao_autor IS 'Quem relatou; nulo quando a pessoa excluiu a conta';


-- ---------------------------------------------------------------------
-- 5. Carga de dados
-- As chaves estrangeiras são buscadas por campos únicos (nome, e-mail,
-- título), então o script não depende dos ids gerados pelo IDENTITY.
-- Datas relativas (SYSDATE - n) mantêm a demonstração sempre recente:
-- SYSDATE - 1 é um dia atrás; SYSDATE - 3/24 são três horas atrás.
-- ---------------------------------------------------------------------

-- 5.1 Regiões
INSERT INTO cl_regiao (nome) VALUES ('Centro');
INSERT INTO cl_regiao (nome) VALUES ('Zona Norte');
INSERT INTO cl_regiao (nome) VALUES ('Zona Sul');
INSERT INTO cl_regiao (nome) VALUES ('Zona Leste');
INSERT INTO cl_regiao (nome) VALUES ('Zona Oeste');

-- 5.2 Vias
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Paulista', id_regiao FROM cl_regiao WHERE nome = 'Centro';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Rua da Consolação', id_regiao FROM cl_regiao WHERE nome = 'Centro';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida 23 de Maio', id_regiao FROM cl_regiao WHERE nome = 'Centro';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Prestes Maia', id_regiao FROM cl_regiao WHERE nome = 'Centro';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Marginal Tietê', id_regiao FROM cl_regiao WHERE nome = 'Zona Norte';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Cruzeiro do Sul', id_regiao FROM cl_regiao WHERE nome = 'Zona Norte';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Inajar de Souza', id_regiao FROM cl_regiao WHERE nome = 'Zona Norte';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Engenheiro Caetano Álvares', id_regiao FROM cl_regiao WHERE nome = 'Zona Norte';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Santo Amaro', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Washington Luís', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Interlagos', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Ricardo Jafet', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Radial Leste', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Aricanduva', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Sapopemba', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Salim Farah Maluf', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Marginal Pinheiros', id_regiao FROM cl_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Rebouças', id_regiao FROM cl_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Brigadeiro Faria Lima', id_regiao FROM cl_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Francisco Morato', id_regiao FROM cl_regiao WHERE nome = 'Zona Oeste';

-- 5.3 Cidadãos: um para cada modal
INSERT INTO cl_cidadao (nome, email, senha_hash, modal, dt_cadastro)
VALUES ('Ana Souza', 'ana.souza@exemplo.com',
        'pbkdf2$65536$mFrQjXjWLY5bwzc2daOtYw==$0fUOtRxmU00PCLgCrU+Iaq+zvnXNunGHFFvJJdp5fFg=', 'MOTO', SYSDATE - 60);

INSERT INTO cl_cidadao (nome, email, senha_hash, modal, dt_cadastro)
VALUES ('Bruno Lima', 'bruno.lima@exemplo.com',
        'pbkdf2$65536$mYm08xWlUKdO/3vC84t5Cg==$9bB72W1KPsexnT7Ag3rZCblXM9rGmtLAkuYHbXYlPFY=', 'CARRO', SYSDATE - 55);

INSERT INTO cl_cidadao (nome, email, senha_hash, modal, dt_cadastro)
VALUES ('Carla Mendes', 'carla.mendes@exemplo.com',
        'pbkdf2$65536$pn14GQEsSajxfHKXS/C9kA==$pwp5OwlckTCaXWS7MetkK9eQlwlkDZ2WA7AyeLn2RzA=', 'ONIBUS', SYSDATE - 50);

INSERT INTO cl_cidadao (nome, email, senha_hash, modal, dt_cadastro)
VALUES ('Diego Santos', 'diego.santos@exemplo.com',
        'pbkdf2$65536$RfuHYpW6irXdLMdQu1nVMQ==$9z0qu5e6z1Q23hNEQETxZioMRzuDbMPuhtgISOzv4/U=', 'APP', SYSDATE - 45);

INSERT INTO cl_cidadao (nome, email, senha_hash, modal, dt_cadastro)
VALUES ('Elisa Ferreira', 'elisa.ferreira@exemplo.com',
        'pbkdf2$65536$PWlZboS/99IOjA+dVmiHKQ==$mt64lB2KulbbtADu1aBQlQK5ulrnML7nDLAXNughPzg=', 'PEDESTRE', SYSDATE - 40);

INSERT INTO cl_cidadao (nome, email, senha_hash, modal, dt_cadastro)
VALUES ('Felipe Rocha', 'felipe.rocha@exemplo.com',
        'pbkdf2$65536$3Fh2VKjU4V4tQH4XofIM0A==$NEvkD+ST3QiZfGWbiuKurfUcmpcsxpn3BZLR2lVxqm0=', 'BIKE', SYSDATE - 35);

-- 5.4 Endereços salvos (atalhos de saída e destino; mesmas posições das rotas abaixo)
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Casa', 'CASA', 'Tatuapé', 1278.42, 1065.76, SYSDATE - 30 FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Trabalho', 'TRABALHO', 'Praça da Sé', 1044.35, 1109.19, SYSDATE - 30 FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Casa', 'CASA', 'Penha', 1415.84, 1008.15, SYSDATE - 30 FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Trabalho', 'TRABALHO', 'República', 1011.32, 1081.27, SYSDATE - 30 FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Casa dos meus pais', 'OUTRO', 'Santana', 1079.42, 897.37, SYSDATE - 30 FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Casa', 'CASA', 'Sapopemba', 1537.36, 1356.02, SYSDATE - 30 FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Faculdade', 'ESTUDO', 'Avenida Paulista', 954.63, 1158.38, SYSDATE - 30 FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Casa', 'CASA', 'Pinheiros', 803.34, 1183.19, SYSDATE - 30 FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Casa', 'CASA', 'Rua da Consolação', 986.85, 1094.57, SYSDATE - 30 FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Trabalho', 'TRABALHO', 'Estação Consolação', 935.47, 1141.54, SYSDATE - 30 FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Casa', 'CASA', 'Avenida Rebouças', 864.51, 1174.33, SYSDATE - 30 FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y, dt_criacao)
SELECT id_cidadao, 'Trabalho', 'TRABALHO', 'Avenida Brigadeiro Faria Lima', 846.98, 1270.94, SYSDATE - 30 FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';

-- 5.5 Rotas salvas (saída → destino) e as vias cadastradas que cada uma percorre.
-- X e Y são a posição no plano do mapa; as vias e a distância foram calculadas pela
-- própria tela de rotas, sobre o mapa do OpenStreetMap.
INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y, destino_descricao, destino_x, destino_y,
                     criterio, distancia_m, dt_criacao)
SELECT id_cidadao, 'Casa para o trabalho', 'Tatuapé', 1278.42, 1065.76,
       'Praça da Sé', 1044.35, 1109.19, 'RAPIDA', 7787, SYSDATE - 30
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'ana.souza@exemplo.com' AND r.nome = 'Casa para o trabalho' AND v.nome = 'Radial Leste';

INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y, destino_descricao, destino_x, destino_y,
                     criterio, distancia_m, dt_criacao)
SELECT id_cidadao, 'Entregas na Paulista', 'Tatuapé', 1278.42, 1065.76,
       'Avenida Paulista', 954.63, 1158.38, 'RAPIDA', 11403, SYSDATE - 20
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'ana.souza@exemplo.com' AND r.nome = 'Entregas na Paulista' AND v.nome = 'Radial Leste';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 2
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'ana.souza@exemplo.com' AND r.nome = 'Entregas na Paulista' AND v.nome = 'Avenida 23 de Maio';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 3
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'ana.souza@exemplo.com' AND r.nome = 'Entregas na Paulista' AND v.nome = 'Avenida Paulista';

INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y, destino_descricao, destino_x, destino_y,
                     criterio, distancia_m, dt_criacao)
SELECT id_cidadao, 'Caminho do trabalho', 'Penha', 1415.84, 1008.15,
       'República', 1011.32, 1081.27, 'RAPIDA', 15648, SYSDATE - 28
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'bruno.lima@exemplo.com' AND r.nome = 'Caminho do trabalho' AND v.nome = 'Radial Leste';

INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y, destino_descricao, destino_x, destino_y,
                     criterio, distancia_m, dt_criacao)
SELECT id_cidadao, 'Visita aos meus pais', 'Penha', 1415.84, 1008.15,
       'Santana', 1079.42, 897.37, 'RAPIDA', 15883, SYSDATE - 28
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'bruno.lima@exemplo.com' AND r.nome = 'Visita aos meus pais' AND v.nome = 'Radial Leste';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 2
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'bruno.lima@exemplo.com' AND r.nome = 'Visita aos meus pais' AND v.nome = 'Marginal Tietê';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 3
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'bruno.lima@exemplo.com' AND r.nome = 'Visita aos meus pais' AND v.nome = 'Avenida Cruzeiro do Sul';

INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y, destino_descricao, destino_x, destino_y,
                     criterio, distancia_m, dt_criacao)
SELECT id_cidadao, 'Ônibus para a faculdade', 'Sapopemba', 1537.36, 1356.02,
       'Avenida Paulista', 954.63, 1158.38, 'RAPIDA', 17803, SYSDATE - 25
  FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'carla.mendes@exemplo.com' AND r.nome = 'Ônibus para a faculdade' AND v.nome = 'Avenida Paulista';

INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y, destino_descricao, destino_x, destino_y,
                     criterio, distancia_m, dt_criacao)
SELECT id_cidadao, 'Corridas da manhã', 'Pinheiros', 803.34, 1183.19,
       'Brooklin', 795.19, 1387.04, 'RAPIDA', 8537, SYSDATE - 22
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'diego.santos@exemplo.com' AND r.nome = 'Corridas da manhã' AND v.nome = 'Avenida Brigadeiro Faria Lima';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 2
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'diego.santos@exemplo.com' AND r.nome = 'Corridas da manhã' AND v.nome = 'Marginal Pinheiros';

INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y, destino_descricao, destino_x, destino_y,
                     criterio, distancia_m, dt_criacao)
SELECT id_cidadao, 'A pé até o metrô', 'Rua da Consolação', 986.85, 1094.57,
       'Estação Consolação', 935.47, 1141.54, 'RAPIDA', 2071, SYSDATE - 18
  FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'elisa.ferreira@exemplo.com' AND r.nome = 'A pé até o metrô' AND v.nome = 'Rua da Consolação';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 2
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'elisa.ferreira@exemplo.com' AND r.nome = 'A pé até o metrô' AND v.nome = 'Avenida Paulista';

INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y, destino_descricao, destino_x, destino_y,
                     criterio, distancia_m, dt_criacao)
SELECT id_cidadao, 'Ciclovia até o trabalho', 'Avenida Rebouças', 864.51, 1174.33,
       'Avenida Brigadeiro Faria Lima', 846.98, 1270.94, 'RAPIDA', 3371, SYSDATE - 15
  FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1
  FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'felipe.rocha@exemplo.com' AND r.nome = 'Ciclovia até o trabalho' AND v.nome = 'Avenida Brigadeiro Faria Lima';

-- 5.6 Ocorrências ativas (aparecem no mapa de riscos e geram alertas)

-- Mesmo alagamento, risco diferente: 3,6 para o Bruno (carro) e 8,1 para a Ana (moto)
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Alagamento na Radial Leste, altura do Tatuapé',
       'Água acima do meio-fio nas duas faixas da direita. Carros passam devagar; motos estão derrapando.',
       'ALAGAMENTO', 3, 'CONFIRMADA', SYSDATE - 3/24, SYSDATE - 3/24 + 25/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Semáforo apagado na Consolação com a Paulista',
       'Semáforo do cruzamento apagado nos dois sentidos. Pedestres atravessando no meio dos carros.',
       'SEMAFORO_APAGADO', 3, 'EM_ATENDIMENTO', SYSDATE - 2/24, SYSDATE - 2/24 + 35/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Queda de árvore bloqueia faixa na Av. Rebouças',
       'Árvore de grande porte caiu sobre a faixa da direita e a ciclovia, perto da Rua Oscar Freire.',
       'QUEDA_ARVORE', 4, 'EM_ATENDIMENTO', SYSDATE - 5/24, SYSDATE - 5/24 + 95/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Colisão entre carro e bicicleta na Faria Lima',
       'Carro atingiu ciclista na faixa da direita. Resgate a caminho e trânsito parado no sentido Pinheiros.',
       'ACIDENTE', 4, 'CONFIRMADA', SYSDATE - 1/24, SYSDATE - 1/24 + 8/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Alagamento na pista local da Marginal Pinheiros',
       'Pista local completamente alagada perto da Ponte Cidade Jardim. Carros voltando de ré.',
       'ALAGAMENTO', 4, 'ABERTA', SYSDATE - 40/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Ponto de ônibus sem iluminação na Av. Sapopemba',
       'Ponto perto do número 9000 está sem luz há três noites. Passageiros esperam no escuro depois das 22h.',
       'INSEGURANCA', 3, 'ABERTA', SYSDATE - 20/24, id_cidadao
  FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Risco de deslizamento na Av. Engenheiro Caetano Álvares',
       'Trincas na encosta ao lado da calçada depois da chuva forte. Terra descendo para a pista.',
       'DESLIZAMENTO', 4, 'CONFIRMADA', SYSDATE - 6/24, SYSDATE - 6/24 + 30/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';

-- 5.7 Ocorrências encerradas (histórico: não aparecem no mapa)
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Colisão entre carro e moto na Marginal Tietê',
       'Acidente na pista expressa perto da Ponte das Bandeiras. Motociclista ferido.',
       'ACIDENTE', 4, 'RESOLVIDA', SYSDATE - 5, SYSDATE - 5 + 85/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Alagamento na Av. Inajar de Souza após temporal',
       'Avenida tomada pela água do córrego. Carros ilhados na altura da Avenida Itaberaba.',
       'ALAGAMENTO', 5, 'RESOLVIDA', SYSDATE - 10, SYSDATE - 10 + 6/24, id_cidadao
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Deslizamento de terra na Av. Francisco Morato',
       'Barranco cedeu e cobriu as duas faixas no sentido Taboão da Serra.',
       'DESLIZAMENTO', 5, 'RESOLVIDA', SYSDATE - 18, SYSDATE - 18 + 20/24, id_cidadao
  FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Semáforo apagado na Av. Santo Amaro',
       'Semáforo do cruzamento com a Avenida Juscelino Kubitschek apagado no horário de pico.',
       'SEMAFORO_APAGADO', 3, 'RESOLVIDA', SYSDATE - 7, SYSDATE - 7 + 70/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Ônibus quebrado ocupa faixa exclusiva na Av. Washington Luís',
       'Ônibus parado na faixa exclusiva. Os outros ônibus estão desviando para a faixa dos carros.',
       'VIA_BLOQUEADA', 2, 'RESOLVIDA', SYSDATE - 3, SYSDATE - 3 + 55/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Árvore caída sobre a calçada da Av. Interlagos',
       'Galhos grandes ocupam a calçada inteira; pedestres precisam andar pela pista.',
       'QUEDA_ARVORE', 3, 'RESOLVIDA', SYSDATE - 14, SYSDATE - 14 + 3/24, id_cidadao
  FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Alagamento no viaduto da Av. Aricanduva',
       'Ponto de alagamento recorrente sob o viaduto. Água na altura da roda dos carros.',
       'ALAGAMENTO', 4, 'RESOLVIDA', SYSDATE - 21, SYSDATE - 21 + 4/24, id_cidadao
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Assaltos frequentes em ponto de ônibus da Radial Leste',
       'Relatos de assaltos à noite no ponto em frente à estação Carrão. Ponto mal iluminado.',
       'INSEGURANCA', 3, 'RESOLVIDA', SYSDATE - 12, SYSDATE - 10, id_cidadao
  FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Moto derrapou em óleo na Av. 23 de Maio',
       'Mancha de óleo na pista sentido centro. Uma moto já caiu; outras estão freando em cima da mancha.',
       'ACIDENTE', 3, 'RESOLVIDA', SYSDATE - 1, SYSDATE - 1 + 40/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';

INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Obra sem sinalização na Av. Ricardo Jafet',
       'Faixa fechada por obra e sem cones de aviso antes do bloqueio.',
       'VIA_BLOQUEADA', 1, 'DESCARTADA', SYSDATE - 2, SYSDATE - 2 + 30/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';

-- 5.8 Vias atingidas por cada ocorrência (N:N)
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Alagamento na Radial Leste, altura do Tatuapé' AND v.nome = 'Radial Leste';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Semáforo apagado na Consolação com a Paulista' AND v.nome = 'Rua da Consolação';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Semáforo apagado na Consolação com a Paulista' AND v.nome = 'Avenida Paulista';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Queda de árvore bloqueia faixa na Av. Rebouças' AND v.nome = 'Avenida Rebouças';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Colisão entre carro e bicicleta na Faria Lima' AND v.nome = 'Avenida Brigadeiro Faria Lima';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Alagamento na pista local da Marginal Pinheiros' AND v.nome = 'Marginal Pinheiros';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'SEM_INTERDICAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Ponto de ônibus sem iluminação na Av. Sapopemba' AND v.nome = 'Avenida Sapopemba';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Risco de deslizamento na Av. Engenheiro Caetano Álvares' AND v.nome = 'Avenida Engenheiro Caetano Álvares';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Colisão entre carro e moto na Marginal Tietê' AND v.nome = 'Marginal Tietê';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Alagamento na Av. Inajar de Souza após temporal' AND v.nome = 'Avenida Inajar de Souza';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Deslizamento de terra na Av. Francisco Morato' AND v.nome = 'Avenida Francisco Morato';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Semáforo apagado na Av. Santo Amaro' AND v.nome = 'Avenida Santo Amaro';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Ônibus quebrado ocupa faixa exclusiva na Av. Washington Luís' AND v.nome = 'Avenida Washington Luís';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Árvore caída sobre a calçada da Av. Interlagos' AND v.nome = 'Avenida Interlagos';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Alagamento no viaduto da Av. Aricanduva' AND v.nome = 'Avenida Aricanduva';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Alagamento no viaduto da Av. Aricanduva' AND v.nome = 'Avenida Salim Farah Maluf';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'SEM_INTERDICAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Assaltos frequentes em ponto de ônibus da Radial Leste' AND v.nome = 'Radial Leste';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Moto derrapou em óleo na Av. 23 de Maio' AND v.nome = 'Avenida 23 de Maio';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Obra sem sinalização na Av. Ricardo Jafet' AND v.nome = 'Avenida Ricardo Jafet';

-- 5.9 Riscos de demonstração para a apresentação
-- Ocorrências ativas espalhadas pelas cinco zonas, com todos os tipos e todos os níveis de risco,
-- para o mapa de riscos ficar cheio no pitch. As vias têm o mesmo nome do mapa do OpenStreetMap
-- (é pelo nome que o mapa acha o traçado). Nenhuma delas fica nas rotas salvas dos cidadãos de
-- demonstração, então os alertas do README (Ana 8,1 alto; Bruno 3,6 baixo) continuam iguais.
-- Para apresentar sem elas, apague daqui até o COMMIT.

-- Vias novas
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Ipiranga', id_regiao FROM cl_regiao WHERE nome = 'Centro';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida São João', id_regiao FROM cl_regiao WHERE nome = 'Centro';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Rua Augusta', id_regiao FROM cl_regiao WHERE nome = 'Centro';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Braz Leme', id_regiao FROM cl_regiao WHERE nome = 'Zona Norte';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Água Fria', id_regiao FROM cl_regiao WHERE nome = 'Zona Norte';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Imirim', id_regiao FROM cl_regiao WHERE nome = 'Zona Norte';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida General Ataliba Leonel', id_regiao FROM cl_regiao WHERE nome = 'Zona Norte';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Nossa Senhora do Ó', id_regiao FROM cl_regiao WHERE nome = 'Zona Norte';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Jabaquara', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Cupecê', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Senador Teotônio Vilela', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Vereador José Diniz', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Nossa Senhora do Sabará', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Itaquera', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Ragueb Chohfi', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Marechal Tito', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Mateo Bei', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Jacu-Pêssego', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Amador Bueno da Veiga', id_regiao FROM cl_regiao WHERE nome = 'Zona Leste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Pompeia', id_regiao FROM cl_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Rua Teodoro Sampaio', id_regiao FROM cl_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Corifeu de Azevedo Marques', id_regiao FROM cl_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Escola Politécnica', id_regiao FROM cl_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Pedroso de Morais', id_regiao FROM cl_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cl_via (nome, id_regiao) SELECT 'Avenida Indianópolis', id_regiao FROM cl_regiao WHERE nome = 'Zona Sul';

-- Ocorrências
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Semáforo apagado na Ipiranga com a São João',
       'Cruzamento famoso sem semáforo nos dois sentidos. Muita gente atravessando entre os carros.',
       'SEMAFORO_APAGADO', 3, 'EM_ATENDIMENTO', SYSDATE - 70/1440, SYSDATE - 70/1440 + 20/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Feira de rua ocupa faixa da Avenida São João',
       'Barracas montadas na faixa da direita perto do Largo do Paissandu. Ônibus desviando.',
       'VIA_BLOQUEADA', 2, 'CONFIRMADA', SYSDATE - 150/1440, SYSDATE - 150/1440 + 30/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Trecho escuro e relatos de furto na Rua Augusta',
       'Postes apagados entre a Rua Peixoto Gomide e a Alameda Santos. Relatos de furto de celular à noite.',
       'INSEGURANCA', 3, 'ABERTA', SYSDATE - 5/24, id_cidadao
  FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Alagamento na Avenida Braz Leme perto do Campo de Marte',
       'Água cobrindo as duas faixas da direita depois da chuva. Motos passando pelo canteiro.',
       'ALAGAMENTO', 3, 'CONFIRMADA', SYSDATE - 95/1440, SYSDATE - 95/1440 + 15/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Galho grande pendurado sobre a Avenida Água Fria',
       'Galho rachado de uma tipuana balançando sobre a faixa da direita, perto da Rua Pedro Doll.',
       'QUEDA_ARVORE', 3, 'ABERTA', SYSDATE - 45/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Batida entre dois carros na Avenida Imirim',
       'Colisão traseira sem feridos. Carros parados na faixa da esquerda aguardando o guincho.',
       'ACIDENTE', 2, 'CONFIRMADA', SYSDATE - 35/1440, SYSDATE - 35/1440 + 10/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Semáforo piscando na General Ataliba Leonel',
       'Semáforo em amarelo piscante no cruzamento com a Rua Voluntários da Pátria.',
       'SEMAFORO_APAGADO', 2, 'ABERTA', SYSDATE - 2/24, id_cidadao
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Terra da encosta na Avenida Nossa Senhora do Ó',
       'Barranco desceu sobre a calçada e parte da faixa. Pedestres andando pela pista.',
       'DESLIZAMENTO', 3, 'CONFIRMADA', SYSDATE - 4/24, SYSDATE - 4/24 + 40/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Alagamento na Avenida Jabaquara perto da estação Conceição',
       'Água acima do meio-fio nos dois sentidos. Carros baixos ficando presos.',
       'ALAGAMENTO', 4, 'EM_ATENDIMENTO', SYSDATE - 80/1440, SYSDATE - 80/1440 + 25/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Trecho sem iluminação na Avenida Cupecê',
       'Cerca de 500 metros sem luz perto do Terminal Cidade Ademar. Ponto de ônibus no escuro.',
       'INSEGURANCA', 4, 'ABERTA', SYSDATE - 10/24, id_cidadao
  FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Caminhão tombado na Avenida Senador Teotônio Vilela',
       'Caminhão de entregas tombou na curva perto da Represa Billings. Uma faixa liberada.',
       'ACIDENTE', 4, 'CONFIRMADA', SYSDATE - 55/1440, SYSDATE - 55/1440 + 12/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Árvore caída no canteiro da Avenida Vereador José Diniz',
       'Árvore de médio porte caiu sobre o canteiro central e encosta na faixa da esquerda.',
       'QUEDA_ARVORE', 2, 'CONFIRMADA', SYSDATE - 3/24, SYSDATE - 3/24 + 50/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Obra fecha faixa da Avenida Nossa Senhora do Sabará',
       'Obra de saneamento com uma faixa fechada nos dois sentidos, sinalizada com cones.',
       'VIA_BLOQUEADA', 3, 'EM_ATENDIMENTO', SYSDATE - 12/24, SYSDATE - 12/24 + 60/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Semáforo apagado na Avenida Indianópolis',
       'Semáforo do cruzamento com a Avenida dos Bandeirantes apagado. Agente de trânsito ainda não chegou.',
       'SEMAFORO_APAGADO', 3, 'ABERTA', SYSDATE - 25/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Córrego transborda na Avenida Itaquera',
       'Córrego encheu e a água tomou a pista perto da Rua Victorio Santim. Motos derrapando.',
       'ALAGAMENTO', 3, 'CONFIRMADA', SYSDATE - 65/1440, SYSDATE - 65/1440 + 18/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Moto derrapa em areia na Avenida Ragueb Chohfi',
       'Areia de obra espalhada na pista. Um motociclista caiu, sem ferimentos graves.',
       'ACIDENTE', 3, 'ABERTA', SYSDATE - 40/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Ponto de ônibus sem luz na Avenida Marechal Tito',
       'Ponto vandalizado e sem iluminação perto da estação São Miguel Paulista.',
       'INSEGURANCA', 3, 'ABERTA', SYSDATE - 15/24, id_cidadao
  FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Árvore derruba fios na Avenida Mateo Bei',
       'Árvore caiu sobre a rede elétrica e fechou a avenida. Fios no chão.',
       'QUEDA_ARVORE', 4, 'EM_ATENDIMENTO', SYSDATE - 130/1440, SYSDATE - 130/1440 + 35/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Carreta quebrada na Avenida Jacu-Pêssego',
       'Carreta parada na faixa da direita no sentido Mauá. Trânsito lento por 2 km.',
       'VIA_BLOQUEADA', 4, 'CONFIRMADA', SYSDATE - 85/1440, SYSDATE - 85/1440 + 20/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Semáforo piscando na Avenida Amador Bueno da Veiga',
       'Semáforo em amarelo piscante perto do Shopping Penha.',
       'SEMAFORO_APAGADO', 2, 'ABERTA', SYSDATE - 50/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Água acumulada na descida da Avenida Pompeia',
       'Poça grande na faixa da direita na descida para a Marginal. Carros jogando água na calçada.',
       'ALAGAMENTO', 2, 'ABERTA', SYSDATE - 30/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Semáforo apagado na Rua Teodoro Sampaio',
       'Semáforo do cruzamento com a Rua Henrique Schaumann apagado no horário de saída das escolas.',
       'SEMAFORO_APAGADO', 3, 'EM_ATENDIMENTO', SYSDATE - 100/1440, SYSDATE - 100/1440 + 30/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Encosta cede ao lado da Avenida Corifeu de Azevedo Marques',
       'Muro de contenção cedeu e a terra fechou as faixas no sentido bairro.',
       'DESLIZAMENTO', 4, 'CONFIRMADA', SYSDATE - 160/1440, SYSDATE - 160/1440 + 30/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Colisão leve na Avenida Escola Politécnica',
       'Carro e moto se tocaram no semáforo. Sem feridos, veículos no acostamento.',
       'ACIDENTE', 2, 'ABERTA', SYSDATE - 20/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Galhos na ciclovia da Avenida Pedroso de Morais',
       'Galhos caídos bloqueiam a ciclovia perto da Praça Pan-Americana. Ciclistas indo para a pista.',
       'QUEDA_ARVORE', 3, 'CONFIRMADA', SYSDATE - 75/1440, SYSDATE - 75/1440 + 15/1440, id_cidadao
  FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';

-- Vias atingidas por cada ocorrência de demonstração (N:N)
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Semáforo apagado na Ipiranga com a São João' AND v.nome = 'Avenida Ipiranga';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Semáforo apagado na Ipiranga com a São João' AND v.nome = 'Avenida São João';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Feira de rua ocupa faixa da Avenida São João' AND v.nome = 'Avenida São João';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'SEM_INTERDICAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Trecho escuro e relatos de furto na Rua Augusta' AND v.nome = 'Rua Augusta';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Alagamento na Avenida Braz Leme perto do Campo de Marte' AND v.nome = 'Avenida Braz Leme';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Galho grande pendurado sobre a Avenida Água Fria' AND v.nome = 'Avenida Água Fria';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Batida entre dois carros na Avenida Imirim' AND v.nome = 'Avenida Imirim';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Semáforo piscando na General Ataliba Leonel' AND v.nome = 'Avenida General Ataliba Leonel';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Terra da encosta na Avenida Nossa Senhora do Ó' AND v.nome = 'Avenida Nossa Senhora do Ó';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Alagamento na Avenida Jabaquara perto da estação Conceição' AND v.nome = 'Avenida Jabaquara';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'SEM_INTERDICAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Trecho sem iluminação na Avenida Cupecê' AND v.nome = 'Avenida Cupecê';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Caminhão tombado na Avenida Senador Teotônio Vilela' AND v.nome = 'Avenida Senador Teotônio Vilela';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Árvore caída no canteiro da Avenida Vereador José Diniz' AND v.nome = 'Avenida Vereador José Diniz';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Obra fecha faixa da Avenida Nossa Senhora do Sabará' AND v.nome = 'Avenida Nossa Senhora do Sabará';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Semáforo apagado na Avenida Indianópolis' AND v.nome = 'Avenida Indianópolis';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Córrego transborda na Avenida Itaquera' AND v.nome = 'Avenida Itaquera';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Moto derrapa em areia na Avenida Ragueb Chohfi' AND v.nome = 'Avenida Ragueb Chohfi';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'SEM_INTERDICAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Ponto de ônibus sem luz na Avenida Marechal Tito' AND v.nome = 'Avenida Marechal Tito';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Árvore derruba fios na Avenida Mateo Bei' AND v.nome = 'Avenida Mateo Bei';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Carreta quebrada na Avenida Jacu-Pêssego' AND v.nome = 'Avenida Jacu-Pêssego';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Semáforo piscando na Avenida Amador Bueno da Veiga' AND v.nome = 'Avenida Amador Bueno da Veiga';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Água acumulada na descida da Avenida Pompeia' AND v.nome = 'Avenida Pompeia';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Semáforo apagado na Rua Teodoro Sampaio' AND v.nome = 'Rua Teodoro Sampaio';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Encosta cede ao lado da Avenida Corifeu de Azevedo Marques' AND v.nome = 'Avenida Corifeu de Azevedo Marques';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Colisão leve na Avenida Escola Politécnica' AND v.nome = 'Avenida Escola Politécnica';
INSERT INTO cl_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cl_ocorrencia o, cl_via v
 WHERE o.titulo = 'Galhos na ciclovia da Avenida Pedroso de Morais' AND v.nome = 'Avenida Pedroso de Morais';

COMMIT;


-- ---------------------------------------------------------------------
-- 6. Conferência: quantidade de linhas em cada tabela
-- ---------------------------------------------------------------------
SELECT 'CL_REGIAO' AS tabela, COUNT(*) AS linhas FROM cl_regiao
UNION ALL SELECT 'CL_VIA', COUNT(*) FROM cl_via
UNION ALL SELECT 'CL_CIDADAO', COUNT(*) FROM cl_cidadao
UNION ALL SELECT 'CL_OCORRENCIA', COUNT(*) FROM cl_ocorrencia
UNION ALL SELECT 'CL_OCORRENCIA_VIA', COUNT(*) FROM cl_ocorrencia_via
UNION ALL SELECT 'CL_ROTA', COUNT(*) FROM cl_rota
UNION ALL SELECT 'CL_ROTA_VIA', COUNT(*) FROM cl_rota_via
UNION ALL SELECT 'CL_ENDERECO', COUNT(*) FROM cl_endereco;
