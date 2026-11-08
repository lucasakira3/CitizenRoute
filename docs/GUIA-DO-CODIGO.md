# Guia do código do CitizenRoute

Este guia é para o grupo estudar o projeto antes da entrega e da apresentação. A regra da Global Solution é clara: código gerado com IA só vale se o grupo entende e consegue explicar. Leia com o código aberto ao lado.

---

## 1. O sistema em um minuto

- **Cidadão** reporta uma ocorrência numa ou mais vias e salva as vias do seu trajeto.
- **Gestor** de um órgão público confirma ou descarta o relato, registra o que está sendo feito e marca como resolvido.
- O sistema calcula o **risco** de cada ocorrência para cada **modal** (carro, moto, ônibus, app, a pé, bike). Quem monitora uma via recebe alerta quando o risco para o seu modal passa de 4.

Tudo é CRUD em Java com JDBC no Oracle. O React só mostra e envia dados: as regras ficam no Java.

---

## 2. O caminho de uma requisição

Exemplo: a Ana (moto) reporta um alagamento.

| Passo | Onde | O que acontece |
|---|---|---|
| 1 | `web/src/pages/OcorrenciaFormPage.jsx` | O formulário valida no navegador (título, tipo, gravidade, vias). Se estiver tudo certo, chama `api.criar("/ocorrencias", corpo)`. |
| 2 | `web/src/services/api.js` | Faz o `fetch` para `/api/ocorrencias`, com o cabeçalho `X-Usuario-Id` da Ana. |
| 3 | `web/vite.config.js` | Em desenvolvimento, o Vite repassa `/api` para `http://127.0.0.1:8080`. |
| 4 | `api/Roteador.java` | Descobre que `POST /api/ocorrencias` pertence ao `OcorrenciaController`. Se qualquer exceção acontecer daqui em diante, é aqui que ela vira uma resposta HTTP com mensagem. |
| 5 | `api/controller/OcorrenciaController.java` | Lê o JSON com `CorpoJson` e monta um objeto `Ocorrencia`. Não valida regra nenhuma. |
| 6 | `service/OcorrenciaService.java` | Confere se quem está pedindo é cidadão (`AutenticacaoService.exigirCidadao`), valida os campos com o `Validador`, confere se as vias existem, define status `ABERTA` e a data. |
| 7 | `dao/OcorrenciaDao.java` | Abre uma transação: `INSERT` em `CR_OCORRENCIA`, lê o id gerado e faz um `INSERT` em lote em `CR_OCORRENCIA_VIA`. Se algo falhar, `rollback`. |
| 8 | volta | O controller converte a ocorrência em mapa (`Conversor`), o `Roteador` escreve o JSON (`Json`) e responde **201**. O React vai para a página de detalhe. |

---

## 3. Backend, pacote por pacote

### `model`: o que existe no domínio

| Classe | Para que serve |
|---|---|
| `Usuario` (abstrata) | Dados comuns. Métodos abstratos `getTipo()` e `getDescricaoPerfil()`. |
| `Cidadao`, `Gestor` | Herdam de `Usuario`. O cidadão tem `modal`; o gestor tem `orgao` e `matricula`. |
| `Regiao`, `Via` | Zona da cidade e logradouro. A via guarda o objeto `Regiao` (associação). |
| `Ocorrencia` | Implementa `AvaliavelRisco`. O método `calcularRisco(Modal)` é a fórmula do projeto. |
| `OcorrenciaVia`, `ViaMonitorada` | As duas associações N:N, cada uma com seu atributo extra (nível de impacto e apelido). |
| `Acao` | Resposta do gestor. `statusNovo` é nulo quando é só um andamento. |
| `Alerta` | Não é tabela: é o resultado do cruzamento entre ocorrências ativas e vias monitoradas. |
| Enums | `Modal`, `Orgao`, `TipoOcorrencia`, `StatusOcorrencia`, `NivelImpacto`, `NivelRisco`, `TipoUsuario`. |
| `relatorio/*` | `record`s com uma linha de cada relatório. |

