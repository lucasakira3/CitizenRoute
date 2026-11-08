-- =====================================================================
-- CitizenRoute · Global Solution 2026/2 · CitizenTech (FIAP)
-- Script Oracle: criação das tabelas e carga inicial de dados.
--
-- Como executar (SQL Developer): abra este arquivo e use "Executar Script" (F5).
-- O script pode ser executado várias vezes. O bloco inicial apaga somente
-- as tabelas do projeto (prefixo CR_) antes de recriá-las.
--
-- Requer Oracle 12c ou superior (colunas IDENTITY e FETCH FIRST).
-- Senha de todos os usuários de demonstração: Citizen@2026
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Limpeza: remove as tabelas do projeto, se existirem
-- ---------------------------------------------------------------------
BEGIN
    FOR tabela IN (SELECT table_name
                     FROM user_tables
                    WHERE table_name IN ('CR_ACAO', 'CR_VIA_MONITORADA', 'CR_OCORRENCIA_VIA',
                                         'CR_OCORRENCIA', 'CR_GESTOR', 'CR_CIDADAO',
                                         'CR_USUARIO', 'CR_VIA', 'CR_REGIAO'))
    LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || tabela.table_name || ' CASCADE CONSTRAINTS PURGE';
    END LOOP;
END;
/


-- ---------------------------------------------------------------------
-- 2. Tabelas
-- ---------------------------------------------------------------------

-- Zonas da cidade
CREATE TABLE cr_regiao (
    id_regiao  NUMBER(10)   GENERATED ALWAYS AS IDENTITY,
    nome       VARCHAR2(60) NOT NULL,
    CONSTRAINT pk_cr_regiao PRIMARY KEY (id_regiao),
    CONSTRAINT uk_cr_regiao_nome UNIQUE (nome)
);

-- Avenidas, ruas e trechos. Cada via pertence a uma região (1:N).
CREATE TABLE cr_via (
    id_via     NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    nome       VARCHAR2(120) NOT NULL,
    id_regiao  NUMBER(10)    NOT NULL,
    CONSTRAINT pk_cr_via PRIMARY KEY (id_via),
    CONSTRAINT fk_cr_via_regiao FOREIGN KEY (id_regiao) REFERENCES cr_regiao (id_regiao),
    CONSTRAINT uk_cr_via_nome_regiao UNIQUE (nome, id_regiao)
);

-- Base de todos os usuários (superclasse). TIPO diz em qual tabela filha está o restante.
CREATE TABLE cr_usuario (
    id_usuario   NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    nome         VARCHAR2(100) NOT NULL,
    email        VARCHAR2(120) NOT NULL,
    senha_hash   VARCHAR2(200) NOT NULL,
    tipo         VARCHAR2(10)  NOT NULL,
    dt_cadastro  DATE          DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_cr_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_cr_usuario_email UNIQUE (email),
    CONSTRAINT ck_cr_usuario_tipo CHECK (tipo IN ('CIDADAO', 'GESTOR'))
);

-- Especialização: cidadão (1:1 com usuário). O modal muda o risco de cada ocorrência.
CREATE TABLE cr_cidadao (
    id_usuario  NUMBER(10)   NOT NULL,
    modal       VARCHAR2(10) NOT NULL,
    CONSTRAINT pk_cr_cidadao PRIMARY KEY (id_usuario),
    CONSTRAINT fk_cr_cidadao_usuario FOREIGN KEY (id_usuario)
        REFERENCES cr_usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_cr_cidadao_modal CHECK (modal IN ('CARRO', 'MOTO', 'ONIBUS', 'APP', 'PEDESTRE', 'BIKE'))
);

-- Especialização: gestor público (1:1 com usuário)
CREATE TABLE cr_gestor (
    id_usuario  NUMBER(10)   NOT NULL,
    orgao       VARCHAR2(15) NOT NULL,
    matricula   VARCHAR2(20) NOT NULL,
    CONSTRAINT pk_cr_gestor PRIMARY KEY (id_usuario),
    CONSTRAINT fk_cr_gestor_usuario FOREIGN KEY (id_usuario)
        REFERENCES cr_usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT uk_cr_gestor_matricula UNIQUE (matricula),
    CONSTRAINT ck_cr_gestor_orgao CHECK (orgao IN ('CET', 'DEFESA_CIVIL', 'SPTRANS', 'SUBPREFEITURA', 'GCM'))
);

-- Evento reportado por um cidadão (1:N cidadão → ocorrências)
CREATE TABLE cr_ocorrencia (
    id_ocorrencia     NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    titulo            VARCHAR2(100) NOT NULL,
    descricao         VARCHAR2(500) NOT NULL,
    tipo              VARCHAR2(20)  NOT NULL,
    gravidade         NUMBER(1)     NOT NULL,
    status            VARCHAR2(15)  DEFAULT 'ABERTA' NOT NULL,
    dt_registro       DATE          DEFAULT SYSDATE NOT NULL,
    dt_atualizacao    DATE,
    id_cidadao_autor  NUMBER(10)    NOT NULL,
    CONSTRAINT pk_cr_ocorrencia PRIMARY KEY (id_ocorrencia),
    CONSTRAINT fk_cr_ocorrencia_cidadao FOREIGN KEY (id_cidadao_autor) REFERENCES cr_cidadao (id_usuario),
    CONSTRAINT ck_cr_ocorrencia_tipo CHECK (tipo IN ('ACIDENTE', 'ALAGAMENTO', 'QUEDA_ARVORE', 'SEMAFORO_APAGADO',
                                                     'DESLIZAMENTO', 'VIA_BLOQUEADA', 'INSEGURANCA')),
    CONSTRAINT ck_cr_ocorrencia_gravidade CHECK (gravidade BETWEEN 1 AND 5),
    CONSTRAINT ck_cr_ocorrencia_status CHECK (status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO',
                                                         'RESOLVIDA', 'DESCARTADA'))
);

