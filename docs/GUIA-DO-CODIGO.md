# Guia do código do CitizenRoute

Este guia é para o grupo estudar o projeto antes da entrega e da apresentação. A regra da Global Solution é clara: código gerado com IA só vale se o grupo entende e consegue explicar. Leia com o código aberto ao lado.

---

## 1. O sistema em um minuto

- **Cidadão** reporta uma ocorrência numa ou mais vias e planeja rotas: digita a saída e o destino, vê o risco de cada ocorrência no caminho e salva as rotas do dia a dia.
- **Gestor** de um órgão público confirma ou descarta o relato, registra o que está sendo feito e marca como resolvido.
- O sistema calcula o **risco** de cada ocorrência para cada **modal** (carro, moto, ônibus, app, a pé, bike). Quem tem uma rota salva recebe alerta quando surge, numa via da rota, uma ocorrência cujo risco para o seu modal passa de 4.

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
| `OcorrenciaVia` | Associação N:N entre ocorrência e via, com o nível de impacto. |
| `Rota`, `PontoRota`, `CriterioRota` | Rota salva pelo cidadão: saída e destino (`PontoRota`, um `record` com o nome e a posição no mapa), o critério (mais rápida ou mais segura) e a lista ordenada de vias que ela percorre (a associação N:N `CR_ROTA_VIA`). |
| `Acao` | Resposta do gestor. `statusNovo` é nulo quando é só um andamento. |
| `Alerta` | Não é tabela: é o resultado do cruzamento entre uma ocorrência ativa e um caminho (as rotas salvas ou uma rota recém-calculada). |
| Enums | `Modal` (também guarda a velocidade média e se é motorizado, usados no cálculo da rota), `Orgao`, `TipoOcorrencia`, `StatusOcorrencia`, `NivelImpacto`, `NivelRisco`, `TipoUsuario`, `CriterioRota`. |
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
| `AlertaService` | Cruza as ocorrências ativas com um caminho e calcula o risco para o modal: os alertas das rotas salvas e a avaliação de uma rota antes de salvar. |
| `RotaService` | CRUD das rotas: valida nome, saída, destino, distância e as vias do caminho; cada cidadão só altera as próprias rotas (máximo de 10). |
| `RelatorioService` | Repassa os relatórios e completa a exposição por modal com o risco médio. |
| `CidadaoService`, `GestorService`, `RegiaoService`, `ViaService` | CRUD com validação, e-mail e matrícula únicos e permissões. |

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
- **`ON DELETE CASCADE`** só onde faz sentido apagar junto: as vias e ações de uma ocorrência excluída, as rotas de um cidadão excluído e as vias de uma rota excluída. Não há cascade de cidadão para ocorrência: o histórico não pode sumir, então o Oracle bloqueia e o sistema explica.
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
| `components/MapaCidade.jsx` | O mapa. Carrega os dados da cidade, desenha o chão num `<canvas>` e põe por cima as placas 3D das ocorrências (HTML + CSS 3D), cada uma na via atingida, na cor do risco. Destaca as vias atingidas e, na página de rotas, desenha a rota calculada e os marcadores A e B. Tem dois modos: `tela` (câmera livre) e `previa` (Painel, câmera parada). |
| `utils/mapaCidade.js` | Carrega `public/mapa/sao-paulo.json`, decodifica as linhas e monta o índice de ruas por nome. É por esse índice que uma via do banco ("Avenida Paulista") acha o seu traçado; a comparação ignora acentos, títulos ("Doutor", "Professor") e o que vem entre parênteses ("Marginal Tietê (Expressa)"). |
| `utils/desenhoMapa.js` | Desenha o mapa a cada quadro, como um app de mapa: chão, verde, água, trilhos, ruas (da menor para a maior), vias com ocorrência, prédios em 3D e os nomes, escolhidos para não se sobreporem. |
| `utils/projecao.js` | As contas da perspectiva (`projetar` e `desprojetar`), usadas pela câmera e pelo desenho. |
| `scripts/gerar-mapa.mjs` | Script que baixa os dados do OpenStreetMap (Overpass API), projeta as coordenadas, emenda os trechos de cada rua, simplifica as linhas e grava o arquivo do mapa e a rede de ruas para as rotas. Só roda quando alguém quer atualizar o mapa; o site não depende dele. |
| `utils/rotas.js` | O cálculo de rotas: carrega a rede (`public/mapa/sao-paulo-rotas.json`), acha o cruzamento mais próximo de um ponto, roda o A\*, descobre quais vias do banco a rota percorre, monta as penalidades da rota mais segura e faz a busca de ruas e bairros. |
| `pages/PlanejarRotaPage.jsx` | Página Planejar rota: saída e destino (`components/CampoLugar.jsx`, com sugestões e marcação no mapa), modal, as opções mais rápida e mais segura lado a lado, as ocorrências no caminho e o formulário para salvar. |
| `pages/MinhasRotasPage.jsx` | Alertas das rotas salvas, lista das rotas (abrir no mapa, renomear, excluir) e os dados do cidadão. |
| `hooks/useCameraMapa.js` | A câmera do mapa: arrastar, roda e pinça para zoom, botão direito ou dois dedos para girar, teclado, voos animados com `requestAnimationFrame` e enquadramento automático. A cada quadro escreve a câmera em variáveis CSS (para as placas) e redesenha o canvas, sem re-renderizar o React. |
| `pages/MapaPage.jsx` | Página Mapa de riscos em tela cheia (visão da gestão pública): painel com o seletor de modal, a lista de ocorrências (clicar voa até a placa) e o detalhe da ocorrência escolhida. No celular o painel vira uma gaveta. |
| `components/Icone.jsx` | Ícones próprios em SVG para os 7 tipos de ocorrência e os 6 modais. |
| `components/Esqueleto.jsx` | Formas cinzentas que aparecem enquanto os dados carregam. |
| `hooks/useContagem.js` | Faz os números do painel "contarem" até o valor, com `requestAnimationFrame`. |
| `hooks/useReordenacao.js` | Quando o modal muda e a lista reordena, cada item desliza da posição antiga para a nova (técnica FLIP, com a Web Animations API). |
| `styles/movimento.css`, `styles/mapa.css` e `styles/rota.css` | Animações de entrada, troca de página (View Transitions API), esqueletos, o visual do mapa (placas 3D com espessura, poste e sombra, painel lateral e controles) e o painel de rotas. |

