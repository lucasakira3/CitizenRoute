# Citizen Life

**Hub de consulta para o dia a dia na cidade.** A página inicial reúne seis pilares da vida urbana em cards: mobilidade, lazer, alimentação, segurança, educação e habitação. Cada card leva a uma primeira consulta que ajuda o cidadão no dia a dia. Nesta versão, o pilar de **mobilidade** está pronto: o cidadão informa a saída e o destino, o sistema calcula a rota e mostra os riscos do caminho para o meio de transporte dele, e um mapa de riscos mostra a cidade inteira. Os outros cinco pilares mostram o que a primeira consulta de cada um vai trazer.

> Global Solution 2026/2 · tema **CitizenTech** · Engenharia de Software FIAP (2ESOA)
>
> **Vídeo pitch:** _cole aqui o link público (YouTube, Drive ou Vimeo)_

---

## O problema

A informação que o cidadão usa para viver a cidade está espalhada: o trânsito num aplicativo, os alagamentos no noticiário, a feira e o posto de saúde no site da prefeitura, cada um com uma linguagem. E ela costuma ser igual para todo mundo, quando não deveria: o mesmo alagamento é lentidão para quem está de carro e perigo de queda para quem está de moto, a pé ou de bicicleta.

## A solução

| Pilar | Primeira consulta | Situação |
|---|---|---|
| **Mobilidade** | Rota da saída ao destino com o risco de cada ocorrência no caminho, calculado para o modal, e mapa de riscos da cidade | **pronto** |
| Lazer | Programação gratuita por região | em breve |
| Alimentação | Comida acessível perto de você | em breve |
| Segurança | Onde pedir ajuda perto de você | em breve |
| Educação | Vagas e cursos gratuitos | em breve |
| Habitação | Programas de moradia para você | em breve |

No pilar de mobilidade, o cidadão:

- **planeja a rota**: digita a saída e o destino (ou clica no mapa), escolhe como vai (carro, moto, ônibus, aplicativo, a pé ou bicicleta) e recebe a rota **mais rápida** e a **mais segura**, que desvia das vias perigosas para aquele modal;
- **vê o mapa de riscos**: São Paulo em 3D com as ocorrências ativas (alagamento, acidente, queda de árvore, semáforo apagado, deslizamento, via bloqueada, insegurança) na cor do risco para o modal escolhido;
- **salva as rotas do dia a dia** (com conta) e recebe alertas quando aparece uma ocorrência perigosa nelas.

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

Exemplo dos dados de demonstração: um alagamento de gravidade 3 na Radial Leste vale **3,6 (baixo)** para o Bruno, que passa de carro, e **8,1 (alto)** para a Ana, que passa de moto. A matriz completa de pesos está em [`TipoOcorrencia.java`](backend/src/main/java/br/com/citizenlife/model/TipoOcorrencia.java).

### ODS da ONU atendidos

| ODS | Como o Citizen Life contribui |
|---|---|
| **11** Cidades e comunidades sustentáveis | Reúne num só lugar o acesso a serviços da cidade; na mobilidade, torna os deslocamentos mais seguros (meta 11.2, transporte seguro e acessível) |
| **3** Saúde e bem-estar | Ajuda a evitar quedas e atropelamentos de motociclistas, pedestres e ciclistas (meta 3.6, mortes e ferimentos no trânsito) |
| **13** Ação contra a mudança do clima | Alerta sobre alagamentos e deslizamentos no caminho de cada pessoa (meta 13.1, resiliência a desastres) |
| **10** Redução das desigualdades | O risco é calculado para quem está mais exposto (a pé, de bicicleta, de moto), não para a média |

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 25 puro, JDBC e `HttpServer` do próprio JDK (sem Spring, sem Hibernate/JPA) |
| Banco | Oracle (12c ou superior), driver oficial `ojdbc11` |
| Front-end | React 19 com Vite (sem bibliotecas de interface ou de rotas) |
| Build | Maven, com Maven Wrapper incluído (`mvnw`): não é preciso instalar o Maven |

A única dependência do backend é o driver JDBC do Oracle. O JSON da API é lido e escrito por uma classe própria ([`Json.java`](backend/src/main/java/br/com/citizenlife/api/Json.java)).

---

## Como executar

### 1. Pré-requisitos

- JDK 25 ou superior, com a variável de ambiente `JAVA_HOME` apontando para a pasta do JDK (veja abaixo)
- Node.js 20.19 ou superior (recomendado: 22 LTS)
- Acesso a um banco Oracle (por exemplo, o Oracle da FIAP)

