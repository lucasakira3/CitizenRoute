-- =====================================================================
-- Citizen Life · Global Solution 2026/2 · CitizenTech (FIAP)
-- Script Oracle: as 6 tabelas do projeto e os dados de demonstração.
--
-- Como executar (SQL Developer): abra este arquivo e use "Executar Script" (F5).
-- Pode ser executado de novo a qualquer momento: o bloco inicial apaga só as
-- tabelas do projeto (prefixo CL_) antes de recriá-las.
-- Senha de todos os cidadãos de demonstração: Citizen@2026
-- =====================================================================


-- 1. Limpeza ----------------------------------------------------------
BEGIN
    FOR tabela IN (SELECT table_name FROM user_tables
                    WHERE table_name IN ('CL_CIDADAO_FEIRA', 'CL_FEIRA', 'CL_ENDERECO', 'CL_ROTA_VIA', 'CL_ROTA',
                                         'CL_OCORRENCIA', 'CL_CIDADAO', 'CL_VIA', 'CL_OCORRENCIA_VIA', 'CL_REGIAO'))
    LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || tabela.table_name || ' CASCADE CONSTRAINTS PURGE';
    END LOOP;
END;
/


-- 2. Tabelas ----------------------------------------------------------

-- Quem usa o hub. O modal (meio de transporte) é opcional e muda o risco das ocorrências.
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

-- Avenidas e ruas acompanhadas, com a zona da cidade.
CREATE TABLE cl_via (
    id_via  NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    nome    VARCHAR2(120) NOT NULL,
    zona    VARCHAR2(20)  NOT NULL,
    CONSTRAINT pk_cl_via PRIMARY KEY (id_via),
    CONSTRAINT uk_cl_via_nome UNIQUE (nome),
    CONSTRAINT ck_cl_via_zona CHECK (zona IN ('Centro', 'Zona Norte', 'Zona Sul', 'Zona Leste', 'Zona Oeste'))
);

-- Problema numa via (1:N via → ocorrências): alagamento, acidente, queda de árvore...
CREATE TABLE cl_ocorrencia (
    id_ocorrencia  NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    id_via         NUMBER(10)    NOT NULL,
    titulo         VARCHAR2(100) NOT NULL,
    descricao      VARCHAR2(500) NOT NULL,
    tipo           VARCHAR2(20)  NOT NULL,
    gravidade      NUMBER(1)     NOT NULL,
    dt_registro    DATE          DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_cl_ocorrencia PRIMARY KEY (id_ocorrencia),
    CONSTRAINT fk_cl_ocorrencia_via FOREIGN KEY (id_via) REFERENCES cl_via (id_via),
    CONSTRAINT uk_cl_ocorrencia_titulo UNIQUE (titulo),
    CONSTRAINT ck_cl_ocorrencia_tipo CHECK (tipo IN ('ACIDENTE', 'ALAGAMENTO', 'QUEDA_ARVORE', 'SEMAFORO_APAGADO',
                                                     'DESLIZAMENTO', 'VIA_BLOQUEADA', 'INSEGURANCA')),
    CONSTRAINT ck_cl_ocorrencia_gravidade CHECK (gravidade BETWEEN 1 AND 5)
);

-- Rota salva pelo cidadão (1:N cidadão → rotas). X e Y são a posição no mapa.
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
    distancia_m        NUMBER(7)     NOT NULL,
    dt_criacao         DATE          DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_cl_rota PRIMARY KEY (id_rota),
    CONSTRAINT fk_cl_rota_cidadao FOREIGN KEY (id_cidadao) REFERENCES cl_cidadao (id_cidadao) ON DELETE CASCADE,
    CONSTRAINT uk_cl_rota_nome UNIQUE (id_cidadao, nome),
    CONSTRAINT ck_cl_rota_distancia CHECK (distancia_m > 0)
);

-- Associativa N:N entre rota e via: as vias por onde a rota passa, na ordem.
CREATE TABLE cl_rota_via (
    id_rota  NUMBER(10) NOT NULL,
    id_via   NUMBER(10) NOT NULL,
    ordem    NUMBER(3)  NOT NULL,
    CONSTRAINT pk_cl_rota_via PRIMARY KEY (id_rota, id_via),
    CONSTRAINT fk_cl_rotavia_rota FOREIGN KEY (id_rota) REFERENCES cl_rota (id_rota) ON DELETE CASCADE,
    CONSTRAINT fk_cl_rotavia_via FOREIGN KEY (id_via) REFERENCES cl_via (id_via),
    CONSTRAINT ck_cl_rotavia_ordem CHECK (ordem >= 1)
);