-- Associativa N:N entre ocorrência e via
CREATE TABLE cr_ocorrencia_via (
    id_ocorrencia  NUMBER(10)   NOT NULL,
    id_via         NUMBER(10)   NOT NULL,
    nivel_impacto  VARCHAR2(15) NOT NULL,
    CONSTRAINT pk_cr_ocorrencia_via PRIMARY KEY (id_ocorrencia, id_via),
    CONSTRAINT fk_cr_ocvia_ocorrencia FOREIGN KEY (id_ocorrencia)
        REFERENCES cr_ocorrencia (id_ocorrencia) ON DELETE CASCADE,
    CONSTRAINT fk_cr_ocvia_via FOREIGN KEY (id_via) REFERENCES cr_via (id_via),
    CONSTRAINT ck_cr_ocvia_impacto CHECK (nivel_impacto IN ('BLOQUEIO_TOTAL', 'PARCIAL', 'LENTIDAO', 'SEM_INTERDICAO'))
);

-- Associativa N:N entre cidadão e via (trajetos salvos)
CREATE TABLE cr_via_monitorada (
    id_cidadao   NUMBER(10)   NOT NULL,
    id_via       NUMBER(10)   NOT NULL,
    apelido      VARCHAR2(60) NOT NULL,
    dt_inclusao  DATE         DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_cr_via_monitorada PRIMARY KEY (id_cidadao, id_via),
    CONSTRAINT fk_cr_viamon_cidadao FOREIGN KEY (id_cidadao)
        REFERENCES cr_cidadao (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_cr_viamon_via FOREIGN KEY (id_via)
        REFERENCES cr_via (id_via) ON DELETE CASCADE
);

-- Resposta do gestor (1:N ocorrência → ações; 1:N gestor → ações)
CREATE TABLE cr_acao (
    id_acao        NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    id_ocorrencia  NUMBER(10)    NOT NULL,
    id_gestor      NUMBER(10)    NOT NULL,
    descricao      VARCHAR2(500) NOT NULL,
    status_novo    VARCHAR2(15),
    dt_acao        DATE          DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_cr_acao PRIMARY KEY (id_acao),
    CONSTRAINT fk_cr_acao_ocorrencia FOREIGN KEY (id_ocorrencia)
        REFERENCES cr_ocorrencia (id_ocorrencia) ON DELETE CASCADE,
    CONSTRAINT fk_cr_acao_gestor FOREIGN KEY (id_gestor) REFERENCES cr_gestor (id_usuario),
    CONSTRAINT ck_cr_acao_status CHECK (status_novo IN ('CONFIRMADA', 'EM_ATENDIMENTO', 'RESOLVIDA', 'DESCARTADA'))
);


-- ---------------------------------------------------------------------
-- 3. Índices nas chaves estrangeiras e filtros mais usados
-- ---------------------------------------------------------------------
CREATE INDEX ix_cr_via_regiao        ON cr_via (id_regiao);
CREATE INDEX ix_cr_ocorrencia_autor  ON cr_ocorrencia (id_cidadao_autor);
CREATE INDEX ix_cr_ocorrencia_status ON cr_ocorrencia (status);
CREATE INDEX ix_cr_ocvia_via         ON cr_ocorrencia_via (id_via);
CREATE INDEX ix_cr_viamon_via        ON cr_via_monitorada (id_via);
CREATE INDEX ix_cr_acao_ocorrencia   ON cr_acao (id_ocorrencia);
CREATE INDEX ix_cr_acao_gestor       ON cr_acao (id_gestor);


-- ---------------------------------------------------------------------
-- 4. Documentação das tabelas no dicionário de dados
-- ---------------------------------------------------------------------
COMMENT ON TABLE cr_regiao IS 'Zonas da cidade';
COMMENT ON TABLE cr_via IS 'Avenidas, ruas e trechos monitorados pelo sistema';
COMMENT ON TABLE cr_usuario IS 'Dados comuns a cidadãos e gestores';
COMMENT ON TABLE cr_cidadao IS 'Quem circula pela cidade; o modal muda o risco de cada ocorrência';
COMMENT ON TABLE cr_gestor IS 'Servidor público que confirma ocorrências e registra ações';
COMMENT ON TABLE cr_ocorrencia IS 'Evento reportado por um cidadão que afeta vias';
COMMENT ON TABLE cr_ocorrencia_via IS 'Associativa N:N: vias atingidas por cada ocorrência';
COMMENT ON TABLE cr_via_monitorada IS 'Associativa N:N: trajetos salvos pelos cidadãos';
COMMENT ON TABLE cr_acao IS 'Histórico de respostas dos gestores';
COMMENT ON COLUMN cr_usuario.senha_hash IS 'Hash PBKDF2 (pbkdf2$iteracoes$sal$hash). A senha nunca é gravada.';
COMMENT ON COLUMN cr_ocorrencia.gravidade IS 'De 1 (leve) a 5 (muito grave)';
COMMENT ON COLUMN cr_acao.status_novo IS 'Status resultante da ação; nulo quando é só um registro de andamento';


-- ---------------------------------------------------------------------
-- 5. Carga de dados
-- As chaves estrangeiras são buscadas por campos únicos (nome, e-mail,
-- título), então o script não depende dos ids gerados pelo IDENTITY.
-- Datas relativas (SYSDATE - n) mantêm a demonstração sempre recente:
-- SYSDATE - 1 é um dia atrás; SYSDATE - 3/24 são três horas atrás.
-- ---------------------------------------------------------------------

-- 5.1 Regiões
INSERT INTO cr_regiao (nome) VALUES ('Centro');
INSERT INTO cr_regiao (nome) VALUES ('Zona Norte');
INSERT INTO cr_regiao (nome) VALUES ('Zona Sul');
INSERT INTO cr_regiao (nome) VALUES ('Zona Leste');
INSERT INTO cr_regiao (nome) VALUES ('Zona Oeste');

-- 5.2 Vias
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Paulista', id_regiao FROM cr_regiao WHERE nome = 'Centro';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Rua da Consolação', id_regiao FROM cr_regiao WHERE nome = 'Centro';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida 23 de Maio', id_regiao FROM cr_regiao WHERE nome = 'Centro';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Prestes Maia', id_regiao FROM cr_regiao WHERE nome = 'Centro';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Marginal Tietê', id_regiao FROM cr_regiao WHERE nome = 'Zona Norte';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Cruzeiro do Sul', id_regiao FROM cr_regiao WHERE nome = 'Zona Norte';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Inajar de Souza', id_regiao FROM cr_regiao WHERE nome = 'Zona Norte';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Engenheiro Caetano Álvares', id_regiao FROM cr_regiao WHERE nome = 'Zona Norte';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Santo Amaro', id_regiao FROM cr_regiao WHERE nome = 'Zona Sul';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Washington Luís', id_regiao FROM cr_regiao WHERE nome = 'Zona Sul';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Interlagos', id_regiao FROM cr_regiao WHERE nome = 'Zona Sul';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Ricardo Jafet', id_regiao FROM cr_regiao WHERE nome = 'Zona Sul';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Radial Leste', id_regiao FROM cr_regiao WHERE nome = 'Zona Leste';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Aricanduva', id_regiao FROM cr_regiao WHERE nome = 'Zona Leste';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Sapopemba', id_regiao FROM cr_regiao WHERE nome = 'Zona Leste';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Salim Farah Maluf', id_regiao FROM cr_regiao WHERE nome = 'Zona Leste';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Marginal Pinheiros', id_regiao FROM cr_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Rebouças', id_regiao FROM cr_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Brigadeiro Faria Lima', id_regiao FROM cr_regiao WHERE nome = 'Zona Oeste';
INSERT INTO cr_via (nome, id_regiao) SELECT 'Avenida Francisco Morato', id_regiao FROM cr_regiao WHERE nome = 'Zona Oeste';

-- 5.3 Cidadãos: um para cada modal
INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Ana Souza', 'ana.souza@exemplo.com',
        'pbkdf2$65536$mFrQjXjWLY5bwzc2daOtYw==$0fUOtRxmU00PCLgCrU+Iaq+zvnXNunGHFFvJJdp5fFg=', 'CIDADAO', SYSDATE - 60);