**A matriz de risco** fica em `TipoOcorrencia`: cada tipo tem um peso e um fator para cada modal. Está escrita como uma tabela no código para ser fácil de ler e defender na apresentação.

### `dao`: SQL e JDBC

- Um DAO por tabela principal. Os que têm CRUD completo implementam `CrudDao<T>` (`inserir`, `listar`, `buscarPorId`, `atualizar`, `excluir`).
- Todo acesso usa `try-with-resources` (a conexão, o `PreparedStatement` e o `ResultSet` fecham sozinhos) e `PreparedStatement` com `?` (sem concatenar valores do usuário no SQL, o que evita SQL injection).
- `JdbcUtil.emTransacao(...)` desliga o auto-commit, executa, faz `commit` ou `rollback`. É usado quando uma operação mexe em mais de uma tabela (cidadão = `CR_USUARIO` + `CR_CIDADAO`; ocorrência = `CR_OCORRENCIA` + `CR_OCORRENCIA_VIA`; mudança de status = `UPDATE` + `INSERT` em `CR_ACAO`).
- `TradutorErroSql` converte o erro do Oracle em mensagem clara. Exemplo: `ORA-02292` na constraint `FK_CR_OCVIA_VIA` vira "Não é possível excluir a via: existem ocorrências registradas nela."
- `RelatorioDao` tem as quatro consultas com JOIN e agregação.

### `service`: regras de negócio

| Classe | Regras principais |
|---|---|
| `Validador` | Junta todos os erros do formulário e lança uma `ValidacaoException` com o erro de cada campo. |
| `AutenticacaoService` | Login (confere o hash da senha) e os métodos `exigirCidadao`, `exigirGestor` e `exigirProprioOuGestor`. |
| `SenhaService` | Gera e confere hashes PBKDF2. A senha nunca é gravada. |
| `RiscoService` | Arredonda o risco, classifica em baixo, moderado, alto ou crítico e decide quando alertar (≥ 4). |
| `OcorrenciaService` | Transições de status, quem pode editar e excluir, validação das vias. |
| `AcaoService` | Andamentos: só em ocorrência ativa; só o gestor autor corrige; ação que mudou status não pode ser apagada. |
| `AlertaService` | Cruza as vias monitoradas com as ocorrências ativas e calcula o risco para o modal do cidadão. |
| `RelatorioService` | Repassa os relatórios e completa a exposição por modal com o risco médio. |
| `CidadaoService`, `GestorService`, `RegiaoService`, `ViaService`, `ViaMonitoradaService` | CRUD com validação, e-mail e matrícula únicos e permissões. |

### `api`: a camada de interface

- `Roteador` transforma `/api/vias/{id}` numa expressão regular, encontra a rota e chama o método certo. O `try/catch` central converte cada exceção no status HTTP adequado (tabela no README).
- `Requisicao` separa parâmetros da rota, da URL, o corpo JSON e o cabeçalho do usuário.
- `Json` é um leitor e escritor de JSON feito à mão, para não depender de biblioteca externa.
- `CorpoJson` lê campos com o tipo certo e acusa o campo errado ("O campo gravidade deve ser um número inteiro").
- `Conversor` transforma objetos do model em mapas para virar JSON. A senha nunca sai.
- `controller/*`: uma classe por recurso, cada uma com o método `registrar(Roteador)` que declara as suas rotas.

### `exception` e `config`

- Todas as exceções herdam de `CitizenRouteException`, que é `RuntimeException`. Assim as assinaturas ficam limpas e um único lugar, o `Roteador`, decide a resposta.
- `OracleConnectionFactory` lê as credenciais das variáveis de ambiente ou do `db.properties` e explica o que falta quando não estão configuradas.

### `CitizenRouteApplication`