-- Endereços salvos pelo cidadão (1:N cidadão → endereços), atalhos de saída e destino.
CREATE TABLE cl_endereco (
    id_endereco  NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    id_cidadao   NUMBER(10)    NOT NULL,
    apelido      VARCHAR2(40)  NOT NULL,
    tipo         VARCHAR2(10)  NOT NULL,
    descricao    VARCHAR2(120) NOT NULL,
    pos_x        NUMBER(8,2)   NOT NULL,
    pos_y        NUMBER(8,2)   NOT NULL,
    CONSTRAINT pk_cl_endereco PRIMARY KEY (id_endereco),
    CONSTRAINT fk_cl_endereco_cidadao FOREIGN KEY (id_cidadao) REFERENCES cl_cidadao (id_cidadao) ON DELETE CASCADE,
    CONSTRAINT uk_cl_endereco_apelido UNIQUE (id_cidadao, apelido),
    CONSTRAINT ck_cl_endereco_tipo CHECK (tipo IN ('CASA', 'TRABALHO', 'ESTUDO', 'OUTRO'))
);

-- Feiras livres do município, organizadas por dia da semana e pela zona da cidade.
CREATE TABLE cl_feira (
    id_feira      NUMBER(10)    GENERATED ALWAYS AS IDENTITY,
    nome          VARCHAR2(80)  NOT NULL,
    rua           VARCHAR2(120) NOT NULL,
    zona          VARCHAR2(20)  NOT NULL,
    dia_semana    VARCHAR2(12)  NOT NULL,
    horario       VARCHAR2(40)  NOT NULL,
    CONSTRAINT pk_cl_feira PRIMARY KEY (id_feira),
    CONSTRAINT uk_cl_feira_rua_dia UNIQUE (rua, dia_semana),
    CONSTRAINT ck_cl_feira_zona CHECK (zona IN ('Centro', 'Zona Norte', 'Zona Sul', 'Zona Leste', 'Zona Oeste')),
    CONSTRAINT ck_cl_feira_dia CHECK (dia_semana IN ('SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO', 'DOMINGO'))
);

-- Feiras favoritas do cidadão. Uma feira pode ser favorita por mais de um cidadão.
CREATE TABLE cl_cidadao_feira (
    id_cidadao  NUMBER(10) NOT NULL,
    id_feira    NUMBER(10) NOT NULL,
    CONSTRAINT pk_cl_cidadao_feira PRIMARY KEY (id_cidadao, id_feira),
    CONSTRAINT fk_cl_cidadao_feira_cidadao FOREIGN KEY (id_cidadao) REFERENCES cl_cidadao (id_cidadao) ON DELETE CASCADE,
    CONSTRAINT fk_cl_cidadao_feira_feira FOREIGN KEY (id_feira) REFERENCES cl_feira (id_feira) ON DELETE CASCADE
);


-- 3. Dados de demonstração --------------------------------------------
-- As chaves estrangeiras são buscadas por campos únicos (nome, e-mail, título),
-- então o script não depende dos ids gerados. SYSDATE - 3/24 = três horas atrás.

-- 3.1 Cidadãos: um para cada meio de transporte
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