INSERT INTO cr_cidadao (id_usuario, modal) SELECT id_usuario, 'MOTO' FROM cr_usuario WHERE email = 'ana.souza@exemplo.com';

INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Bruno Lima', 'bruno.lima@exemplo.com',
        'pbkdf2$65536$mYm08xWlUKdO/3vC84t5Cg==$9bB72W1KPsexnT7Ag3rZCblXM9rGmtLAkuYHbXYlPFY=', 'CIDADAO', SYSDATE - 55);
INSERT INTO cr_cidadao (id_usuario, modal) SELECT id_usuario, 'CARRO' FROM cr_usuario WHERE email = 'bruno.lima@exemplo.com';

INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Carla Mendes', 'carla.mendes@exemplo.com',
        'pbkdf2$65536$pn14GQEsSajxfHKXS/C9kA==$pwp5OwlckTCaXWS7MetkK9eQlwlkDZ2WA7AyeLn2RzA=', 'CIDADAO', SYSDATE - 50);
INSERT INTO cr_cidadao (id_usuario, modal) SELECT id_usuario, 'ONIBUS' FROM cr_usuario WHERE email = 'carla.mendes@exemplo.com';

INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Diego Santos', 'diego.santos@exemplo.com',
        'pbkdf2$65536$RfuHYpW6irXdLMdQu1nVMQ==$9z0qu5e6z1Q23hNEQETxZioMRzuDbMPuhtgISOzv4/U=', 'CIDADAO', SYSDATE - 45);
INSERT INTO cr_cidadao (id_usuario, modal) SELECT id_usuario, 'APP' FROM cr_usuario WHERE email = 'diego.santos@exemplo.com';

INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Elisa Ferreira', 'elisa.ferreira@exemplo.com',
        'pbkdf2$65536$PWlZboS/99IOjA+dVmiHKQ==$mt64lB2KulbbtADu1aBQlQK5ulrnML7nDLAXNughPzg=', 'CIDADAO', SYSDATE - 40);
INSERT INTO cr_cidadao (id_usuario, modal) SELECT id_usuario, 'PEDESTRE' FROM cr_usuario WHERE email = 'elisa.ferreira@exemplo.com';

INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Felipe Rocha', 'felipe.rocha@exemplo.com',
        'pbkdf2$65536$3Fh2VKjU4V4tQH4XofIM0A==$NEvkD+ST3QiZfGWbiuKurfUcmpcsxpn3BZLR2lVxqm0=', 'CIDADAO', SYSDATE - 35);
INSERT INTO cr_cidadao (id_usuario, modal) SELECT id_usuario, 'BIKE' FROM cr_usuario WHERE email = 'felipe.rocha@exemplo.com';

-- 5.4 Gestores: um por órgão
INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Gabriela Prado', 'gabriela.prado@exemplo.com',
        'pbkdf2$65536$/SUpUr/UDTsyl0hTSwlAUA==$BZlvvxq9lreJCKQCmuai+8DTnv/QcL6H4dN2kB4gztE=', 'GESTOR', SYSDATE - 90);
INSERT INTO cr_gestor (id_usuario, orgao, matricula)
SELECT id_usuario, 'CET', 'CET-10234' FROM cr_usuario WHERE email = 'gabriela.prado@exemplo.com';

INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Henrique Alves', 'henrique.alves@exemplo.com',
        'pbkdf2$65536$GNqZ1I+kEAaY+b28hIU4JA==$tyk+e+ObU6fWRqtwlrdz4c1RK3h9YSc84741G+7zaDc=', 'GESTOR', SYSDATE - 90);
INSERT INTO cr_gestor (id_usuario, orgao, matricula)
SELECT id_usuario, 'DEFESA_CIVIL', 'DC-20871' FROM cr_usuario WHERE email = 'henrique.alves@exemplo.com';

INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Isabela Nunes', 'isabela.nunes@exemplo.com',
        'pbkdf2$65536$vw6V5qimSkgVOhQxFSI7kw==$wy5evvq/qvCZNoJX9FJA+s2005+PCPtvYP0fNsK9tGw=', 'GESTOR', SYSDATE - 90);
INSERT INTO cr_gestor (id_usuario, orgao, matricula)
SELECT id_usuario, 'SPTRANS', 'SPT-33019' FROM cr_usuario WHERE email = 'isabela.nunes@exemplo.com';

INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('João Ribeiro', 'joao.ribeiro@exemplo.com',
        'pbkdf2$65536$Js/V/raTOo8XJQxcMxAh2w==$tH82GhPzbuptTULhS8ZxKKWqJ39raLCfeRyUwp3mAFs=', 'GESTOR', SYSDATE - 90);
INSERT INTO cr_gestor (id_usuario, orgao, matricula)
SELECT id_usuario, 'SUBPREFEITURA', 'SUB-41552' FROM cr_usuario WHERE email = 'joao.ribeiro@exemplo.com';

INSERT INTO cr_usuario (nome, email, senha_hash, tipo, dt_cadastro)
VALUES ('Larissa Costa', 'larissa.costa@exemplo.com',
        'pbkdf2$65536$IoB/mcR8yR0Yzx1TrdpKRw==$kFeKTWl9sEOLhvuxLOjj/uvaCyUUg3dmKbIRLDbpMU0=', 'GESTOR', SYSDATE - 90);
INSERT INTO cr_gestor (id_usuario, orgao, matricula)
SELECT id_usuario, 'GCM', 'GCM-50318' FROM cr_usuario WHERE email = 'larissa.costa@exemplo.com';

-- 5.5 Vias monitoradas (trajetos salvos)
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Volta para casa', SYSDATE - 30
  FROM cr_usuario u, cr_via v WHERE u.email = 'ana.souza@exemplo.com' AND v.nome = 'Radial Leste';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Rota das entregas', SYSDATE - 30
  FROM cr_usuario u, cr_via v WHERE u.email = 'ana.souza@exemplo.com' AND v.nome = 'Avenida Aricanduva';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Entregas no centro', SYSDATE - 20
  FROM cr_usuario u, cr_via v WHERE u.email = 'ana.souza@exemplo.com' AND v.nome = 'Avenida Paulista';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Caminho do trabalho', SYSDATE - 28
  FROM cr_usuario u, cr_via v WHERE u.email = 'bruno.lima@exemplo.com' AND v.nome = 'Radial Leste';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Visita aos meus pais', SYSDATE - 28
  FROM cr_usuario u, cr_via v WHERE u.email = 'bruno.lima@exemplo.com' AND v.nome = 'Marginal Tietê';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Ônibus para a faculdade', SYSDATE - 25
  FROM cr_usuario u, cr_via v WHERE u.email = 'carla.mendes@exemplo.com' AND v.nome = 'Avenida Sapopemba';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Estágio', SYSDATE - 25
  FROM cr_usuario u, cr_via v WHERE u.email = 'carla.mendes@exemplo.com' AND v.nome = 'Avenida Paulista';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Corridas da manhã', SYSDATE - 22
  FROM cr_usuario u, cr_via v WHERE u.email = 'diego.santos@exemplo.com' AND v.nome = 'Marginal Pinheiros';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Corridas no horário comercial', SYSDATE - 22
  FROM cr_usuario u, cr_via v WHERE u.email = 'diego.santos@exemplo.com' AND v.nome = 'Avenida Brigadeiro Faria Lima';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'A pé até o metrô', SYSDATE - 18
  FROM cr_usuario u, cr_via v WHERE u.email = 'elisa.ferreira@exemplo.com' AND v.nome = 'Rua da Consolação';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Trabalho', SYSDATE - 18
  FROM cr_usuario u, cr_via v WHERE u.email = 'elisa.ferreira@exemplo.com' AND v.nome = 'Avenida Paulista';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Ciclovia até o trabalho', SYSDATE - 15
  FROM cr_usuario u, cr_via v WHERE u.email = 'felipe.rocha@exemplo.com' AND v.nome = 'Avenida Rebouças';