Cria os objetos na ordem DAO, depois Service, depois Controller, passando cada um pelo construtor (injeção de dependência feita à mão). Depois sobe o `HttpServer` do JDK com 8 threads.

---

## 4. Banco de dados

- **Prefixo `CR_`**: o usuário Oracle da FIAP é usado em várias disciplinas. O prefixo evita apagar ou colidir com uma tabela `USUARIO` de outro trabalho.
- **Herança no banco** (tabela por subclasse): `CR_USUARIO` guarda o comum; `CR_CIDADAO` e `CR_GESTOR` usam o mesmo id como PK e FK.
- **`ON DELETE CASCADE`** só onde faz sentido apagar junto: as vias e ações de uma ocorrência excluída, os trajetos de um cidadão excluído. Não há cascade de cidadão para ocorrência: o histórico não pode sumir, então o Oracle bloqueia e o sistema explica.
- **`DATE`** guarda data e hora no Oracle. `DATE - DATE` dá dias; vezes 1440 dá minutos. É assim que o relatório calcula o tempo de resposta.
- **Dados de demonstração** com datas relativas (`SYSDATE - 3/24` é três horas atrás), para a demo parecer sempre atual.

---

## 5. Front-end

| Arquivo | Papel |
|---|---|
| `App.jsx` | Lista de rotas, sessão do usuário e carregamento do `/api/dominio` (rótulos e opções vindos do Java). |
| `hooks/useRota.js` | Navegação por hash (`#/ocorrencias`), sem biblioteca de rotas. |
| `hooks/useCarregar.js` | Busca dados e guarda `carregando`, `erro` e `dados`. |
| `hooks/useFormulario.js` | Valores, erros por campo, envio e exibição dos erros vindos do Java. |
| `utils/validacao.js` | As mesmas regras de tamanho, e-mail e senha do `Validador` Java. |
| `components/Layout.jsx` | Menu igual em todas as páginas e a faixa que avisa quando a API ou o banco caem. |
| `components/PlacaVia.jsx` | Mostra a via como uma placa de rua azul de São Paulo. |
| `components/SinalRisco.jsx` | Mostra o risco como uma placa de advertência (losango) colorida pela faixa. |
| `components/MapaCidade.jsx` | O mapa. Carrega os dados da cidade, desenha o chão num `<canvas>` e põe por cima as placas 3D das ocorrências (HTML + CSS 3D), cada uma na via atingida, na cor do risco. Destaca as vias atingidas e o trajeto do cidadão. Tem dois modos: `tela` (página Mapa, câmera livre) e `previa` (Painel, câmera parada). |
| `utils/mapaCidade.js` | Carrega `public/mapa/sao-paulo.json`, decodifica as linhas e monta o índice de ruas por nome. É por esse índice que uma via do banco ("Avenida Paulista") acha o seu traçado; a comparação ignora acentos, títulos ("Doutor", "Professor") e o que vem entre parênteses ("Marginal Tietê (Expressa)"). |
| `utils/desenhoMapa.js` | Desenha o mapa a cada quadro, como um app de mapa: chão, verde, água, trilhos, ruas (da menor para a maior), vias com ocorrência, prédios em 3D e os nomes, escolhidos para não se sobreporem. |
| `utils/projecao.js` | As contas da perspectiva (`projetar` e `desprojetar`), usadas pela câmera e pelo desenho. |
| `scripts/gerar-mapa.mjs` | Script que baixa os dados do OpenStreetMap (Overpass API), projeta as coordenadas, emenda os trechos de cada rua, simplifica as linhas e grava o arquivo do mapa. Só roda quando alguém quer atualizar o mapa; o site não depende dele. |
| `hooks/useCameraMapa.js` | A câmera do mapa: arrastar, roda e pinça para zoom, botão direito ou dois dedos para girar, teclado, voos animados com `requestAnimationFrame` e enquadramento automático. A cada quadro escreve a câmera em variáveis CSS (para as placas) e redesenha o canvas, sem re-renderizar o React. |
| `pages/MapaPage.jsx` | Página Mapa em tela cheia: painel com o seletor de modal, a lista de ocorrências (clicar voa até a placa) e o detalhe da ocorrência escolhida. No celular o painel vira uma gaveta. |
| `components/Icone.jsx` | Ícones próprios em SVG para os 7 tipos de ocorrência e os 6 modais. |
| `components/Esqueleto.jsx` | Formas cinzentas que aparecem enquanto os dados carregam. |
| `hooks/useContagem.js` | Faz os números do painel "contarem" até o valor, com `requestAnimationFrame`. |
| `hooks/useReordenacao.js` | Quando o modal muda e a lista reordena, cada item desliza da posição antiga para a nova (técnica FLIP, com a Web Animations API). |
| `styles/movimento.css` e `styles/mapa.css` | Animações de entrada, troca de página (View Transitions API), esqueletos e o visual do mapa (placas 3D com espessura, poste e sombra, painel lateral e controles). |