-- 3.2 Vias
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Paulista', 'Centro');
INSERT INTO cl_via (nome, zona) VALUES ('Rua da Consolação', 'Centro');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida 23 de Maio', 'Centro');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Prestes Maia', 'Centro');
INSERT INTO cl_via (nome, zona) VALUES ('Marginal Tietê', 'Zona Norte');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Cruzeiro do Sul', 'Zona Norte');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Inajar de Souza', 'Zona Norte');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Engenheiro Caetano Álvares', 'Zona Norte');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Santo Amaro', 'Zona Sul');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Washington Luís', 'Zona Sul');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Interlagos', 'Zona Sul');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Ricardo Jafet', 'Zona Sul');
INSERT INTO cl_via (nome, zona) VALUES ('Radial Leste', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Aricanduva', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Sapopemba', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Salim Farah Maluf', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Marginal Pinheiros', 'Zona Oeste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Rebouças', 'Zona Oeste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Brigadeiro Faria Lima', 'Zona Oeste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Francisco Morato', 'Zona Oeste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Ipiranga', 'Centro');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida São João', 'Centro');
INSERT INTO cl_via (nome, zona) VALUES ('Rua Augusta', 'Centro');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Braz Leme', 'Zona Norte');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Água Fria', 'Zona Norte');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Imirim', 'Zona Norte');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida General Ataliba Leonel', 'Zona Norte');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Nossa Senhora do Ó', 'Zona Norte');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Jabaquara', 'Zona Sul');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Cupecê', 'Zona Sul');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Senador Teotônio Vilela', 'Zona Sul');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Vereador José Diniz', 'Zona Sul');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Nossa Senhora do Sabará', 'Zona Sul');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Itaquera', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Ragueb Chohfi', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Marechal Tito', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Mateo Bei', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Jacu-Pêssego', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Amador Bueno da Veiga', 'Zona Leste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Pompeia', 'Zona Oeste');
INSERT INTO cl_via (nome, zona) VALUES ('Rua Teodoro Sampaio', 'Zona Oeste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Corifeu de Azevedo Marques', 'Zona Oeste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Escola Politécnica', 'Zona Oeste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Pedroso de Morais', 'Zona Oeste');
INSERT INTO cl_via (nome, zona) VALUES ('Avenida Indianópolis', 'Zona Sul');