### Como o mapa funciona (sem biblioteca)

1. **Os dados** vêm do OpenStreetMap. O script `scripts/gerar-mapa.mjs` baixou uma vez o contorno da cidade, os 96 distritos (juntados nas 5 zonas da Prefeitura), as ruas, a água, o verde, os bairros e os prédios com altura informada. As coordenadas foram convertidas para um plano em que 1 unidade vale 25 m, e cada linha foi gravada como números inteiros relativos ao ponto anterior. O resultado, `public/mapa/sao-paulo.json`, tem cerca de 4,5 MB.
2. **A câmera** é a mesma para tudo: `perspective(1500px) rotateX(inclinação) scale3d(zoom) rotateZ(giro) translate(-x, -y)`. As placas recebem isso pelo CSS; o canvas faz a mesma conta em JavaScript (`utils/projecao.js`). Por isso as placas nunca escorregam das ruas.
3. **O chão é redesenhado a cada quadro** num `<canvas>`. Para caber em 16 ms, só entra o que está na tela (cada feição tem uma caixa), as ruas pequenas só aparecem de perto, de longe usa-se uma versão simplificada das avenidas e pontos a menos de 1 px do anterior são pulados.
4. **As ruas** são desenhadas em duas passadas, como nos apps de mapa: primeiro o contorno (mais largo e escuro), depois a pista. A largura cresce devagar com o zoom e diminui com a distância (perspectiva).
5. **Os nomes** são escolhidos a cada quadro por prioridade (zonas, rodovias, bairros, avenidas, ruas). Um nome só é escrito se não encostar em outro nem em uma placa, e o mesmo nome não se repete a menos de 320 px. Quem já estava na tela tem preferência, para não piscar.
6. **Os prédios** (altura real do OpenStreetMap) são desenhados de perto com o algoritmo do pintor: primeiro os mais distantes, cada parede com um tom conforme o lado em que bate o sol.
7. **As placas** ficam de pé "desfazendo" a câmera: `rotateZ(-giro) rotateX(-inclinação)`. É a técnica de *billboard*. A espessura vem de três camadas (frente colorida, miolo escuro e verso cinza) separadas no eixo Z.
8. **Arrastar e dar zoom no ponto certo** exige a conta inversa da perspectiva (`desprojetar`): descobrir qual ponto do chão está embaixo do cursor.

### Como a rota é calculada (sem biblioteca e sem internet)