### Como o mapa funciona (sem biblioteca)

1. **Os dados** vêm do OpenStreetMap. O script `scripts/gerar-mapa.mjs` baixou uma vez o contorno da cidade, os 96 distritos (juntados nas 5 zonas da Prefeitura), as ruas, a água, o verde, os bairros e os prédios com altura informada. As coordenadas foram convertidas para um plano em que 1 unidade vale 25 m, e cada linha foi gravada como números inteiros relativos ao ponto anterior. O resultado, `public/mapa/sao-paulo.json`, tem cerca de 4,5 MB.
2. **A câmera** é a mesma para tudo: `perspective(1500px) rotateX(inclinação) scale3d(zoom) rotateZ(giro) translate(-x, -y)`. As placas recebem isso pelo CSS; o canvas faz a mesma conta em JavaScript (`utils/projecao.js`). Por isso as placas nunca escorregam das ruas.
3. **O chão é redesenhado a cada quadro** num `<canvas>`. Para caber em 16 ms, só entra o que está na tela (cada feição tem uma caixa), as ruas pequenas só aparecem de perto, de longe usa-se uma versão simplificada das avenidas e pontos a menos de 1 px do anterior são pulados.
4. **As ruas** são desenhadas em duas passadas, como nos apps de mapa: primeiro o contorno (mais largo e escuro), depois a pista. A largura cresce devagar com o zoom e diminui com a distância (perspectiva).
5. **Os nomes** são escolhidos a cada quadro por prioridade (zonas, rodovias, bairros, avenidas, ruas). Um nome só é escrito se não encostar em outro nem em uma placa, e o mesmo nome não se repete a menos de 320 px. Quem já estava na tela tem preferência, para não piscar.
6. **Os prédios** (altura real do OpenStreetMap) são desenhados de perto com o algoritmo do pintor: primeiro os mais distantes, cada parede com um tom conforme o lado em que bate o sol.
7. **As placas** ficam de pé "desfazendo" a câmera: `rotateZ(-giro) rotateX(-inclinação)`. É a técnica de *billboard*. A espessura vem de três camadas (frente colorida, miolo escuro e verso cinza) separadas no eixo Z.
8. **Arrastar e dar zoom no ponto certo** exige a conta inversa da perspectiva (`desprojetar`): descobrir qual ponto do chão está embaixo do cursor.

---

## 6. Perguntas que a banca pode fazer

**Por que não usaram Spring?**
A disciplina pede Java puro com JDBC. O `HttpServer` já vem no JDK e o roteamento é uma lista de expressões regulares.

**Como o risco é calculado?**
`gravidade × peso do tipo × fator do modal`, em `Ocorrencia.calcularRisco`. Os pesos estão em `TipoOcorrencia`. As faixas e o limite de alerta estão em `RiscoService`.

**Onde está a herança? E a interface?**
`Usuario` → `Cidadao` e `Gestor`. Interfaces: `AvaliavelRisco` e `CrudDao<T>`.

