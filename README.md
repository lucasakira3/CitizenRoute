# CitizenRoute

**Hub de segurança da mobilidade urbana.** Quem circula pela cidade reporta ocorrências nas vias e recebe alertas com o risco calculado para o seu meio de transporte. A gestão pública confirma, registra ações e acompanha cada caso até resolver.

> Global Solution 2026/2 · tema **CitizenTech** · Engenharia de Software FIAP (2ESOA)
>
> **Vídeo pitch:** _cole aqui o link público (YouTube, Drive ou Vimeo)_

---

## O problema

O mesmo alagamento não é igual para todo mundo. Para quem está de carro, água na pista é lentidão. Para quem está de moto, a pé ou de bicicleta, é risco de queda, de arrastamento ou de atropelamento. Os canais de informação de trânsito tratam todos do mesmo jeito, e o cidadão que vê o problema primeiro não tem como avisar quem vem atrás nem a gestão pública.

## A solução

| Lado | O que faz no CitizenRoute |
|---|---|
| **Cidadão se protege** | Reporta ocorrências (acidente, alagamento, queda de árvore, semáforo apagado, deslizamento, via bloqueada, insegurança), salva as vias do seu trajeto e recebe alertas com o risco calculado para o seu modal. |
| **Gestão pública age** | CET, Defesa Civil, SPTrans, Subprefeitura e GCM veem a fila do seu órgão, confirmam ou descartam relatos, registram andamentos e marcam como resolvido. Tudo fica no histórico. |
| **Quem planeja decide** | Relatórios com SQL mostram as vias mais críticas, a distribuição por região e tipo, o tempo médio de resposta de cada órgão e quais modais estão mais expostos. |

### A regra central: o risco depende do modal

```
risco = gravidade (1 a 5) × peso do tipo (1,0 a 2,0) × exposição do modal (0,5 a 1,8)
```

| Faixa | Risco | O cidadão recebe alerta? |
|---|---|---|
| Baixo | abaixo de 4 | não |
| Moderado | de 4 a 6,9 | sim |
| Alto | de 7 a 9,9 | sim |
| Crítico | 10 ou mais | sim |

Exemplo dos dados de demonstração: um alagamento de gravidade 3 na Radial Leste vale **3,6 (baixo)** para o Bruno, que passa de carro, e **8,1 (alto)** para a Ana, que passa de moto. A matriz completa de pesos está em [`TipoOcorrencia.java`](backend/src/main/java/br/com/citizenroute/model/TipoOcorrencia.java).

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 25 puro, JDBC e `HttpServer` do próprio JDK (sem Spring, sem Hibernate/JPA) |
| Banco | Oracle (12c ou superior), driver oficial `ojdbc11` |
| Front-end | React 19 com Vite (sem bibliotecas de interface ou de rotas) |
| Build | Maven, com Maven Wrapper incluído (`mvnw`): não é preciso instalar o Maven |

A única dependência do backend é o driver JDBC do Oracle. O JSON da API é lido e escrito por uma classe própria ([`Json.java`](backend/src/main/java/br/com/citizenroute/api/Json.java)).

---

## Como executar

### 1. Pré-requisitos

- JDK 25 ou superior
- Node.js 20.19 ou superior (recomendado: 22 LTS)
- Acesso a um banco Oracle (por exemplo, o Oracle da FIAP)

### 2. Criar as tabelas e os dados

1. Abra o **SQL Developer** e conecte no seu usuário Oracle.
2. Abra o arquivo [`sql/script.sql`](sql/script.sql).
3. Execute com **Executar Script (F5)**.

O script cria as 9 tabelas, insere os dados de demonstração e, no fim, mostra a contagem de linhas e os relatórios. Ele pode ser executado de novo a qualquer momento: o bloco inicial apaga só as tabelas do projeto (prefixo `CR_`).

### 3. Configurar a conexão

Copie `backend/db.properties.example` para `backend/db.properties` e preencha:

```properties
ORACLE_JDBC_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
ORACLE_DB_USER=rm000000
ORACLE_DB_PASSWORD=sua_senha
```

O arquivo `db.properties` está no `.gitignore` e nunca vai para o repositório. Também é possível usar variáveis de ambiente com os mesmos nomes; elas têm prioridade sobre o arquivo.

### 4. Subir a API Java (porta 8080)

```powershell
cd backend
.\mvnw.cmd compile exec:java
```

No Linux ou no macOS, use `./mvnw compile exec:java`. Na primeira execução, o Maven Wrapper baixa o Maven sozinho.

O terminal mostra `CitizenRoute API no ar em http://127.0.0.1:8080/api` e, logo abaixo, se o banco está conectado. Se algo estiver errado (senha, URL, tabelas ausentes), a mensagem diz o que corrigir.

No IntelliJ, também dá para rodar a classe `CitizenRouteApplication` direto, com a pasta `backend` como diretório de trabalho.

### 5. Subir o front-end (porta 5173)

Em outro terminal:

```powershell
cd web
npm install
npm run dev
```

Abra **http://localhost:5173**. O Vite repassa as chamadas `/api` para a API Java.

### 6. Entrar com uma conta de demonstração

A senha de todas as contas é **`Citizen@2026`**. A página **Entrar** tem atalhos para preencher cada uma.

| Conta | Perfil | Para que serve na demonstração |
|---|---|---|
| ana.souza@exemplo.com | Cidadão · moto | Recebe alerta **alto** do alagamento na Radial Leste |
| bruno.lima@exemplo.com | Cidadão · carro | Monitora a mesma Radial Leste, mas o risco para ele é **baixo** |
| elisa.ferreira@exemplo.com | Cidadão · pedestre | Alerta do semáforo apagado na Consolação |
| gabriela.prado@exemplo.com | Gestor · CET | Fila de acidentes, semáforos e vias bloqueadas |
| henrique.alves@exemplo.com | Gestor · Defesa Civil | Fila de alagamentos e deslizamentos |

---

## Telas

O menu é o mesmo em todas as páginas.

| Página | Quem usa | O que tem |
|---|---|---|
| **Painel** | todos | Seletor de modal que recalcula o risco de todas as ocorrências ativas, prévia do mapa 3D (clique numa zona para filtrar a lista) e os números da cidade |
| **Mapa** | todos | Mapa de São Paulo em tela cheia com dados reais do OpenStreetMap: todas as avenidas e ruas com nome, rios, represas, parques, bairros e prédios altos em 3D. Cada ocorrência ativa vira uma placa de advertência 3D na via certa, na cor do risco para o modal escolhido, e a via atingida acende na mesma cor. Câmera livre (arrastar, zoom até o nível da rua, girar, inclinar, vista 2D), lista lateral, filtro por zona e, para o cidadão logado, o trajeto dele em azul |
| **Ocorrências** | todos | Lista com filtros por status, tipo e região. Cidadãos reportam, editam e excluem os próprios relatos |
| **Detalhe da ocorrência** | todos | Relato, vias atingidas, risco para cada modal e linha do tempo das ações |
| **Meu trajeto** | cidadão | Alertas nas vias salvas, CRUD das vias monitoradas e edição do perfil |
| **Gestão** | gestor | Fila do órgão: confirmar, iniciar atendimento, resolver ou descartar, sempre com descrição da ação |
| **Relatórios** | todos | Quatro relatórios com SQL (JOIN e agregação) |
| **Cadastros** | gestor edita, todos leem | CRUD de vias, regiões, cidadãos e gestores |
| **Entrar** | todos | Login e cadastro de cidadão |

As telas validam os campos antes de enviar e mostram os erros ao lado de cada campo. O Java valida de novo e devolve os erros no mesmo formato. A interface funciona em celular e desktop.