O Maven Wrapper (`mvnw`) precisa do `JAVA_HOME`. Sem ele, o passo 4 falha com `The JAVA_HOME environment variable is not defined correctly`. Alguns instaladores (como o Eclipse Adoptium/Temurin) deixam essa opção desmarcada. Para conferir no PowerShell:

```powershell
java -version      # deve mostrar a versão 25 ou superior
echo $env:JAVA_HOME
```

Se o `JAVA_HOME` sair vazio, defina com o caminho onde o JDK foi instalado e **feche e abra o terminal (ou o VS Code/IntelliJ)**:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\caminho\do\jdk-25", "User")
```

### 2. Criar as tabelas e os dados

1. Abra o **SQL Developer** e conecte no seu usuário Oracle.
2. Abra o arquivo [`sql/script.sql`](sql/script.sql).
3. Execute com **Executar Script (F5)**.

O script cria as 8 tabelas, insere os dados de demonstração e, no fim, mostra a contagem de linhas de cada tabela. Ele pode ser executado de novo a qualquer momento: o bloco inicial apaga só as tabelas do projeto (prefixo `CL_`).

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

O terminal mostra `Citizen Life API no ar em http://127.0.0.1:8080/api` e, logo abaixo, se o banco está conectado. Se algo estiver errado (senha, URL, tabelas ausentes), a mensagem diz o que corrigir.

No IntelliJ, também dá para rodar a classe `CitizenLifeApplication` direto, com a pasta `backend` como diretório de trabalho.

### 5. Subir o front-end (porta 5173)

Em outro terminal:

```powershell
cd web
npm install
npm run dev
```

Abra **http://localhost:5173**. O Vite repassa as chamadas `/api` para a API Java.

### 6. Entrar com uma conta de demonstração

Planejar rota e ver o mapa de riscos não exigem conta. Para salvar rotas e ver os alertas, crie uma conta na página **Entrar** (só nome, e-mail e senha) ou use uma das contas que o script já cria. A senha de todas é **`Citizen@2026`**.

| Conta | Modal | Para que serve na demonstração |
|---|---|---|
| ana.souza@exemplo.com | moto | Recebe alerta **alto** do alagamento na Radial Leste, que fica na rota "Casa para o trabalho" |
| bruno.lima@exemplo.com | carro | A rota dele passa pela mesma Radial Leste, mas o risco para ele é **baixo** |
| elisa.ferreira@exemplo.com | a pé | Semáforo apagado na Consolação, no caminho a pé até o metrô |
| felipe.rocha@exemplo.com | bicicleta | Rota de bicicleta pela Rebouças e pela Faria Lima |

---

## Telas

O menu é o mesmo em todas as páginas: os seis pilares (Mobilidade, Educação, Alimentação, Segurança, Habitação e Lazer), cada um com a sua cor. A logo leva ao início, e quem está logado abre o perfil pelo próprio nome, ao lado de Sair.

| Página | Quem usa | O que tem |
|---|---|---|
| **Início** | todos | Os seis pilares em cards, cada um com a sua primeira consulta. Mobilidade está disponível; os outros, em breve |
| **Mobilidade** | todos | As duas consultas do pilar (Planejar rota e Mapa de riscos) e a regra do risco explicada |
| **Planejar rota** | todos (salvar: com conta) | Saída e destino com busca de ruas, avenidas e bairros (ou clique no mapa) e o modal. O sistema calcula a rota **mais rápida** e a **mais segura**, mostra as duas com tempo, distância e ocorrências no caminho, pinta na cor do risco os trechos com ocorrência e deixa salvar a rota. Quem tem endereços salvos (casa, trabalho...) usa eles como atalho de saída e destino |
| **Mapa de riscos** | todos | Mapa de São Paulo em tela cheia com dados reais do OpenStreetMap (avenidas, ruas, rios, represas, parques, bairros e prédios altos em 3D). Cada ocorrência ativa vira uma placa de advertência 3D na via certa, na cor do risco para o modal escolhido. Câmera livre (arrastar, zoom até o nível da rua, girar, inclinar, vista 2D), lista lateral e filtro por zona |
| **Perfil** | com conta | No estilo das contas de loja on-line: barra lateral com "Minha conta" e uma linha por pilar, e o conteúdo ao centro; dentro de cada pilar, as funções ficam em abas. **Resumo**: cartão do cidadão e três números (alertas, rotas e endereços). **Meus dados**: CRUD da conta (nome, e-mail, senha, excluir). **Mobilidade**: *Minhas rotas* (alertas e CRUD das rotas), *Como me locomovo* (um clique escolhe e já salva o modal) e *Meus endereços* (CRUD de casa, trabalho, estudo e outros). Os outros pilares mostram, como "em breve", o que vão pedir no perfil |
| **Lazer, Alimentação, Segurança, Educação, Habitação** | todos | O que a primeira consulta do pilar vai trazer |
| **Entrar** | todos | Login e cadastro simples (nome, e-mail e senha). O meio de transporte é opcional e fica para o perfil; sem ele, os alertas calculam o risco para quem anda a pé, o caso mais exposto |