-- 3.3 Ocorrências espalhadas pelas cinco zonas (o mapa de riscos)
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Alagamento na Radial Leste, altura do Tatuapé',
       'Água acima do meio-fio nas duas faixas da direita. Carros passam devagar; motos estão derrapando.',
       'ALAGAMENTO', 3, SYSDATE - 3/24
  FROM cl_via WHERE nome = 'Radial Leste';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Semáforo apagado na Consolação com a Paulista',
       'Semáforo do cruzamento apagado nos dois sentidos. Pedestres atravessando no meio dos carros.',
       'SEMAFORO_APAGADO', 3, SYSDATE - 2/24
  FROM cl_via WHERE nome = 'Rua da Consolação';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Queda de árvore bloqueia faixa na Av. Rebouças',
       'Árvore de grande porte caiu sobre a faixa da direita e a ciclovia, perto da Rua Oscar Freire.',
       'QUEDA_ARVORE', 4, SYSDATE - 5/24
  FROM cl_via WHERE nome = 'Avenida Rebouças';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Colisão entre carro e bicicleta na Faria Lima',
       'Carro atingiu ciclista na faixa da direita. Resgate a caminho e trânsito parado no sentido Pinheiros.',
       'ACIDENTE', 4, SYSDATE - 1/24
  FROM cl_via WHERE nome = 'Avenida Brigadeiro Faria Lima';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Alagamento na pista local da Marginal Pinheiros',
       'Pista local completamente alagada perto da Ponte Cidade Jardim. Carros voltando de ré.',
       'ALAGAMENTO', 4, SYSDATE - 40/1440
  FROM cl_via WHERE nome = 'Marginal Pinheiros';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Ponto de ônibus sem iluminação na Av. Sapopemba',
       'Ponto perto do número 9000 está sem luz há três noites. Passageiros esperam no escuro depois das 22h.',
       'INSEGURANCA', 3, SYSDATE - 20/24
  FROM cl_via WHERE nome = 'Avenida Sapopemba';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Risco de deslizamento na Av. Engenheiro Caetano Álvares',
       'Trincas na encosta ao lado da calçada depois da chuva forte. Terra descendo para a pista.',
       'DESLIZAMENTO', 4, SYSDATE - 6/24
  FROM cl_via WHERE nome = 'Avenida Engenheiro Caetano Álvares';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Semáforo apagado na Ipiranga com a São João',
       'Cruzamento famoso sem semáforo nos dois sentidos. Muita gente atravessando entre os carros.',
       'SEMAFORO_APAGADO', 3, SYSDATE - 70/1440
  FROM cl_via WHERE nome = 'Avenida Ipiranga';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Feira de rua ocupa faixa da Avenida São João',
       'Barracas montadas na faixa da direita perto do Largo do Paissandu. Ônibus desviando.',
       'VIA_BLOQUEADA', 2, SYSDATE - 150/1440
  FROM cl_via WHERE nome = 'Avenida São João';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Trecho escuro e relatos de furto na Rua Augusta',
       'Postes apagados entre a Rua Peixoto Gomide e a Alameda Santos. Relatos de furto de celular à noite.',
       'INSEGURANCA', 3, SYSDATE - 5/24
  FROM cl_via WHERE nome = 'Rua Augusta';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Alagamento na Avenida Braz Leme perto do Campo de Marte',
       'Água cobrindo as duas faixas da direita depois da chuva. Motos passando pelo canteiro.',
       'ALAGAMENTO', 3, SYSDATE - 95/1440
  FROM cl_via WHERE nome = 'Avenida Braz Leme';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Galho grande pendurado sobre a Avenida Água Fria',
       'Galho rachado de uma tipuana balançando sobre a faixa da direita, perto da Rua Pedro Doll.',
       'QUEDA_ARVORE', 3, SYSDATE - 45/1440
  FROM cl_via WHERE nome = 'Avenida Água Fria';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Batida entre dois carros na Avenida Imirim',
       'Colisão traseira sem feridos. Carros parados na faixa da esquerda aguardando o guincho.',
       'ACIDENTE', 2, SYSDATE - 35/1440
  FROM cl_via WHERE nome = 'Avenida Imirim';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Semáforo piscando na General Ataliba Leonel',
       'Semáforo em amarelo piscante no cruzamento com a Rua Voluntários da Pátria.',
       'SEMAFORO_APAGADO', 2, SYSDATE - 2/24
  FROM cl_via WHERE nome = 'Avenida General Ataliba Leonel';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Terra da encosta na Avenida Nossa Senhora do Ó',
       'Barranco desceu sobre a calçada e parte da faixa. Pedestres andando pela pista.',
       'DESLIZAMENTO', 3, SYSDATE - 4/24
  FROM cl_via WHERE nome = 'Avenida Nossa Senhora do Ó';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Alagamento na Avenida Jabaquara perto da estação Conceição',
       'Água acima do meio-fio nos dois sentidos. Carros baixos ficando presos.',
       'ALAGAMENTO', 4, SYSDATE - 80/1440
  FROM cl_via WHERE nome = 'Avenida Jabaquara';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Trecho sem iluminação na Avenida Cupecê',
       'Cerca de 500 metros sem luz perto do Terminal Cidade Ademar. Ponto de ônibus no escuro.',
       'INSEGURANCA', 4, SYSDATE - 10/24
  FROM cl_via WHERE nome = 'Avenida Cupecê';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Caminhão tombado na Avenida Senador Teotônio Vilela',
       'Caminhão de entregas tombou na curva perto da Represa Billings. Uma faixa liberada.',
       'ACIDENTE', 4, SYSDATE - 55/1440
  FROM cl_via WHERE nome = 'Avenida Senador Teotônio Vilela';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Árvore caída no canteiro da Avenida Vereador José Diniz',
       'Árvore de médio porte caiu sobre o canteiro central e encosta na faixa da esquerda.',
       'QUEDA_ARVORE', 2, SYSDATE - 3/24
  FROM cl_via WHERE nome = 'Avenida Vereador José Diniz';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Obra fecha faixa da Avenida Nossa Senhora do Sabará',
       'Obra de saneamento com uma faixa fechada nos dois sentidos, sinalizada com cones.',
       'VIA_BLOQUEADA', 3, SYSDATE - 12/24
  FROM cl_via WHERE nome = 'Avenida Nossa Senhora do Sabará';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Semáforo apagado na Avenida Indianópolis',
       'Semáforo do cruzamento com a Avenida dos Bandeirantes apagado. Agente de trânsito ainda não chegou.',
       'SEMAFORO_APAGADO', 3, SYSDATE - 25/1440
  FROM cl_via WHERE nome = 'Avenida Indianópolis';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Córrego transborda na Avenida Itaquera',
       'Córrego encheu e a água tomou a pista perto da Rua Victorio Santim. Motos derrapando.',
       'ALAGAMENTO', 3, SYSDATE - 65/1440
  FROM cl_via WHERE nome = 'Avenida Itaquera';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Moto derrapa em areia na Avenida Ragueb Chohfi',
       'Areia de obra espalhada na pista. Um motociclista caiu, sem ferimentos graves.',
       'ACIDENTE', 3, SYSDATE - 40/1440
  FROM cl_via WHERE nome = 'Avenida Ragueb Chohfi';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Ponto de ônibus sem luz na Avenida Marechal Tito',
       'Ponto vandalizado e sem iluminação perto da estação São Miguel Paulista.',
       'INSEGURANCA', 3, SYSDATE - 15/24
  FROM cl_via WHERE nome = 'Avenida Marechal Tito';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Árvore derruba fios na Avenida Mateo Bei',
       'Árvore caiu sobre a rede elétrica e fechou a avenida. Fios no chão.',
       'QUEDA_ARVORE', 4, SYSDATE - 130/1440
  FROM cl_via WHERE nome = 'Avenida Mateo Bei';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Carreta quebrada na Avenida Jacu-Pêssego',
       'Carreta parada na faixa da direita no sentido Mauá. Trânsito lento por 2 km.',
       'VIA_BLOQUEADA', 4, SYSDATE - 85/1440
  FROM cl_via WHERE nome = 'Avenida Jacu-Pêssego';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Semáforo piscando na Avenida Amador Bueno da Veiga',
       'Semáforo em amarelo piscante perto do Shopping Penha.',
       'SEMAFORO_APAGADO', 2, SYSDATE - 50/1440
  FROM cl_via WHERE nome = 'Avenida Amador Bueno da Veiga';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Água acumulada na descida da Avenida Pompeia',
       'Poça grande na faixa da direita na descida para a Marginal. Carros jogando água na calçada.',
       'ALAGAMENTO', 2, SYSDATE - 30/1440
  FROM cl_via WHERE nome = 'Avenida Pompeia';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Semáforo apagado na Rua Teodoro Sampaio',
       'Semáforo do cruzamento com a Rua Henrique Schaumann apagado no horário de saída das escolas.',
       'SEMAFORO_APAGADO', 3, SYSDATE - 100/1440
  FROM cl_via WHERE nome = 'Rua Teodoro Sampaio';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Encosta cede ao lado da Avenida Corifeu de Azevedo Marques',
       'Muro de contenção cedeu e a terra fechou as faixas no sentido bairro.',
       'DESLIZAMENTO', 4, SYSDATE - 160/1440
  FROM cl_via WHERE nome = 'Avenida Corifeu de Azevedo Marques';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Colisão leve na Avenida Escola Politécnica',
       'Carro e moto se tocaram no semáforo. Sem feridos, veículos no acostamento.',
       'ACIDENTE', 2, SYSDATE - 20/1440
  FROM cl_via WHERE nome = 'Avenida Escola Politécnica';