**Por que `Ocorrencia` não tem subclasses por tipo?**
Os tipos só mudam dados (peso, fatores, órgão), não comportamento. Um enum com esses dados é mais simples e mantém uma tabela só no banco.

**Como funciona o login? Por que não JWT?**
A senha é conferida contra o hash PBKDF2. Depois, o front envia o id no cabeçalho `X-Usuario-Id` e cada service confere o perfil. Num produto real, esse id seria trocado por um token assinado; isso ficou fora do MVP.

**O que acontece se o banco cair?**
A conexão lança `PersistenciaException` marcada como indisponível; a API responde 503 com a explicação, e o front mostra uma faixa amarela e preta no topo de todas as páginas.

**De onde vem o mapa? O site depende de algum serviço?**
Os dados são do OpenStreetMap (licença ODbL, com crédito no canto do mapa). Eles foram baixados uma vez pelo script `scripts/gerar-mapa.mjs` e ficam no projeto, em `public/mapa/sao-paulo.json`. O site não acessa nenhum serviço externo: funciona sem internet. O banco guarda só o nome e a região de cada via; o front encontra a via no mapa pelo nome. Guardar coordenadas no banco fica para a visão de futuro.

**Por que não usaram Leaflet, Mapbox ou Three.js?**
Seriam tecnologia fora do conteúdo, e a regra da entrega zera o requisito nesse caso. O chão é desenhado com a API Canvas 2D e as placas com transformações CSS, que são recursos nativos do navegador.

**Como garantem que o diagrama de classes bate com o código?**
Ele foi gerado a partir das classes compiladas (`javap`). As fontes Mermaid estão em `docs/diagramas`.

---

## 7. Roteiro sugerido para a demonstração do pitch

Antes de gravar, rode `sql/script.sql` de novo para os horários ficarem recentes.

1. **Painel com "Moto" selecionado:** mostrar quantas ocorrências ativas pedem atenção. Trocar para "Carro": as placas do mapa mudam de cor e se levantam de novo, a lista se reordena e o número cai. Clicar em "Abrir o mapa 3D".
2. **Mapa em tela cheia:** girar e inclinar a cidade, clicar numa placa (a câmera voa até ela e o painel mostra o detalhe) e clicar numa zona para filtrar.
3. **Entrar como Ana (moto):** em Meu trajeto, o alerta alto da Radial Leste. Abrir a ocorrência e mostrar o risco para cada modal.
4. **Entrar como Bruno (carro):** a mesma Radial Leste aparece como risco baixo.
5. **Como Ana, reportar uma ocorrência nova** com duas vias e mostrar a validação (enviar com erro primeiro).
6. **Entrar como Henrique (Defesa Civil):** a ocorrência nova está na fila. Confirmar com uma descrição. Abrir o detalhe e mostrar a linha do tempo.
7. **No SQL Developer:** `SELECT` em `CR_OCORRENCIA` e `CR_ACAO` mostrando que os dados novos estão no Oracle.
8. **Relatórios:** vias críticas e tempo de resposta por órgão.

Para o dado real da abertura do pitch, consulte fontes oficiais, como o Infosiga SP (acidentes de trânsito) e o CGE da Prefeitura de São Paulo (alagamentos), e cite a fonte no vídeo.

---

## 8. Antes de entregar

- [ ] Rodar `sql/script.sql` no Oracle da FIAP e conferir as contagens no fim.
- [ ] Subir a API com `.\mvnw.cmd compile exec:java` e ver "Banco Oracle conectado".
- [ ] Subir o front, entrar com as contas de demonstração e repetir o roteiro acima.
- [ ] Preencher `integrantes.txt` e o link do pitch no README e no `.txt`.
- [ ] Preencher a seção "Validação com pessoas reais" do README.
- [ ] Gerar o `.zip` sem `web/node_modules`, `web/dist` e `backend/target`.