As telas validam os campos antes de enviar e mostram os erros ao lado de cada campo. O Java valida de novo e devolve os erros no mesmo formato. A interface funciona em celular e desktop.

As animações (troca de página, cards que entram em sequência, losangos de risco que viram) usam só CSS e APIs nativas do navegador, sem bibliotecas, e são desligadas para quem ativa "reduzir movimento" no sistema. O mapa também não usa biblioteca: o chão é desenhado com a API Canvas 2D do navegador e as placas 3D usam transformações CSS (`perspective`, `rotateX`, `rotateZ`, `scale3d`). O cálculo de rotas é um algoritmo A\* escrito em JavaScript puro.

### Paleta de cores

Base neutra e quente, para o site ficar leve, com cores alegres só onde ajudam a reconhecer cada coisa.

| Cor | Uso |
|---|---|
| `#FAF7F0` e `#FFFFFF` | fundo creme da página e cards brancos |
| `#1F2A24` e `#5B665F` | texto (grafite esverdeado no lugar do preto) e texto de apoio |
| `#287D4D` | cor principal: botões, links, o card e a cor do pilar Mobilidade |
| `#F4C34F` | o "sol": cabeça do bonequinho, selos e avatar |
| `#2F93C8`, `#F4B740`, `#2A9D8F`, `#8B7BE8`, `#E06C4F` | pilares Lazer, Alimentação, Segurança, Educação e Habitação; o azul também é a linha da rota no mapa |

O texto e os botões passam no contraste AA (4,5:1). As cores de risco (verde, amarelo, laranja e vermelho) continuam as da sinalização de trânsito, porque carregam significado.

**Marca:** o cidadão de braços abertos, com a cabeça amarela (o sol), dentro de um quadrado verde ([`Logo.jsx`](web/src/components/Logo.jsx) e `web/public/favicon.svg`). Na página inicial, ele aparece no centro de uma órbita com os seis pilares em volta, que é a ideia do hub. Títulos em Plus Jakarta Sans, texto em Public Sans; a Overpass ficou só nas placas de rua e nos números de risco, onde lembra a sinalização.

### Mapa da cidade (dados do OpenStreetMap)

