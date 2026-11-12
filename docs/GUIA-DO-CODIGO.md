# Guia do código do Citizen Life

Este guia é para o grupo estudar o projeto antes da entrega e da apresentação. A regra da Global Solution é clara: código gerado com IA só vale se o grupo entende e consegue explicar. Leia com o código aberto ao lado.

---

## 1. O sistema em um minuto

- A **página inicial** é um hub com seis pilares da vida na cidade (mobilidade, lazer, alimentação, segurança, educação e habitação). Cada card leva a uma primeira consulta.
- O pilar de **mobilidade** está pronto. O cidadão digita a saída e o destino, escolhe o modal (carro, moto, ônibus, app, a pé, bike) e recebe a rota mais rápida e a mais segura, com o **risco** de cada ocorrência no caminho calculado para aquele modal. O **mapa de riscos** mostra todas as ocorrências da cidade.
- Com uma conta, o cidadão salva as rotas do dia a dia e recebe alerta quando surge, numa via da rota, uma ocorrência cujo risco para o seu modal passa de 4.
- Os outros cinco pilares mostram uma página "em breve" com o que a primeira consulta vai trazer.

O CRUD é em Java com JDBC no Oracle: o do cidadão (cadastro, editar, excluir a conta), o das rotas (salvar, listar, abrir, renomear, excluir) e o dos endereços. São **20 classes** e **6 tabelas**: o projeto foi enxugado de propósito para ser fácil de explicar. O React só mostra e envia dados: as regras ficam no Java.

---

## 2. O caminho de uma requisição

Exemplo: a Ana (moto) salva a rota "Casa para o trabalho".

| Passo | Onde | O que acontece |
|---|---|---|
| 1 | `web/src/pages/PlanejarRotaPage.jsx` | Com a rota calculada, o formulário valida o nome no navegador e chama `api.criar("/cidadaos/1/rotas", corpo)`. O corpo tem saída, destino, distância e os ids das vias do banco que o caminho percorre. |
| 2 | `web/src/services/api.js` | Faz o `fetch` com o cabeçalho `X-Usuario-Id` da Ana. |
| 3 | `web/vite.config.js` | Em desenvolvimento, o Vite repassa `/api` para `http://127.0.0.1:8080`. |
| 4 | `api/Servidor.java` | Troca os números do caminho por `{id}` e cai no `case "POST /api/cidadaos/{id}/rotas"` do `switch`. Lê o JSON (`Json.ler`) e monta um objeto `Rota`. Se qualquer exceção acontecer daqui em diante, é aqui que ela vira uma resposta HTTP com mensagem. |
| 5 | `service/RotaService.java` | Confere se quem pede é a dona da conta (`CidadaoService.exigirDono`), valida nome, saída, destino e distância com a `ValidacaoException`, confere se as vias existem e se ela ainda não tem 10 rotas. |
| 6 | `dao/RotaDao.java` | Abre uma transação: `INSERT` em `CL_ROTA`, lê o id gerado e faz um `INSERT` em lote em `CL_ROTA_VIA` (com a ordem de cada via). Se algo falhar, `rollback`. |
| 7 | volta | O `Servidor` converte a rota em mapa, escreve o JSON (`Json.escrever`) e responde **201**. O React vai para o perfil (Mobilidade › Minhas rotas), que já mostra a rota e os alertas dela. |

---

## 3. Backend, pacote por pacote

São 20 classes em 6 pacotes. Os diagramas estão em `docs/` (visão geral e uma página por camada).

### `model`: os dados

| Classe | Para que serve |
|---|---|
| `Cidadao` | Conta de quem usa o hub: nome, e-mail, hash da senha e o `modal` (opcional). `modalParaRisco()` devolve o modal ou, se ele não foi informado, o de quem anda a pé, o caso mais exposto. |
| `Via` | Avenida ou rua, com a zona da cidade. |
| `Ocorrencia` | Problema numa via (alagamento, acidente...). Guarda os pesos de cada tipo e a matriz de exposição de cada modal. `calcularRisco(modal)` é a fórmula do projeto e `nivel(risco)` diz a faixa (baixo, moderado, alto, crítico). |
| `Rota` | Rota salva: saída e destino (nome e posição no mapa), distância e a lista ordenada de vias que ela percorre. `passaPor(idVia)` é usado nos alertas. |
| `Endereco` | Casa, trabalho, estudo ou outro, com apelido e posição. `isTipoUnico()` diz que casa e trabalho só podem aparecer uma vez. |

Os tipos (modal, tipo de ocorrência, tipo de endereço) são `String` conferidas contra listas fixas no Java e um `CHECK` no Oracle: menos classes e o mesmo resultado.

### `dao`: SQL e JDBC