INSERT INTO cr_via_monitorada (id_cidadao, id_via, apelido, dt_inclusao)
SELECT u.id_usuario, v.id_via, 'Ciclovia da Faria Lima', SYSDATE - 15
  FROM cr_usuario u, cr_via v WHERE u.email = 'felipe.rocha@exemplo.com' AND v.nome = 'Avenida Brigadeiro Faria Lima';

-- 5.6 Ocorrências ativas (geram alertas)

-- Mesmo alagamento, risco diferente: 3,6 para o Bruno (carro) e 8,1 para a Ana (moto)
INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Alagamento na Radial Leste, altura do Tatuapé',
       'Água acima do meio-fio nas duas faixas da direita. Carros passam devagar; motos estão derrapando.',
       'ALAGAMENTO', 3, 'CONFIRMADA', SYSDATE - 3/24, SYSDATE - 3/24 + 25/1440, id_usuario
  FROM cr_usuario WHERE email = 'bruno.lima@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Semáforo apagado na Consolação com a Paulista',
       'Semáforo do cruzamento apagado nos dois sentidos. Pedestres atravessando no meio dos carros.',
       'SEMAFORO_APAGADO', 3, 'EM_ATENDIMENTO', SYSDATE - 2/24, SYSDATE - 2/24 + 35/1440, id_usuario
  FROM cr_usuario WHERE email = 'elisa.ferreira@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Queda de árvore bloqueia faixa na Av. Rebouças',
       'Árvore de grande porte caiu sobre a faixa da direita e a ciclovia, perto da Rua Oscar Freire.',
       'QUEDA_ARVORE', 4, 'EM_ATENDIMENTO', SYSDATE - 5/24, SYSDATE - 5/24 + 95/1440, id_usuario
  FROM cr_usuario WHERE email = 'felipe.rocha@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Colisão entre carro e bicicleta na Faria Lima',
       'Carro atingiu ciclista na faixa da direita. Resgate a caminho e trânsito parado no sentido Pinheiros.',
       'ACIDENTE', 4, 'CONFIRMADA', SYSDATE - 1/24, SYSDATE - 1/24 + 8/1440, id_usuario
  FROM cr_usuario WHERE email = 'diego.santos@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Alagamento na pista local da Marginal Pinheiros',
       'Pista local completamente alagada perto da Ponte Cidade Jardim. Carros voltando de ré.',
       'ALAGAMENTO', 4, 'ABERTA', SYSDATE - 40/1440, id_usuario
  FROM cr_usuario WHERE email = 'diego.santos@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, id_cidadao_autor)
SELECT 'Ponto de ônibus sem iluminação na Av. Sapopemba',
       'Ponto perto do número 9000 está sem luz há três noites. Passageiros esperam no escuro depois das 22h.',
       'INSEGURANCA', 3, 'ABERTA', SYSDATE - 20/24, id_usuario
  FROM cr_usuario WHERE email = 'carla.mendes@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Risco de deslizamento na Av. Engenheiro Caetano Álvares',
       'Trincas na encosta ao lado da calçada depois da chuva forte. Terra descendo para a pista.',
       'DESLIZAMENTO', 4, 'CONFIRMADA', SYSDATE - 6/24, SYSDATE - 6/24 + 30/1440, id_usuario
  FROM cr_usuario WHERE email = 'ana.souza@exemplo.com';

-- 5.7 Ocorrências encerradas (histórico para os relatórios)
INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Colisão entre carro e moto na Marginal Tietê',
       'Acidente na pista expressa perto da Ponte das Bandeiras. Motociclista ferido.',
       'ACIDENTE', 4, 'RESOLVIDA', SYSDATE - 5, SYSDATE - 5 + 85/1440, id_usuario
  FROM cr_usuario WHERE email = 'bruno.lima@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Alagamento na Av. Inajar de Souza após temporal',
       'Avenida tomada pela água do córrego. Carros ilhados na altura da Avenida Itaberaba.',
       'ALAGAMENTO', 5, 'RESOLVIDA', SYSDATE - 10, SYSDATE - 10 + 6/24, id_usuario
  FROM cr_usuario WHERE email = 'ana.souza@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Deslizamento de terra na Av. Francisco Morato',
       'Barranco cedeu e cobriu as duas faixas no sentido Taboão da Serra.',
       'DESLIZAMENTO', 5, 'RESOLVIDA', SYSDATE - 18, SYSDATE - 18 + 20/24, id_usuario
  FROM cr_usuario WHERE email = 'felipe.rocha@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Semáforo apagado na Av. Santo Amaro',
       'Semáforo do cruzamento com a Avenida Juscelino Kubitschek apagado no horário de pico.',
       'SEMAFORO_APAGADO', 3, 'RESOLVIDA', SYSDATE - 7, SYSDATE - 7 + 70/1440, id_usuario
  FROM cr_usuario WHERE email = 'elisa.ferreira@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Ônibus quebrado ocupa faixa exclusiva na Av. Washington Luís',
       'Ônibus parado na faixa exclusiva. Os outros ônibus estão desviando para a faixa dos carros.',
       'VIA_BLOQUEADA', 2, 'RESOLVIDA', SYSDATE - 3, SYSDATE - 3 + 55/1440, id_usuario
  FROM cr_usuario WHERE email = 'carla.mendes@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Árvore caída sobre a calçada da Av. Interlagos',
       'Galhos grandes ocupam a calçada inteira; pedestres precisam andar pela pista.',
       'QUEDA_ARVORE', 3, 'RESOLVIDA', SYSDATE - 14, SYSDATE - 14 + 3/24, id_usuario
  FROM cr_usuario WHERE email = 'elisa.ferreira@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Alagamento no viaduto da Av. Aricanduva',
       'Ponto de alagamento recorrente sob o viaduto. Água na altura da roda dos carros.',
       'ALAGAMENTO', 4, 'RESOLVIDA', SYSDATE - 21, SYSDATE - 21 + 4/24, id_usuario
  FROM cr_usuario WHERE email = 'ana.souza@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Assaltos frequentes em ponto de ônibus da Radial Leste',
       'Relatos de assaltos à noite no ponto em frente à estação Carrão. Ponto mal iluminado.',
       'INSEGURANCA', 3, 'RESOLVIDA', SYSDATE - 12, SYSDATE - 10, id_usuario
  FROM cr_usuario WHERE email = 'carla.mendes@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Moto derrapou em óleo na Av. 23 de Maio',
       'Mancha de óleo na pista sentido centro. Uma moto já caiu; outras estão freando em cima da mancha.',
       'ACIDENTE', 3, 'RESOLVIDA', SYSDATE - 1, SYSDATE - 1 + 40/1440, id_usuario
  FROM cr_usuario WHERE email = 'ana.souza@exemplo.com';