O banco guarda só o nome e a região de cada via. O desenho da cidade vem do arquivo `web/public/mapa/sao-paulo.json`, gerado a partir do [OpenStreetMap](https://www.openstreetmap.org/copyright) (© colaboradores do OpenStreetMap, licença ODbL; o crédito aparece no canto do mapa). O site **não acessa nenhum serviço externo**: ele só lê esse arquivo, que já está no projeto. Cada via do banco é encontrada no mapa pelo nome.

### Como a rota é calculada

1. **Rede de ruas.** O arquivo `web/public/mapa/sao-paulo-rotas.json`, gerado junto com o mapa, guarda a rede das vias principais de São Paulo (avenidas, vias expressas e ruas coletoras): cada cruzamento é um nó e cada trecho entre dois cruzamentos é uma aresta, com comprimento, classe da via, nome e sentido (mão única).
2. **Saída e destino.** A busca usa os nomes de ruas e bairros do próprio mapa, sem serviço de endereços. O ponto escolhido é ligado ao cruzamento de via principal mais próximo.
3. **Caminho.** O algoritmo A\* acha o caminho de menor tempo. Carro, moto, app e ônibus respeitam a mão única; pedestre e bicicleta não usam vias expressas.
4. **Rota mais segura.** O mesmo cálculo, mas as vias com ocorrência de risco moderado ou maior para o modal ficam muito mais "caras" (6×, 25× ou 80×, conforme o risco), e o caminho desvia delas sempre que existe alternativa.
5. **Risco.** O front-end descobre quais vias cadastradas no banco a rota percorre e pergunta ao Java (`POST /api/rotas/avaliar`). A regra de risco fica toda no backend.

A busca não conhece números de casa: o cidadão escolhe a rua, a avenida ou o bairro, ou marca o ponto exato no mapa.

Para gerar o arquivo de novo (opcional, precisa de internet e leva alguns minutos):

```bash
cd web
node scripts/gerar-mapa.mjs
```

---

## Arquitetura

```
CitizenRoute/                     (pasta do repositório)
├── backend/                      API Java
│   ├── pom.xml                   Java 25 + ojdbc11
│   ├── mvnw, mvnw.cmd            Maven Wrapper
│   ├── db.properties.example     modelo de configuração do banco
│   └── src/main/java/br/com/citizenlife/
│       ├── CitizenLifeApplication.java   monta as camadas e sobe o HttpServer
│       ├── model/        entidades, enums e a interface AvaliavelRisco
│       ├── dao/          JDBC: SQL parametrizado e transações
│       ├── service/      regras de negócio, validação e permissões
│       ├── api/          interface HTTP: roteador, JSON, conversores
│       │   └── controller/   uma classe por recurso da API
│       ├── exception/    exceções da aplicação
│       └── config/       conexão com o Oracle
├── web/                          React + Vite
│   └── src/
│       ├── pages/        uma página por rota
│       ├── components/   placa de via, sinal de risco, campos, mapa, layout...
│       ├── services/     comunicação HTTP com a API Java
│       ├── hooks/        rota, carregamento de dados e formulários
│       ├── utils/        pilares do hub, cálculo de rotas, validação, formatação
│       └── styles/       tokens, componentes e páginas (responsivo)
├── sql/script.sql                CREATE TABLE + INSERTs
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

- **Interfaces:** `AvaliavelRisco` (implementada por `Ocorrencia`: tudo que tem risco calculável para um modal) e `CrudDao<T>` (o contrato inserir, listar, buscar, atualizar e excluir, implementado por `CidadaoDao`, `RotaDao` e `EnderecoDao`).
- **Herança:** as exceções da aplicação herdam de `CitizenLifeException`, e o `Roteador` trata cada subclasse com o código HTTP certo.
- **Polimorfismo:** o `RiscoService` calcula o risco de qualquer `AvaliavelRisco`, sem saber que é uma ocorrência.
- **Encapsulamento:** atributos privados com getters e setters; regras no service, nunca no controller.
- **Relacionamentos:** `Cidadao` 1:N `Rota`; `Rota` N:N `Via` (com a ordem); `Ocorrencia` N:N `Via` (com o nível de impacto); `Via` N:1 `Regiao`.

### Tratamento de erros

Toda `SQLException` vira `PersistenciaException` com mensagem clara (o `TradutorErroSql` reconhece o código ORA e o nome da constraint). O `Roteador` é o único lugar que transforma exceções em respostas, então nenhum erro derruba o servidor:

| Exceção | HTTP | Exemplo de mensagem |
|---|---|---|
| `ValidacaoException` | 400 | "Nome da rota precisa ter pelo menos 2 caracteres." (com o campo) |
| `AcessoNegadoException` | 401 ou 403 | "Você só pode ver as rotas da sua própria conta." |
| `RecursoNaoEncontradoException` | 404 | "Rota 99 não encontrada." |
| `RegraNegocioException` | 409 | "Você já tem 10 rotas salvas. Exclua uma antes de criar outra." |
| `PersistenciaException` | 409 ou 503 | "Você já tem uma rota com este nome." |

---

## Banco de dados

8 tabelas com prefixo `CL_` (para não colidir com outras tabelas do seu usuário Oracle):

| Tabela | Papel |
|---|---|
| `CL_REGIAO` | zonas da cidade; nome **UNIQUE** |
| `CL_VIA` | avenidas e ruas (N:1 região) |
| `CL_CIDADAO` | conta do cidadão: e-mail **UNIQUE**, senha guardada como hash PBKDF2, **modal** opcional (CHECK) |
| `CL_OCORRENCIA` | evento que afeta vias; **gravidade CHECK 1–5**, **status CHECK**; autor com `ON DELETE SET NULL` |
| `CL_OCORRENCIA_VIA` | **associativa N:N** ocorrência × via, com o nível de impacto |
| `CL_ROTA` | rota salva pelo cidadão: saída, destino, critério (**CHECK** rápida ou segura), distância; nome **UNIQUE** por cidadão |
| `CL_ROTA_VIA` | **associativa N:N** rota × via, com a ordem da via no caminho |
| `CL_ENDERECO` | endereços salvos pelo cidadão (casa, trabalho, estudo, outro): **CHECK** no tipo, apelido **UNIQUE** por cidadão; viram atalhos de saída e destino |

Todas as constraints têm nome explícito (`PK_`, `FK_`, `UK_`, `CK_`). Os IDs usam `GENERATED ALWAYS AS IDENTITY`. As chaves estrangeiras têm índices, e os INSERTs buscam as FKs por campos únicos, sem depender de IDs fixos.

As ocorrências entram pela carga do script (na versão completa, viriam dos relatos dos cidadãos e dos órgãos da cidade). As consultas mais elaboradas do sistema são feitas com JOIN: a lista de ocorrências traz as vias e as regiões de cada uma, e os alertas de um cidadão cruzam `CL_OCORRENCIA_VIA` com `CL_ROTA_VIA` e `CL_ROTA` para achar as ocorrências ativas nas vias das rotas dele.

---

## API

Todas as respostas são JSON. Operações que exigem login recebem o id do cidadão no cabeçalho `X-Usuario-Id`, que o front-end envia depois do login.

| Método | Rota | Descrição | Quem pode |
|---|---|---|---|
| GET | `/api/health` | Situação da API e do banco | todos |
| GET | `/api/dominio` | Opções dos enums, matriz de risco e limites | todos |
| POST | `/api/auth/login` | Login com e-mail e senha | todos |
| GET | `/api/auth/sessao` | Confere a sessão guardada no navegador | logado |
| POST | `/api/cidadaos` | Cadastro público: `{nome, email, senha}` (`modal` é opcional) | todos |
| GET, PUT, DELETE | `/api/cidadaos/{id}` | Buscar, atualizar e excluir a conta | o próprio cidadão |
| GET, POST | `/api/cidadaos/{id}/rotas` | Listar e salvar rotas | o próprio cidadão |
| GET, PUT, DELETE | `/api/cidadaos/{id}/rotas/{idRota}` | Rota com os alertas atuais; editar; excluir | o próprio cidadão |
| GET | `/api/cidadaos/{id}/alertas` | Ocorrências ativas nas vias das rotas salvas, com risco para o modal | o próprio cidadão |
| GET, POST | `/api/cidadaos/{id}/enderecos` | Listar e salvar endereços: `{apelido, tipo, local: {descricao, x, y}}` | o próprio cidadão |
| PUT, DELETE | `/api/cidadaos/{id}/enderecos/{idEndereco}` | Editar e excluir um endereço | o próprio cidadão |
| POST | `/api/rotas/avaliar` | Risco de um caminho recém-calculado (`{modal, vias}`), antes de salvar | todos |
| GET | `/api/vias` e `/api/vias/{id}` | Vias cadastradas, com a região | todos |
| GET | `/api/ocorrencias?ativas=true&modal=MOTO` | Ocorrências com o risco para o modal (mapa de riscos) | todos |
| GET | `/api/ocorrencias/{id}` | Detalhe com o risco para cada modal | todos |

---

## Diagramas

- [Diagrama de classes (PDF, 4 páginas)](docs/diagrama-classes.pdf), com uma imagem por camada em `docs/diagrama-classes-*.png`
- [Diagrama entidade-relacionamento (PDF)](docs/diagrama-er.pdf) e [PNG](docs/diagrama-er.png)

O diagrama de classes foi gerado a partir das classes compiladas, então os atributos, métodos e relacionamentos são exatamente os do código. Os getters e setters simples foram omitidos para manter a leitura possível. As fontes em Mermaid estão em `docs/diagramas/` e podem ser editadas em [mermaid.live](https://mermaid.live).

---

## Visão de futuro

- **Os outros cinco pilares**, cada um com a sua primeira consulta: programação gratuita (lazer), comida acessível (alimentação), onde pedir ajuda (segurança), vagas e cursos gratuitos (educação) e programas de moradia (habitação).
- **Mobilidade:** relatos de ocorrência feitos pelos próprios cidadãos e confirmados pelos órgãos da cidade, cruzados com sensores de nível de água, câmeras, APIs de trânsito e clima e um modelo de previsão de risco por via e horário.

---

## Validação com pessoas reais

_Preencher com as conversas feitas pelo grupo: quem foi ouvido (perfil e modal), o que disse e o que mudou no projeto por causa disso._

| Perfil ouvido | O que relatou | O que mudou no Citizen Life |
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
| Claude Code (Anthropic) | Reescrita do backend Java (camadas, JDBC, regras de risco e tratamento de erros), do script Oracle, do front-end React, mudança de CitizenRoute para Citizen Life (hub com seis pilares), geração dos diagramas a partir do código, testes da API e desta documentação. |

O código produzido com apoio de IA foi revisado e testado pelo grupo, que é responsável por todo o conteúdo entregue. O guia [`docs/GUIA-DO-CODIGO.md`](docs/GUIA-DO-CODIGO.md) explica cada parte do sistema.