- `CrudDao<T>` é a interface do CRUD (`inserir`, `listar`, `buscarPorId`, `atualizar`, `excluir`), implementada por `CidadaoDao`, `RotaDao` e `EnderecoDao`. `OcorrenciaDao` só lista (com JOIN na via), e `RotaDao.listarVias()` atende o mapa.
- Todo acesso usa `try-with-resources` (a conexão, o `PreparedStatement` e o `ResultSet` fecham sozinhos) e `PreparedStatement` com `?` (sem concatenar valores do usuário no SQL, o que evita SQL injection).
- Salvar ou editar uma rota mexe em duas tabelas, então o `RotaDao` desliga o auto-commit, grava `CL_ROTA` e `CL_ROTA_VIA` e faz `commit` ou `rollback`.
- `Conexao` abre a conexão (lendo as variáveis de ambiente ou o `db.properties`) e traduz o erro do Oracle em mensagem clara. Exemplo: `ORA-00001` na constraint `UK_CL_ROTA_NOME` vira "Você já tem uma rota com este nome."

### `service`: regras de negócio

| Classe | Regras principais |
|---|---|
| `CidadaoService` | Cadastro simples (nome, e-mail e senha), login (confere o hash PBKDF2; a senha nunca é gravada), edição e exclusão. `exigirDono` confere se há alguém logado e se a conta é dele; as outras services usam esse método. |
| `RotaService` | CRUD das rotas: valida nome, saída, destino, distância e as vias do caminho; máximo de 10 rotas por cidadão. |
| `EnderecoService` | CRUD dos endereços: apelido único, casa e trabalho no máximo uma vez cada, no máximo 10 endereços. |
| `OcorrenciaService` | Lista as ocorrências com o risco para o modal (da maior para a menor), avalia um caminho antes de salvar (`noCaminho`) e monta os alertas: as ocorrências que estão em alguma via das rotas do cidadão. |

### `api`: a porta HTTP

- `Servidor` implementa `HttpHandler`. Troca os números do caminho por `{id}` (`/api/cidadaos/1/rotas` vira `POST /api/cidadaos/{id}/rotas`) e escolhe a operação num único `switch`. Também monta o JSON de cada objeto (a senha nunca sai) e, no `try/catch` central, converte cada exceção na resposta `{erro, campos}` com o status certo.
- `Json` é um leitor e escritor de JSON feito à mão, para não depender de biblioteca externa.

### `exception`

- `CitizenLifeException` herda de `RuntimeException` e carrega o status HTTP. Tem fábricas com nome: `naoEncontrado` (404), `naoAutenticado` (401), `acessoNegado` (403), `conflito` (409) e `bancoIndisponivel` (503).
- `ValidacaoException` (400) herda dela e junta o erro de cada campo do formulário antes de lançar, para o front mostrar todos de uma vez.

### `CitizenLife`

O `main`: cria os objetos na ordem DAO, depois Service, depois Servidor, passando cada um pelo construtor (injeção de dependência feita à mão). Depois sobe o `HttpServer` do JDK com 8 threads e diz se o banco respondeu.

---

## 4. Banco de dados

- **Prefixo `CL_`**: o usuário Oracle da FIAP é usado em várias disciplinas. O prefixo evita apagar ou colidir com uma tabela de outro trabalho.
- **6 tabelas:** `CL_CIDADAO`, `CL_VIA`, `CL_OCORRENCIA`, `CL_ROTA`, `CL_ROTA_VIA` e `CL_ENDERECO`. A associativa N:N é `CL_ROTA_VIA`, com a ordem da via no caminho. Cada ocorrência fica em uma via (N:1).
- **`ON DELETE CASCADE`** só onde faz sentido apagar junto: as rotas e os endereços de um cidadão que exclui a conta, e as vias de uma rota excluída.
- **Dados de demonstração** com datas relativas (`SYSDATE - 3/24` é três horas atrás), para a demo parecer sempre atual.

---

## 5. Front-end