INSERT INTO cr_ocorrencia (titulo, descricao, tipo, gravidade, status, dt_registro, dt_atualizacao, id_cidadao_autor)
SELECT 'Obra sem sinalização na Av. Ricardo Jafet',
       'Faixa fechada por obra e sem cones de aviso antes do bloqueio.',
       'VIA_BLOQUEADA', 1, 'DESCARTADA', SYSDATE - 2, SYSDATE - 2 + 30/1440, id_usuario
  FROM cr_usuario WHERE email = 'diego.santos@exemplo.com';

-- 5.8 Vias atingidas por cada ocorrência (N:N)
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Alagamento na Radial Leste, altura do Tatuapé' AND v.nome = 'Radial Leste';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Semáforo apagado na Consolação com a Paulista' AND v.nome = 'Rua da Consolação';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Semáforo apagado na Consolação com a Paulista' AND v.nome = 'Avenida Paulista';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Queda de árvore bloqueia faixa na Av. Rebouças' AND v.nome = 'Avenida Rebouças';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Colisão entre carro e bicicleta na Faria Lima' AND v.nome = 'Avenida Brigadeiro Faria Lima';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Alagamento na pista local da Marginal Pinheiros' AND v.nome = 'Marginal Pinheiros';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'SEM_INTERDICAO' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Ponto de ônibus sem iluminação na Av. Sapopemba' AND v.nome = 'Avenida Sapopemba';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Risco de deslizamento na Av. Engenheiro Caetano Álvares' AND v.nome = 'Avenida Engenheiro Caetano Álvares';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Colisão entre carro e moto na Marginal Tietê' AND v.nome = 'Marginal Tietê';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Alagamento na Av. Inajar de Souza após temporal' AND v.nome = 'Avenida Inajar de Souza';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Deslizamento de terra na Av. Francisco Morato' AND v.nome = 'Avenida Francisco Morato';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Semáforo apagado na Av. Santo Amaro' AND v.nome = 'Avenida Santo Amaro';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Ônibus quebrado ocupa faixa exclusiva na Av. Washington Luís' AND v.nome = 'Avenida Washington Luís';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Árvore caída sobre a calçada da Av. Interlagos' AND v.nome = 'Avenida Interlagos';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'BLOQUEIO_TOTAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Alagamento no viaduto da Av. Aricanduva' AND v.nome = 'Avenida Aricanduva';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Alagamento no viaduto da Av. Aricanduva' AND v.nome = 'Avenida Salim Farah Maluf';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'SEM_INTERDICAO' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Assaltos frequentes em ponto de ônibus da Radial Leste' AND v.nome = 'Radial Leste';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'PARCIAL' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Moto derrapou em óleo na Av. 23 de Maio' AND v.nome = 'Avenida 23 de Maio';
INSERT INTO cr_ocorrencia_via (id_ocorrencia, id_via, nivel_impacto)
SELECT o.id_ocorrencia, v.id_via, 'LENTIDAO' FROM cr_ocorrencia o, cr_via v
 WHERE o.titulo = 'Obra sem sinalização na Av. Ricardo Jafet' AND v.nome = 'Avenida Ricardo Jafet';