INSERT INTO cl_ocorrencia (id_via, titulo, descricao, tipo, gravidade, dt_registro)
SELECT id_via, 'Galhos na ciclovia da Avenida Pedroso de Morais',
       'Galhos caídos bloqueiam a ciclovia perto da Praça Pan-Americana. Ciclistas indo para a pista.',
       'QUEDA_ARVORE', 3, SYSDATE - 75/1440
  FROM cl_via WHERE nome = 'Avenida Pedroso de Morais';

-- 3.4 Rotas salvas e as vias por onde cada uma passa (N:N)
INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                     destino_descricao, destino_x, destino_y, distancia_m, dt_criacao)
SELECT id_cidadao, 'Casa para o trabalho', 'Tatuapé', 1278.42, 1065.76, 'Praça da Sé', 1044.35, 1109.19, 7787, SYSDATE - 30
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                     destino_descricao, destino_x, destino_y, distancia_m, dt_criacao)
SELECT id_cidadao, 'Entregas na Paulista', 'Tatuapé', 1278.42, 1065.76, 'Avenida Paulista', 954.63, 1158.38, 11403, SYSDATE - 20
  FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                     destino_descricao, destino_x, destino_y, distancia_m, dt_criacao)
SELECT id_cidadao, 'Caminho do trabalho', 'Penha', 1415.84, 1008.15, 'República', 1011.32, 1081.27, 15648, SYSDATE - 28
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                     destino_descricao, destino_x, destino_y, distancia_m, dt_criacao)
SELECT id_cidadao, 'Visita aos meus pais', 'Penha', 1415.84, 1008.15, 'Santana', 1079.42, 897.37, 15883, SYSDATE - 28
  FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                     destino_descricao, destino_x, destino_y, distancia_m, dt_criacao)