| Arquivo | Papel |
|---|---|
| `App.jsx` | Lista de rotas e sessão do cidadão. |
| `utils/dominio.js` | Opções fixas do domínio: modais (com a velocidade média), tipos de ocorrência e de endereço, faixas de risco e limites. As mesmas listas que o Java confere. |
| `utils/pilares.js` | Os seis pilares do hub: nome, ícone, cor, primeira consulta e o que cada um vai trazer. |
| `components/Logo.jsx` | A marca: o cidadão de braços abertos (SVG próprio), usado no cabeçalho, no rodapé e na página inicial. |
| `pages/InicioPage.jsx` | O hub: o painel de abertura com o cidadão no centro e os seis pilares em órbita (posicionados com `rotate` e `translateX` em CSS) e os seis cards. Mobilidade aparece em destaque, como "Disponível". |
| `pages/MobilidadePage.jsx` | As duas consultas do pilar (Planejar rota e Mapa de riscos) e a regra do risco. |
| `pages/EmBrevePage.jsx` | Página dos pilares que ainda não têm consulta (`/lazer`, `/alimentacao`...). Um caminho que não é pilar cai na página "não encontrada". |
| `hooks/useRota.js` | Navegação por hash (`#/mobilidade`), sem biblioteca de rotas. |
| `hooks/useCarregar.js` | Busca dados e guarda `carregando`, `erro` e `dados`. |
| `hooks/useFormulario.js` | Valores, erros por campo, envio e exibição dos erros vindos do Java. |
| `utils/validacao.js` | As mesmas regras de tamanho, e-mail e senha da validação do Java. |
| `components/Layout.jsx` | Cabeçalho com os seis pilares e rodapé, iguais em todas as páginas. |
| `components/PlacaVia.jsx` | Mostra a via como uma placa de rua, com a zona. |
| `components/SinalRisco.jsx` | Mostra o risco como uma placa de advertência (losango) colorida pela faixa. |
| `components/MapaCidade.jsx` | O mapa. Carrega os dados da cidade, desenha o chão num `<canvas>` e põe por cima as placas 3D das ocorrências (HTML + CSS 3D), cada uma na via atingida, na cor do risco. Destaca as vias atingidas e, na página de rotas, desenha a rota calculada e os marcadores A e B. |
| `utils/mapaCidade.js` | Carrega `public/mapa/sao-paulo.json`, decodifica as linhas e monta o índice de ruas por nome. É por esse índice que uma via do banco ("Avenida Paulista") acha o seu traçado; a comparação ignora acentos, títulos ("Doutor", "Professor") e o que vem entre parênteses ("Marginal Tietê (Expressa)"). |
| `utils/desenhoMapa.js` | Desenha o mapa a cada quadro, como um app de mapa: chão, verde, água, trilhos, ruas (da menor para a maior), vias com ocorrência, prédios em 3D e os nomes, escolhidos para não se sobreporem. |
| `utils/projecao.js` | As contas da perspectiva (`projetar` e `desprojetar`), usadas pela câmera e pelo desenho. |
| `scripts/gerar-mapa.mjs` | Script que baixa os dados do OpenStreetMap (Overpass API), projeta as coordenadas, emenda os trechos de cada rua, simplifica as linhas e grava o arquivo do mapa e a rede de ruas para as rotas. Só roda quando alguém quer atualizar o mapa; o site não depende dele. |
| `utils/rotas.js` | O cálculo de rotas: carrega a rede (`public/mapa/sao-paulo-rotas.json`), acha o cruzamento mais próximo de um ponto, roda o A\*, descobre quais vias do banco a rota percorre, monta as penalidades da rota mais segura e faz a busca de ruas e bairros. |
| `pages/PlanejarRotaPage.jsx` | Página Planejar rota: saída e destino (`components/CampoLugar.jsx`, com sugestões e marcação no mapa), modal, as opções mais rápida e mais segura lado a lado, as ocorrências no caminho e o formulário para salvar. |
| `pages/PerfilPage.jsx` | O perfil, no estilo das contas de loja on-line: barra lateral (Minha conta e uma linha por pilar) e o conteúdo ao centro; dentro do pilar, as funções ficam em abas. O caminho escolhe o que abrir: `#/perfil`, `#/perfil/dados`, `#/perfil/<pilar>` (abre a primeira aba) e `#/perfil/mobilidade/rotas`, `.../locomocao` ou `.../enderecos`. Os pilares em breve mostram as abas futuras desativadas. |
| `components/perfil/*` | As seções do perfil: `ResumoConta` (cartão do cidadão e três números), `DadosConta`, `MinhasRotas` (alertas e CRUD das rotas), `Locomocao` (cards de modal: um clique escolhe e já salva) e `Enderecos` (CRUD dos endereços, usando o mesmo `CampoLugar` da tela de rotas). |
| `hooks/useCameraMapa.js` | A câmera do mapa: arrastar, roda e pinça para zoom, botão direito ou dois dedos para girar, teclado, voos animados com `requestAnimationFrame` e enquadramento automático. A cada quadro escreve a câmera em variáveis CSS (para as placas) e redesenha o canvas, sem re-renderizar o React. |
| `pages/MapaPage.jsx` | Página Mapa de riscos em tela cheia: painel com o seletor de modal, a lista de ocorrências (clicar voa até a placa) e o detalhe da ocorrência escolhida. Aceita `?ocorrencia=3` na URL, que é como os alertas abrem o mapa já na ocorrência. No celular o painel vira uma gaveta. |
| `components/Icone.jsx` | Ícones próprios em SVG para os 6 pilares, os 7 tipos de ocorrência e os 6 modais. |
| `styles/base.css` | Os tokens da paleta (cores, fontes, raios e sombras) e a estrutura da página. |
| `styles/hub.css` | O painel de abertura com a órbita, os cards dos pilares, o cabeçalho de cada pilar (tingido com a cor dele, definida em `--pilar-*` no `base.css`) e a página "em breve". |
| `styles/movimento.css`, `styles/mapa.css` e `styles/rota.css` | Animações de entrada e troca de página (View Transitions API), o visual do mapa (placas 3D com espessura, poste e sombra, painel lateral e controles) e o painel de rotas. |

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
5. **A rota mais segura** multiplica o custo das vias com ocorrência de risco moderado (6×), alto (25×) ou crítico (80×) para o modal. O caminho só passa por elas quando não há alternativa razoável.
6. **O risco** é sempre calculado no Java: o front descobre quais vias do banco a rota percorre (pelo mesmo casamento de nomes do mapa) e chama `POST /api/rotas/avaliar`.