-- 5.9 Ações dos gestores (histórico de cada ocorrência)
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Alagamento confirmado pelas câmeras. Equipe acionada para desobstruir os bueiros.',
       'CONFIRMADA', SYSDATE - 3/24 + 25/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Alagamento na Radial Leste, altura do Tatuapé' AND u.email = 'henrique.alves@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Falha confirmada pela central de semáforos.', 'CONFIRMADA', SYSDATE - 2/24 + 12/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Semáforo apagado na Consolação com a Paulista' AND u.email = 'gabriela.prado@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Agentes da CET orientando o trânsito e a travessia até o reparo.',
       'EM_ATENDIMENTO', SYSDATE - 2/24 + 35/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Semáforo apagado na Consolação com a Paulista' AND u.email = 'gabriela.prado@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Queda confirmada. Equipe de poda acionada.', 'CONFIRMADA', SYSDATE - 5/24 + 40/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Queda de árvore bloqueia faixa na Av. Rebouças' AND u.email = 'joao.ribeiro@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Corte e remoção em andamento. Ciclovia interditada no trecho.',
       'EM_ATENDIMENTO', SYSDATE - 5/24 + 95/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Queda de árvore bloqueia faixa na Av. Rebouças' AND u.email = 'joao.ribeiro@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Viatura e resgate no local. Faixa da direita interditada.', 'CONFIRMADA',
       SYSDATE - 1/24 + 8/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Colisão entre carro e bicicleta na Faria Lima' AND u.email = 'gabriela.prado@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Vistoria confirmou trincas na encosta. Calçada isolada preventivamente.',
       'CONFIRMADA', SYSDATE - 6/24 + 30/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Risco de deslizamento na Av. Engenheiro Caetano Álvares' AND u.email = 'henrique.alves@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Acidente confirmado pelas câmeras da CET.', 'CONFIRMADA', SYSDATE - 5 + 10/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Colisão entre carro e moto na Marginal Tietê' AND u.email = 'gabriela.prado@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Equipe de apoio e guincho a caminho.', 'EM_ATENDIMENTO', SYSDATE - 5 + 18/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Colisão entre carro e moto na Marginal Tietê' AND u.email = 'gabriela.prado@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Veículos removidos e faixas liberadas.', 'RESOLVIDA', SYSDATE - 5 + 85/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Colisão entre carro e moto na Marginal Tietê' AND u.email = 'gabriela.prado@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Alagamento confirmado. Alerta enviado para a região.', 'CONFIRMADA',
       SYSDATE - 10 + 20/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Alagamento na Av. Inajar de Souza após temporal' AND u.email = 'henrique.alves@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Via bloqueada com desvio pela Avenida Itaberaba.', 'EM_ATENDIMENTO',
       SYSDATE - 10 + 45/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Alagamento na Av. Inajar de Souza após temporal' AND u.email = 'henrique.alves@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Água escoou. Via limpa e reaberta.', 'RESOLVIDA', SYSDATE - 10 + 6/24
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Alagamento na Av. Inajar de Souza após temporal' AND u.email = 'henrique.alves@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Deslizamento confirmado. Pista interditada nos dois sentidos.', 'CONFIRMADA',
       SYSDATE - 18 + 15/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Deslizamento de terra na Av. Francisco Morato' AND u.email = 'henrique.alves@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Máquinas retirando a terra; engenheiros avaliam a encosta.', 'EM_ATENDIMENTO',
       SYSDATE - 18 + 30/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Deslizamento de terra na Av. Francisco Morato' AND u.email = 'henrique.alves@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Encosta contida e pista liberada.', 'RESOLVIDA', SYSDATE - 18 + 20/24
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Deslizamento de terra na Av. Francisco Morato' AND u.email = 'henrique.alves@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Falha confirmada; técnico enviado.', 'CONFIRMADA', SYSDATE - 7 + 22/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Semáforo apagado na Av. Santo Amaro' AND u.email = 'gabriela.prado@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Controlador substituído. Semáforo operando normalmente.', 'RESOLVIDA',
       SYSDATE - 7 + 70/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Semáforo apagado na Av. Santo Amaro' AND u.email = 'gabriela.prado@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Operadora acionada para remover o veículo.', 'CONFIRMADA', SYSDATE - 3 + 15/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Ônibus quebrado ocupa faixa exclusiva na Av. Washington Luís' AND u.email = 'isabela.nunes@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Ônibus guinchado e faixa exclusiva liberada.', 'RESOLVIDA', SYSDATE - 3 + 55/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Ônibus quebrado ocupa faixa exclusiva na Av. Washington Luís' AND u.email = 'isabela.nunes@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Queda confirmada. Equipe de poda programada.', 'CONFIRMADA', SYSDATE - 14 + 50/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Árvore caída sobre a calçada da Av. Interlagos' AND u.email = 'joao.ribeiro@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Galhos removidos e calçada liberada.', 'RESOLVIDA', SYSDATE - 14 + 3/24
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Árvore caída sobre a calçada da Av. Interlagos' AND u.email = 'joao.ribeiro@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Ponto de alagamento conhecido. Equipe enviada.', 'CONFIRMADA', SYSDATE - 21 + 18/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Alagamento no viaduto da Av. Aricanduva' AND u.email = 'henrique.alves@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Bombeamento concluído e viaduto liberado.', 'RESOLVIDA', SYSDATE - 21 + 4/24
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Alagamento no viaduto da Av. Aricanduva' AND u.email = 'henrique.alves@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Ronda reforçada no ponto entre 20h e 0h.', 'CONFIRMADA', SYSDATE - 12 + 2/24
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Assaltos frequentes em ponto de ônibus da Radial Leste' AND u.email = 'larissa.costa@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Iluminação recuperada pela prefeitura. Ronda mantida por 30 dias.', 'RESOLVIDA',
       SYSDATE - 10
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Assaltos frequentes em ponto de ônibus da Radial Leste' AND u.email = 'larissa.costa@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Mancha de óleo confirmada. Equipe de limpeza acionada.', 'CONFIRMADA',
       SYSDATE - 1 + 9/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Moto derrapou em óleo na Av. 23 de Maio' AND u.email = 'gabriela.prado@exemplo.com';
INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Pista limpa com serragem; trânsito normalizado.', 'RESOLVIDA', SYSDATE - 1 + 40/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Moto derrapou em óleo na Av. 23 de Maio' AND u.email = 'gabriela.prado@exemplo.com';

INSERT INTO cr_acao (id_ocorrencia, id_gestor, descricao, status_novo, dt_acao)
SELECT o.id_ocorrencia, u.id_usuario, 'Obra autorizada e sinalizada no local. Relato não procede.', 'DESCARTADA',
       SYSDATE - 2 + 30/1440
  FROM cr_ocorrencia o, cr_usuario u
 WHERE o.titulo = 'Obra sem sinalização na Av. Ricardo Jafet' AND u.email = 'gabriela.prado@exemplo.com';

COMMIT;


-- ---------------------------------------------------------------------
-- 6. Conferência: quantidade de linhas em cada tabela
-- ---------------------------------------------------------------------
SELECT 'CR_REGIAO' AS tabela, COUNT(*) AS linhas FROM cr_regiao
UNION ALL SELECT 'CR_VIA', COUNT(*) FROM cr_via
UNION ALL SELECT 'CR_USUARIO', COUNT(*) FROM cr_usuario
UNION ALL SELECT 'CR_CIDADAO', COUNT(*) FROM cr_cidadao
UNION ALL SELECT 'CR_GESTOR', COUNT(*) FROM cr_gestor
UNION ALL SELECT 'CR_OCORRENCIA', COUNT(*) FROM cr_ocorrencia
UNION ALL SELECT 'CR_OCORRENCIA_VIA', COUNT(*) FROM cr_ocorrencia_via
UNION ALL SELECT 'CR_VIA_MONITORADA', COUNT(*) FROM cr_via_monitorada
UNION ALL SELECT 'CR_ACAO', COUNT(*) FROM cr_acao;


-- ---------------------------------------------------------------------
-- 7. Relatórios (as mesmas consultas usadas pela API em RelatorioDao)
-- ---------------------------------------------------------------------

-- 7.1 Vias mais críticas
SELECT v.nome AS via, r.nome AS regiao,
       COUNT(o.id_ocorrencia) AS total,
       SUM(CASE WHEN o.status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO') THEN 1 ELSE 0 END) AS ativas,
       ROUND(AVG(o.gravidade), 1) AS gravidade_media,
       MAX(o.gravidade) AS gravidade_maxima
  FROM cr_via v
  JOIN cr_regiao r ON r.id_regiao = v.id_regiao
  JOIN cr_ocorrencia_via ov ON ov.id_via = v.id_via
  JOIN cr_ocorrencia o ON o.id_ocorrencia = ov.id_ocorrencia
 WHERE o.status <> 'DESCARTADA'
 GROUP BY v.id_via, v.nome, r.nome
 ORDER BY ativas DESC, total DESC, gravidade_media DESC
 FETCH FIRST 10 ROWS ONLY;

-- 7.2 Ocorrências por região e tipo
SELECT r.nome AS regiao, o.tipo,
       COUNT(DISTINCT o.id_ocorrencia) AS total,
       COUNT(DISTINCT CASE WHEN o.status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO')
                           THEN o.id_ocorrencia END) AS ativas
  FROM cr_ocorrencia o
  JOIN cr_ocorrencia_via ov ON ov.id_ocorrencia = o.id_ocorrencia
  JOIN cr_via v ON v.id_via = ov.id_via
  JOIN cr_regiao r ON r.id_regiao = v.id_regiao
 WHERE o.status <> 'DESCARTADA'
 GROUP BY r.nome, o.tipo
 ORDER BY r.nome, total DESC;

-- 7.3 Tempo médio até a primeira resposta, por órgão (em minutos)
SELECT p.orgao,
       COUNT(*) AS atendidas,
       ROUND(AVG((p.primeira_acao - o.dt_registro) * 1440), 1) AS minutos_medios,
       SUM(CASE WHEN o.status = 'RESOLVIDA' THEN 1 ELSE 0 END) AS resolvidas
  FROM (SELECT a.id_ocorrencia, g.orgao, MIN(a.dt_acao) AS primeira_acao
          FROM cr_acao a
          JOIN cr_gestor g ON g.id_usuario = a.id_gestor
         GROUP BY a.id_ocorrencia, g.orgao) p
  JOIN cr_ocorrencia o ON o.id_ocorrencia = p.id_ocorrencia
 GROUP BY p.orgao
 ORDER BY minutos_medios;

-- 7.4 Exposição por modal: cidadãos, vias monitoradas e ocorrências ativas nessas vias
SELECT c.modal,
       COUNT(DISTINCT c.id_usuario) AS cidadaos,
       COUNT(DISTINCT CASE WHEN vm.id_via IS NOT NULL
                           THEN vm.id_cidadao || '-' || vm.id_via END) AS vias_monitoradas,
       COUNT(DISTINCT CASE WHEN o.status IN ('ABERTA', 'CONFIRMADA', 'EM_ATENDIMENTO')
                           THEN o.id_ocorrencia END) AS ocorrencias_ativas
  FROM cr_cidadao c
  LEFT JOIN cr_via_monitorada vm ON vm.id_cidadao = c.id_usuario
  LEFT JOIN cr_ocorrencia_via ov ON ov.id_via = vm.id_via
  LEFT JOIN cr_ocorrencia o ON o.id_ocorrencia = ov.id_ocorrencia
 GROUP BY c.modal
 ORDER BY ocorrencias_ativas DESC;