SELECT id_cidadao, 'Ônibus para a faculdade', 'Sapopemba', 1537.36, 1356.02, 'Avenida Paulista', 954.63, 1158.38, 17803, SYSDATE - 25
  FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';
INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                     destino_descricao, destino_x, destino_y, distancia_m, dt_criacao)
SELECT id_cidadao, 'Corridas da manhã', 'Pinheiros', 803.34, 1183.19, 'Brooklin', 795.19, 1387.04, 8537, SYSDATE - 22
  FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';
INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                     destino_descricao, destino_x, destino_y, distancia_m, dt_criacao)
SELECT id_cidadao, 'A pé até o metrô', 'Rua da Consolação', 986.85, 1094.57, 'Estação Consolação', 935.47, 1141.54, 2071, SYSDATE - 18
  FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_rota (id_cidadao, nome, origem_descricao, origem_x, origem_y,
                     destino_descricao, destino_x, destino_y, distancia_m, dt_criacao)
SELECT id_cidadao, 'Ciclovia até o trabalho', 'Avenida Rebouças', 864.51, 1174.33, 'Avenida Brigadeiro Faria Lima', 846.98, 1270.94, 3371, SYSDATE - 15
  FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'ana.souza@exemplo.com' AND r.nome = 'Casa para o trabalho' AND v.nome = 'Radial Leste';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'ana.souza@exemplo.com' AND r.nome = 'Entregas na Paulista' AND v.nome = 'Radial Leste';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 2 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'ana.souza@exemplo.com' AND r.nome = 'Entregas na Paulista' AND v.nome = 'Avenida 23 de Maio';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 3 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'ana.souza@exemplo.com' AND r.nome = 'Entregas na Paulista' AND v.nome = 'Avenida Paulista';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'bruno.lima@exemplo.com' AND r.nome = 'Caminho do trabalho' AND v.nome = 'Radial Leste';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'bruno.lima@exemplo.com' AND r.nome = 'Visita aos meus pais' AND v.nome = 'Radial Leste';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 2 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'bruno.lima@exemplo.com' AND r.nome = 'Visita aos meus pais' AND v.nome = 'Marginal Tietê';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 3 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'bruno.lima@exemplo.com' AND r.nome = 'Visita aos meus pais' AND v.nome = 'Avenida Cruzeiro do Sul';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'carla.mendes@exemplo.com' AND r.nome = 'Ônibus para a faculdade' AND v.nome = 'Avenida Paulista';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'diego.santos@exemplo.com' AND r.nome = 'Corridas da manhã' AND v.nome = 'Avenida Brigadeiro Faria Lima';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 2 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'diego.santos@exemplo.com' AND r.nome = 'Corridas da manhã' AND v.nome = 'Marginal Pinheiros';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'elisa.ferreira@exemplo.com' AND r.nome = 'A pé até o metrô' AND v.nome = 'Rua da Consolação';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 2 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'elisa.ferreira@exemplo.com' AND r.nome = 'A pé até o metrô' AND v.nome = 'Avenida Paulista';
INSERT INTO cl_rota_via (id_rota, id_via, ordem)
SELECT r.id_rota, v.id_via, 1 FROM cl_rota r JOIN cl_cidadao c ON c.id_cidadao = r.id_cidadao, cl_via v
 WHERE c.email = 'felipe.rocha@exemplo.com' AND r.nome = 'Ciclovia até o trabalho' AND v.nome = 'Avenida Brigadeiro Faria Lima';