1. **A rede de ruas** é um grafo gerado pelo `scripts/gerar-mapa.mjs`. No OpenStreetMap, duas vias se cruzam quando usam o mesmo nó (o mesmo id). Cada nó usado por mais de uma via vira um cruzamento; o pedaço de via entre dois cruzamentos vira uma aresta com comprimento, classe (expressa, arterial, principal...), nome e sentido. Entram só as vias principais (cerca de 29 mil cruzamentos, pouco mais de 1 MB): com as ruas de bairro o arquivo passaria de 10 MB.
2. **Saída e destino:** a busca procura nos nomes de ruas e bairros do próprio mapa. O ponto escolhido é ligado ao cruzamento mais próximo (essa ligação aparece tracejada).
3. **O A\*** é o Dijkstra com uma estimativa: além do custo até agora, ele soma a distância em linha reta até o destino dividida pela maior velocidade possível. Como essa estimativa nunca passa do custo real, o caminho achado é o melhor, e o algoritmo olha muito menos cruzamentos. A fila de prioridade é um heap binário escrito à mão (`class Fila`).
4. **O custo** de cada aresta é o tempo: comprimento dividido pela velocidade da classe da via (veículos) ou do modal (pedestre e bicicleta). Veículos não andam na contramão; pedestre e bicicleta não entram em via expressa.
5. **A rota mais segura** multiplica o custo das vias com ocorrência ativa de risco moderado (6×), alto (25×) ou crítico (80×) para o modal. O caminho só passa por elas quando não há alternativa razoável.
6. **O risco** é sempre calculado no Java: o front descobre quais vias do banco a rota percorre (pelo mesmo casamento de nomes do mapa) e chama `POST /api/rotas/avaliar`.

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

**Por que a rota é calculada no navegador e não no Java?**
A rede de ruas já está no front (sai do mesmo script do mapa) e o cálculo leva poucos milissegundos. O Java continua dono das regras: ele recebe as vias do caminho, calcula o risco para o modal e grava a rota com validação. Num produto real, o cálculo do caminho poderia ir para um serviço próprio.

**Por que não usaram Google Maps ou um serviço de rotas?**
Seria um serviço externo e uma tecnologia fora do conteúdo. Além disso, a solução precisa continuar útil fora do cenário ideal de conectividade: aqui a rota sai mesmo sem internet. O preço é não ter número de casa: o cidadão escolhe a rua, a avenida ou o bairro, ou marca o ponto no mapa.

**Como garantem que o diagrama de classes bate com o código?**
Ele foi gerado a partir das classes compiladas (`javap`). As fontes Mermaid estão em `docs/diagramas`.

---

## 7. Roteiro sugerido para a demonstração do pitch

Antes de gravar, rode `sql/script.sql` de novo para os horários ficarem recentes.

1. **Painel com "Moto" selecionado:** mostrar quantas ocorrências ativas pedem atenção. Trocar para "Carro": as placas do mapa mudam de cor e se levantam de novo, a lista se reordena e o número cai. Clicar em "Abrir o mapa 3D".
2. **Entrar como Ana (moto) e planejar uma rota:** do Tatuapé até a Sé. A rota mais rápida passa pelo alagamento da Radial Leste (risco alto para moto, trecho pintado de laranja); a rota mais segura desvia. Trocar o modal para Carro: o risco cai.
3. **Minhas rotas:** o alerta alto da Radial Leste na rota "Casa para o trabalho". Abrir a ocorrência e mostrar o risco para cada modal.
4. **Entrar como Bruno (carro):** a rota dele passa pela mesma Radial Leste, com risco baixo.
5. **Mapa de riscos (visão da gestão):** girar e inclinar a cidade, clicar numa placa (a câmera voa até ela e o painel mostra o detalhe) e clicar numa zona para filtrar.
6. **Como Ana, reportar uma ocorrência nova** com duas vias e mostrar a validação (enviar com erro primeiro).
7. **Entrar como Henrique (Defesa Civil):** a ocorrência nova está na fila. Confirmar com uma descrição. Abrir o detalhe e mostrar a linha do tempo.
8. **No SQL Developer:** `SELECT` em `CR_OCORRENCIA`, `CR_ACAO` e `CR_ROTA` mostrando que os dados novos estão no Oracle.
9. **Relatórios:** vias críticas e tempo de resposta por órgão.

Para o dado real da abertura do pitch, consulte fontes oficiais, como o Infosiga SP (acidentes de trânsito) e o CGE da Prefeitura de São Paulo (alagamentos), e cite a fonte no vídeo.

---

## 8. Antes de entregar

- [ ] Rodar `sql/script.sql` no Oracle da FIAP e conferir as contagens no fim.
- [ ] Subir a API com `.\mvnw.cmd compile exec:java` e ver "Banco Oracle conectado".
- [ ] Subir o front, entrar com as contas de demonstração e repetir o roteiro acima.
- [ ] Preencher `integrantes.txt` e o link do pitch no README e no `.txt`.
- [ ] Preencher a seção "Validação com pessoas reais" do README.
- [ ] Gerar o `.zip` sem `web/node_modules`, `web/dist` e `backend/target`.