As animações (troca de página, reordenação da lista, números que contam, losangos que viram) usam só CSS e APIs nativas do navegador, sem bibliotecas, e são desligadas para quem ativa "reduzir movimento" no sistema. O mapa também não usa biblioteca: o chão é desenhado com a API Canvas 2D do navegador e as placas 3D usam transformações CSS (`perspective`, `rotateX`, `rotateZ`, `scale3d`).

### Mapa da cidade (dados do OpenStreetMap)

O banco guarda só o nome e a região de cada via. O desenho da cidade vem do arquivo `web/public/mapa/sao-paulo.json`, gerado a partir do [OpenStreetMap](https://www.openstreetmap.org/copyright) (© colaboradores do OpenStreetMap, licença ODbL; o crédito aparece no canto do mapa). O site **não acessa nenhum serviço externo**: ele só lê esse arquivo, que já está no projeto. Cada via do banco é encontrada no mapa pelo nome; por isso uma via nova cadastrada pela tela, se existir em São Paulo, já aparece no lugar certo.

Para gerar o arquivo de novo (opcional, precisa de internet e leva alguns minutos):

```bash
cd web
node scripts/gerar-mapa.mjs
```

---

## Arquitetura

```
CitizenRoute/
├── backend/                      API Java
│   ├── pom.xml                   Java 25 + ojdbc11
│   ├── mvnw, mvnw.cmd            Maven Wrapper
│   ├── db.properties.example     modelo de configuração do banco
│   └── src/main/java/br/com/citizenroute/
│       ├── CitizenRouteApplication.java   monta as camadas e sobe o HttpServer
│       ├── model/        entidades, enums e a interface AvaliavelRisco
│       ├── dao/          JDBC: um DAO por tabela, SQL parametrizado, transações
│       ├── service/      regras de negócio, validação e permissões
│       ├── api/          interface HTTP: roteador, JSON, conversores
│       │   └── controller/   uma classe por recurso da API
│       ├── exception/    exceções da aplicação
│       └── config/       conexão com o Oracle
├── web/                          React + Vite
│   └── src/
│       ├── pages/        uma página por rota
│       ├── components/   placa de via, sinal de risco, campos, layout...
│       ├── services/     comunicação HTTP com a API Java
│       ├── hooks/        rota, carregamento de dados e formulários
│       ├── utils/        validação, formatação e rótulos
│       └── styles/       tokens, componentes e páginas (responsivo)
├── sql/script.sql                CREATE TABLE + INSERTs + relatórios
├── docs/
│   ├── diagrama-classes.pdf      UML (4 páginas, uma por camada) + PNGs
│   ├── diagrama-er.pdf           modelo entidade-relacionamento + PNG
│   ├── diagramas/*.mmd           fontes Mermaid dos diagramas
│   └── GUIA-DO-CODIGO.md         explicação do código para estudo
└── integrantes.txt
```

### Caminho de uma requisição

```
React (fetch /api/...)
  → Roteador (api)           encontra a rota e converte exceções em respostas HTTP
  → Controller (api)         lê o JSON e chama a service
  → Service                  valida, confere permissões e aplica as regras
  → DAO                      executa o SQL com PreparedStatement
  → Oracle
```

### Programação orientada a objetos

- **Herança:** `Usuario` é abstrata; `Cidadao` e `Gestor` herdam dela. No banco, `CR_USUARIO` com `CR_CIDADAO` e `CR_GESTOR` (1:1).
- **Polimorfismo:** `getTipo()` e `getDescricaoPerfil()` são abstratos em `Usuario`. O `UsuarioDao` devolve a subclasse certa a partir do banco.
- **Interfaces:** `AvaliavelRisco` (implementada por `Ocorrencia`) e `CrudDao<T>` (implementada pelos DAOs das entidades principais).
- **Encapsulamento:** atributos privados com getters e setters; regras no service, nunca no controller.

### Tratamento de erros

Toda `SQLException` vira `PersistenciaException` com mensagem clara (o `TradutorErroSql` reconhece o código ORA e o nome da constraint). O `Roteador` é o único lugar que transforma exceções em respostas, então nenhum erro derruba o servidor:

| Exceção | HTTP | Exemplo de mensagem |
|---|---|---|
| `ValidacaoException` | 400 | "Título precisa ter pelo menos 5 caracteres." (com o campo) |
| `AcessoNegadoException` | 401 ou 403 | "Apenas gestores públicos podem alterar o status de ocorrências." |
| `RecursoNaoEncontradoException` | 404 | "Ocorrência 99 não encontrada." |
| `RegraNegocioException` | 409 | "Não é possível passar de "Resolvida" para "Aberta"." |
| `PersistenciaException` | 409 ou 503 | "Não é possível excluir a via: existem ocorrências registradas nela." |

---

## Banco de dados

9 tabelas com prefixo `CR_` (para não colidir com outras tabelas do seu usuário Oracle):

| Tabela | Papel |
|---|---|
| `CR_REGIAO` | zonas da cidade |
| `CR_VIA` | avenidas e ruas (N:1 região) |
| `CR_USUARIO` | dados comuns, e-mail **UNIQUE**, senha guardada como hash PBKDF2 |
| `CR_CIDADAO` | especialização com o **modal** (CHECK) |
| `CR_GESTOR` | especialização com **órgão** (CHECK) e matrícula (UNIQUE) |
| `CR_OCORRENCIA` | relato; **gravidade CHECK 1–5**, **status CHECK** |
| `CR_OCORRENCIA_VIA` | **associativa N:N** ocorrência × via, com o nível de impacto |
| `CR_VIA_MONITORADA` | **associativa N:N** cidadão × via, com o apelido do trajeto |
| `CR_ACAO` | histórico de ações dos gestores |

Todas as constraints têm nome explícito (`PK_`, `FK_`, `UK_`, `CK_`). Os IDs usam `GENERATED ALWAYS AS IDENTITY`. As chaves estrangeiras têm índices, e os INSERTs buscam as FKs por campos únicos, sem depender de IDs fixos.

### Relatórios (SQL com JOIN e agregação)

| Relatório | Endpoint | SQL |
|---|---|---|
| Vias mais críticas | `GET /api/relatorios/vias-criticas` | 4 tabelas, `COUNT`, `SUM(CASE)`, `AVG`, `MAX`, `FETCH FIRST` |
| Ocorrências por região e tipo | `GET /api/relatorios/ocorrencias-por-regiao` | 4 tabelas, `COUNT(DISTINCT)` |
| Tempo médio de resposta por órgão | `GET /api/relatorios/tempo-resposta` | subconsulta com `MIN` por órgão e `AVG` da diferença de datas |
| Exposição por modal | `GET /api/relatorios/exposicao-modal` | `LEFT JOIN` em 4 tabelas, mais o risco médio calculado no Java |

As mesmas consultas estão no fim de `sql/script.sql` para rodar direto no SQL Developer.

---

## API

Todas as respostas são JSON. Operações que exigem login recebem o id do usuário no cabeçalho `X-Usuario-Id`, que o front-end envia depois do login.

| Método | Rota | Descrição | Quem pode |
|---|---|---|---|
| GET | `/api/health` | Situação da API e do banco | todos |
| GET | `/api/dominio` | Opções dos enums, matriz de risco e limites | todos |
| POST | `/api/auth/login` | Login com e-mail e senha | todos |
| GET | `/api/auth/sessao` | Confere a sessão guardada no navegador | logado |
| GET, POST | `/api/regioes` | Listar e cadastrar regiões | listar: todos; cadastrar: gestor |
| GET, PUT, DELETE | `/api/regioes/{id}` | Buscar, atualizar e excluir | gestor para alterar |
| GET, POST | `/api/vias?regiao=&busca=` | Listar com filtros e cadastrar | gestor para cadastrar |
| GET, PUT, DELETE | `/api/vias/{id}` | Buscar, atualizar e excluir | gestor para alterar |
| GET, POST | `/api/cidadaos` | Listar e cadastrar (cadastro é público) | todos |
| GET, PUT, DELETE | `/api/cidadaos/{id}` | Buscar, atualizar e excluir | o próprio ou gestor |
| GET, POST | `/api/cidadaos/{id}/vias-monitoradas` | Trajetos salvos | o próprio cidadão |
| PUT, DELETE | `/api/cidadaos/{id}/vias-monitoradas/{idVia}` | Renomear e remover | o próprio cidadão |
| GET | `/api/cidadaos/{id}/alertas` | Ocorrências ativas nas vias, com risco para o modal | o próprio ou gestor |
| GET, POST | `/api/gestores` | Listar e cadastrar | gestor para cadastrar |
| GET, PUT, DELETE | `/api/gestores/{id}` | Buscar, atualizar e excluir | gestor |
| GET, POST | `/api/ocorrencias?status=&tipo=&orgao=&regiao=&via=&autor=&ativas=&modal=` | Listar com filtros e registrar | registrar: cidadão |
| GET, PUT, DELETE | `/api/ocorrencias/{id}` | Detalhe com risco por modal e histórico; editar; excluir | editar: autor, enquanto aberta |
| PATCH | `/api/ocorrencias/{id}/status` | Mudar o status e registrar a ação | gestor |
| GET, POST | `/api/ocorrencias/{id}/acoes` | Histórico e novo andamento | gestor para registrar |
| PUT, DELETE | `/api/acoes/{id}` | Corrigir ou remover um andamento | gestor autor |
| GET | `/api/relatorios/resumo` e os quatro relatórios acima | Números do painel e relatórios | todos |

### Fluxo de status

| Status atual | Pode ir para |
|---|---|
| ABERTA | CONFIRMADA ou DESCARTADA |
| CONFIRMADA | EM_ATENDIMENTO, RESOLVIDA ou DESCARTADA |
| EM_ATENDIMENTO | RESOLVIDA |
| RESOLVIDA ou DESCARTADA | nenhum: a ocorrência está encerrada |

Não existe volta. Cada mudança grava uma linha em `CR_ACAO` na mesma transação, e só gestores mudam o status.

---

## Diagramas

- [Diagrama de classes (PDF, 4 páginas)](docs/diagrama-classes.pdf), com uma imagem por camada em `docs/diagrama-classes-*.png`
- [Diagrama entidade-relacionamento (PDF)](docs/diagrama-er.pdf) e [PNG](docs/diagrama-er.png)

O diagrama de classes foi gerado a partir das classes compiladas, então os atributos, métodos e relacionamentos são exatamente os do código. Os getters e setters simples foram omitidos para manter a leitura possível. As fontes em Mermaid estão em `docs/diagramas/` e podem ser editadas em [mermaid.live](https://mermaid.live).

---

## Visão de futuro

O MVP usa detecção colaborativa: o cidadão reporta e o gestor confirma. Os próximos passos seriam cruzar os relatos com sensores de nível de água, câmeras com visão computacional, APIs de trânsito e clima, e um modelo de previsão de risco por via e horário.

---

## Validação com pessoas reais

_Preencher com as conversas feitas pelo grupo: quem foi ouvido (perfil e modal), o que disse e o que mudou no projeto por causa disso._

| Perfil ouvido | O que relatou | O que mudou no CitizenRoute |
|---|---|---|
| | | |

---

## Integrantes

Veja [`integrantes.txt`](integrantes.txt).

---

## Uso de IA generativa

| Ferramenta | Para que foi usada |
|---|---|
| GitHub Copilot | Estrutura inicial do projeto, depois substituída integralmente. |
| Claude Code (Anthropic) | Reescrita do backend Java (camadas, JDBC, regras de risco e tratamento de erros), do script Oracle, do front-end React, geração dos diagramas a partir do código, testes da API e desta documentação. |

O código produzido com apoio de IA foi revisado e testado pelo grupo, que é responsável por todo o conteúdo entregue. O guia [`docs/GUIA-DO-CODIGO.md`](docs/GUIA-DO-CODIGO.md) explica cada parte do sistema.