-- 3.5 Endereços salvos (casa e trabalho viram atalhos na tela de rota)
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Casa', 'CASA', 'Tatuapé', 1278.42, 1065.76 FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Trabalho', 'TRABALHO', 'Praça da Sé', 1044.35, 1109.19 FROM cl_cidadao WHERE email = 'ana.souza@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Casa', 'CASA', 'Penha', 1415.84, 1008.15 FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Trabalho', 'TRABALHO', 'República', 1011.32, 1081.27 FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Casa dos meus pais', 'OUTRO', 'Santana', 1079.42, 897.37 FROM cl_cidadao WHERE email = 'bruno.lima@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Casa', 'CASA', 'Sapopemba', 1537.36, 1356.02 FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Faculdade', 'ESTUDO', 'Avenida Paulista', 954.63, 1158.38 FROM cl_cidadao WHERE email = 'carla.mendes@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Casa', 'CASA', 'Pinheiros', 803.34, 1183.19 FROM cl_cidadao WHERE email = 'diego.santos@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Casa', 'CASA', 'Rua da Consolação', 986.85, 1094.57 FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Trabalho', 'TRABALHO', 'Estação Consolação', 935.47, 1141.54 FROM cl_cidadao WHERE email = 'elisa.ferreira@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Casa', 'CASA', 'Avenida Rebouças', 864.51, 1174.33 FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';
INSERT INTO cl_endereco (id_cidadao, apelido, tipo, descricao, pos_x, pos_y)
SELECT id_cidadao, 'Trabalho', 'TRABALHO', 'Avenida Brigadeiro Faria Lima', 846.98, 1270.94 FROM cl_cidadao WHERE email = 'felipe.rocha@exemplo.com';

-- 3.6 Feiras livres e favoritas
INSERT INTO cl_feira (nome, rua, zona, dia_semana, horario) VALUES ('Mercado da Rua da Consolação', 'Rua da Consolação', 'Centro', 'SEGUNDA', '08:00 às 13:00');
INSERT INTO cl_feira (nome, rua, zona, dia_semana, horario) VALUES ('Feira da Avenida Paulista', 'Avenida Paulista', 'Centro', 'QUARTA', '08:00 às 17:00');
INSERT INTO cl_feira (nome, rua, zona, dia_semana, horario) VALUES ('Mercado da Vila Mariana', 'Avenida Santo Amaro', 'Zona Sul', 'SEXTA', '09:00 às 16:00');
INSERT INTO cl_feira (nome, rua, zona, dia_semana, horario) VALUES ('Feira da Vila Prudente', 'Rua Padre Adelino', 'Zona Leste', 'SABADO', '07:30 às 14:00');
INSERT INTO cl_feira (nome, rua, zona, dia_semana, horario) VALUES ('Feira de Mandaqui', 'Avenida General Ataliba Leonel', 'Zona Norte', 'DOMINGO', '08:00 às 15:00');
INSERT INTO cl_feira (nome, rua, zona, dia_semana, horario) VALUES ('Mercado da Lapa', 'Rua da Lapa', 'Centro', 'TERCA', '08:30 às 13:30');

INSERT INTO cl_cidadao_feira (id_cidadao, id_feira)
SELECT c.id_cidadao, f.id_feira
FROM cl_cidadao c
JOIN cl_feira f ON f.rua = 'Rua da Consolação' AND f.dia_semana = 'SEGUNDA'
WHERE c.email IN ('ana.souza@exemplo.com', 'elisa.ferreira@exemplo.com');

INSERT INTO cl_cidadao_feira (id_cidadao, id_feira)
SELECT c.id_cidadao, f.id_feira
FROM cl_cidadao c
JOIN cl_feira f ON f.rua = 'Avenida Paulista' AND f.dia_semana = 'QUARTA'
WHERE c.email IN ('bruno.lima@exemplo.com', 'carla.mendes@exemplo.com');

COMMIT;


-- 4. Conferência: linhas em cada tabela -------------------------------
SELECT 'CL_CIDADAO' AS tabela, COUNT(*) AS linhas FROM cl_cidadao
UNION ALL SELECT 'CL_VIA', COUNT(*) FROM cl_via
UNION ALL SELECT 'CL_OCORRENCIA', COUNT(*) FROM cl_ocorrencia
UNION ALL SELECT 'CL_ROTA', COUNT(*) FROM cl_rota
UNION ALL SELECT 'CL_ROTA_VIA', COUNT(*) FROM cl_rota_via
UNION ALL SELECT 'CL_ENDERECO', COUNT(*) FROM cl_endereco
UNION ALL SELECT 'CL_FEIRA', COUNT(*) FROM cl_feira
UNION ALL SELECT 'CL_CIDADAO_FEIRA', COUNT(*) FROM cl_cidadao_feira;
