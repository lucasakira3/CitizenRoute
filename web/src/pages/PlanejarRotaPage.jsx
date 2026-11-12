import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import CampoLugar from "../components/CampoLugar.jsx";
import Campo from "../components/Campo.jsx";
import { Aviso } from "../components/Estados.jsx";
import Icone from "../components/Icone.jsx";
import MapaCidade from "../components/MapaCidade.jsx";
import PlacaVia from "../components/PlacaVia.jsx";
import SeletorModal from "../components/SeletorModal.jsx";
import SinalRisco from "../components/SinalRisco.jsx";
import { useApp } from "../contexto.js";
import { useFormulario } from "../hooks/useFormulario.js";
import { navegar } from "../hooks/useRota.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api, consulta } from "../services/api.js";
import { avisarNaProximaPagina } from "../services/aviso.js";
import { formatarMinutos, formatarNumero } from "../utils/formato.js";
import { carregarMapa } from "../utils/mapaCidade.js";
import {
  calcularRota,
  carregarRede,
  corpoDaRota,
  descreverPonto,
  minutosPara,
  nomesDasVias,
  penalidadesDeRisco,
  prepararBusca,
  trechosComRisco,
  viasNaRota,
} from "../utils/rotas.js";
import { ICONE_ENDERECO, MODAL_FRASE } from "../utils/rotulos.js";
import { criarValidador } from "../utils/validacao.js";

const CELULAR = "(max-width: 860px)";
const ORDEM_RISCO = { BAIXO: 1, MODERADO: 2, ALTO: 3, CRITICO: 4 };
const MARCADOR = { origem: { texto: "A", cor: "#1f2a24" }, destino: { texto: "B", cor: "#2f93c8" } };

/** Rota salva pedida na URL (#/mobilidade/rota?id=3), quando o cidadão abre uma rota de "Minhas rotas". */
function idDaUrl() {
  const busca = new URLSearchParams(window.location.hash.split("?")[1] ?? "");
  const id = Number(busca.get("id"));
  return Number.isInteger(id) && id > 0 ? id : null;
}

function piorNivel(alertas) {
  return alertas.reduce(
    (pior, alerta) => (!pior || ORDEM_RISCO[alerta.risco.nivel] > ORDEM_RISCO[pior.nivel] ? alerta.risco : pior),
    null,
  );
}

export default function PlanejarRotaPage() {
  useTitulo("Planejar rota");
  const { sessao, dominio, rotulo } = useApp();
  const cidadao = Boolean(sessao);
  const [idRota] = useState(idDaUrl);

  const [origem, setOrigem] = useState(null);
  const [destino, setDestino] = useState(null);
  const [modal, setModal] = useState(sessao?.modal ?? "CARRO");
  const [criterio, setCriterio] = useState("SEGURA");
  const [marcando, setMarcando] = useState(null);
  const [rotaSalva, setRotaSalva] = useState(null);

  const [base, setBase] = useState(null); // { dados, rede, vias, nomesPorVia }
  const [erroBase, setErroBase] = useState(null);
  const [resultado, setResultado] = useState(null); // { RAPIDA: {...}, SEGURA: {...} }
  const [calculando, setCalculando] = useState(false);
  const [erroRota, setErroRota] = useState(null);
  const [aviso, setAviso] = useState(null);

  const [celular, setCelular] = useState(() => window.matchMedia(CELULAR).matches);
  const [painelAberto, setPainelAberto] = useState(true);
  const [folga, setFolga] = useState({ esquerda: 0, inferior: 0 });
  const refMapa = useRef(null);
  const refPainel = useRef(null);
  const geracao = useRef(0);

  const infoModal = dominio.modais.find((item) => item.codigo === modal);

  // Mapa, rede de ruas e vias cadastradas: tudo o que o cálculo precisa
  useEffect(() => {
    let ativo = true;
    Promise.all([carregarMapa(), carregarRede(), api.buscar("/vias")])
      .then(([dados, rede, vias]) => {
        if (!ativo) return;
        setBase({ dados, rede, vias, nomesPorVia: nomesDasVias(dados, vias) });
        // Monta o índice de ruas e bairros logo em seguida, para a primeira busca não travar a digitação
        setTimeout(() => prepararBusca(dados), 50);
      })
      .catch((erro) => ativo && setErroBase(erro));
    return () => {
      ativo = false;
    };
  }, []);

  // Endereços salvos no perfil (casa, trabalho...) viram atalhos para a saída e o destino
  const [enderecos, setEnderecos] = useState([]);
  useEffect(() => {
    if (!cidadao) return;
    api
      .buscar(`/cidadaos/${sessao.id}/enderecos`)
      .then(setEnderecos)
      .catch(() => setEnderecos([]));
  }, [cidadao, sessao?.id]);

  /** Preenche a saída se ela estiver vazia; senão, o destino. */
  function usarEndereco(endereco) {
    const ponto = { ...endereco.local };
    if (!origem) setOrigem(ponto);
    else setDestino(ponto);
  }

  // Rota salva aberta pela URL: preenche saída, destino, critério e o nome
  useEffect(() => {
    if (!idRota || !cidadao) return;
    api
      .buscar(`/cidadaos/${sessao.id}/rotas/${idRota}`)
      .then((rota) => {
        setRotaSalva(rota);
        setOrigem(rota.origem);
        setDestino(rota.destino);
      })
      .catch((erro) => setAviso({ tipo: "erro", texto: erro.message }));
  }, [idRota, cidadao, sessao?.id]);

  // Celular: o painel vira uma gaveta embaixo; computador: uma coluna à esquerda
  useEffect(() => {
    const consultaMidia = window.matchMedia(CELULAR);
    const aoMudar = () => setCelular(consultaMidia.matches);
    consultaMidia.addEventListener("change", aoMudar);
    return () => consultaMidia.removeEventListener("change", aoMudar);
  }, []);

  useEffect(() => {
    const painel = refPainel.current;
    if (!painel || typeof ResizeObserver === "undefined") return undefined;
    const observador = new ResizeObserver(() => {
      const caixa = painel.getBoundingClientRect();
      setFolga(celular ? { esquerda: 0, inferior: caixa.height + 12 } : { esquerda: caixa.width + 24, inferior: 0 });
    });
    observador.observe(painel);
    return () => observador.disconnect();
  }, [celular]);

  /**
   * Calcula as duas rotas (mais rápida e mais segura) e pede ao Java o risco de cada uma.
   * O caminho sai do navegador; a regra de risco fica toda no backend.
   */
  const calcular = useCallback(async () => {
    if (!base || !origem || !destino || !infoModal) return;
    const minha = ++geracao.current;
    setCalculando(true);
    setErroRota(null);
    try {
      const ativas = await api.buscar(`/ocorrencias${consulta({ modal })}`);
      const opcoes = {
        motorizado: infoModal.motorizado,
        velocidade: infoModal.velocidadeMediaKmh,
      };
      const rapida = calcularRota(base.rede, origem, destino, opcoes);
      const segura = calcularRota(base.rede, origem, destino, {
        ...opcoes,
        penalidade: penalidadesDeRisco(ativas, base.nomesPorVia),
      });
      const avaliar = async (rota) => {
        const vias = viasNaRota(rota, base.vias, base.nomesPorVia);
        const alertas = await api.criar("/rotas/avaliar", { modal, vias: vias.map((via) => via.id) });
        return {
          rota,
          vias,
          alertas,
          pior: piorNivel(alertas),
          minutos: minutosPara(rota.distanciaMetros, infoModal.velocidadeMediaKmh),
          trechos: trechosComRisco(rota, alertas, base.nomesPorVia),
        };
      };
      const [RAPIDA, SEGURA] = await Promise.all([avaliar(rapida), avaliar(segura)]);
      if (minha !== geracao.current) return;
      setResultado({ RAPIDA, SEGURA, iguais: rapida.distanciaMetros === segura.distanciaMetros });
      refMapa.current?.verPontos(rapida.pontos.length > segura.pontos.length ? rapida.pontos : segura.pontos);
      if (celular) setPainelAberto(true);
    } catch (erro) {
      if (minha !== geracao.current) return;
      setResultado(null);
      setErroRota(erro.message);
    } finally {
      if (minha === geracao.current) setCalculando(false);
    }
  }, [base, origem, destino, modal, infoModal, celular]);

  // Recalcula sozinho quando saída, destino ou modal mudam
  useEffect(() => {
    if (origem && destino) {
      calcular();
    } else {
      setResultado(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [origem, destino, modal, base]);

  const escolhida = resultado?.[criterio] ?? null;

  const desenhoRota = useMemo(
    () => (escolhida ? { pontos: escolhida.rota.pontos, acessos: escolhida.rota.acessos, trechos: escolhida.trechos } : null),
    [escolhida],
  );
  const marcadores = useMemo(
    () =>
      [
        origem && { x: origem.x, y: origem.y, ...MARCADOR.origem },
        destino && { x: destino.x, y: destino.y, ...MARCADOR.destino },
      ].filter(Boolean),
    [origem, destino],
  );
  const ocorrenciasNaRota = useMemo(
    () => (escolhida ? escolhida.alertas.map((alerta) => ({ ...alerta.ocorrencia, risco: alerta.risco })) : []),
    [escolhida],
  );

  const aoClicarNoChao = useCallback(
    (x, y) => {
      if (!marcando || !base) return;
      const ponto = { descricao: descreverPonto(base.dados, base.rede, x, y), x, y };
      if (marcando === "origem") setOrigem(ponto);
      else setDestino(ponto);
      setMarcando(null);
    },
    [marcando, base],
  );

  function inverter() {
    setOrigem(destino);
    setDestino(origem);
  }

  function limpar() {
    setOrigem(null);
    setDestino(null);
    setResultado(null);
    setErroRota(null);
    setRotaSalva(null);
    if (idRota) navegar("/mobilidade/rota");
  }

  return (
    <section className="mapa-tela rota-tela" aria-labelledby="rota-titulo" data-tela-cheia>
      <MapaCidade
        ref={refMapa}
        ocorrencias={ocorrenciasNaRota}
        modo="tela"
        rota={desenhoRota}
        marcadores={marcadores}
        aoClicarNoChao={marcando ? aoClicarNoChao : undefined}
        rotuloRegiao="Mapa de São Paulo com a rota calculada. Arraste para mover e use a roda ou a pinça para aproximar."
        folga={folga}
      />
      {marcando && (
        <p className="rota-tela__marcando" role="status">
          <Icone nome="ALVO" tamanho={18} />
          Clique no mapa para marcar {marcando === "origem" ? "a saída" : "o destino"}.
          <button type="button" className="botao-texto" onClick={() => setMarcando(null)}>
            Cancelar
          </button>
        </p>
      )}

      <aside
        ref={refPainel}
        className={`mapa-painel rota-painel ${painelAberto ? "mapa-painel--aberto" : ""}`}
        aria-label="Planejar rota"
      >
        <header className="mapa-painel__cabeca">
          {celular && (
            <button
              type="button"
              className="mapa-painel__puxador"
              aria-expanded={painelAberto}
              aria-controls="rota-painel-corpo"
              onClick={() => setPainelAberto((aberto) => !aberto)}
            >
              <span className="visualmente-oculto">{painelAberto ? "Recolher o painel" : "Abrir o painel"}</span>
            </button>
          )}
          {rotaSalva && <p className="sobretitulo">Rota salva</p>}
          <h1 id="rota-titulo" className="mapa-painel__titulo">
            {rotaSalva ? rotaSalva.nome : "Para onde você vai?"}
          </h1>
        </header>

        <div id="rota-painel-corpo" className="mapa-painel__corpo" hidden={celular && !painelAberto}>
          <Aviso aviso={aviso} aoFechar={() => setAviso(null)} />
          {erroBase && <Aviso aviso={{ tipo: "erro", texto: erroBase.message }} />}

          <div className="rota-pontos">
            <CampoLugar
              id="rota-origem"
              rotulo="Saída"
              letra="A"
              valor={origem}
              dados={base?.dados}
              placeholder="Rua, avenida ou bairro"
              aoEscolher={setOrigem}
              marcandoNoMapa={marcando === "origem"}
              aoMarcarNoMapa={() => setMarcando(marcando === "origem" ? null : "origem")}
            />
            <button
              type="button"
              className="rota-pontos__inverter"
              onClick={inverter}
              disabled={!origem && !destino}
              title="Inverter saída e destino"
            >
              <Icone nome="TROCAR" tamanho={18} />
              <span className="visualmente-oculto">Inverter saída e destino</span>
            </button>
            <CampoLugar
              id="rota-destino"
              rotulo="Destino"
              letra="B"
              valor={destino}
              dados={base?.dados}
              placeholder="Rua, avenida ou bairro"
              aoEscolher={setDestino}
              marcandoNoMapa={marcando === "destino"}
              aoMarcarNoMapa={() => setMarcando(marcando === "destino" ? null : "destino")}
            />
          </div>

          {enderecos.length > 0 && (
            <div className="atalhos-endereco" role="group" aria-label="Seus endereços salvos">
              {enderecos.map((endereco) => {
                const emUso = [origem, destino].some(
                  (ponto) => ponto && ponto.x === endereco.local.x && ponto.y === endereco.local.y,
                );
                return (
                  <button
                    key={endereco.id}
                    type="button"
                    aria-pressed={emUso}
                    title={`${endereco.local.descricao}: usar como ${origem ? "destino" : "saída"}`}
                    onClick={() => usarEndereco(endereco)}
                  >
                    <Icone nome={ICONE_ENDERECO[endereco.tipo]} tamanho={14} />
                    {endereco.apelido}
                  </button>
                );
              })}
            </div>
          )}

          <SeletorModal valor={modal} aoMudar={setModal} legenda="Como você vai" />

          {!base && !erroBase && <p className="mapa-painel__carregando">Carregando o mapa e a rede de ruas…</p>}
          {calculando && (
            <p className="mapa-painel__carregando" role="status">
              Calculando a rota e o risco para quem {MODAL_FRASE[modal]}…
            </p>
          )}
          {erroRota && <Aviso aviso={{ tipo: "erro", texto: erroRota }} />}

          {!origem || !destino ? (
            <p className="rota-dica">
              Escolha a saída e o destino, ou marque no mapa.
            </p>
          ) : (
            resultado && (
              <Resultado
                resultado={resultado}
                criterio={criterio}
                aoEscolher={setCriterio}
                modal={modal}
                rotulo={rotulo}
              />
            )
          )}

          {escolhida && (
            <SalvarRota
              cidadao={cidadao}
              rotaSalva={rotaSalva}
              origem={origem}
              destino={destino}
              escolhida={escolhida}
              modalDiferente={cidadao && modal !== (sessao.modal ?? dominio.modalPadraoRisco)}
              aoSalvar={(rota, texto) => {
                setRotaSalva(rota);
                setAviso({ tipo: "sucesso", texto });
              }}
            />
          )}

          {(origem || destino) && (
            <button type="button" className="botao botao--discreto botao--pequeno rota-limpar" onClick={limpar}>
              Começar outra rota
            </button>
          )}
        </div>
      </aside>
    </section>
  );
}

// ---------------------------------------------------------------- resultado

function Resultado({ resultado, criterio, aoEscolher, modal, rotulo }) {
  const escolhida = resultado[criterio];
  const frase = MODAL_FRASE[modal];
  const evitadas = resultado.RAPIDA.alertas.length - resultado.SEGURA.alertas.length;
  const minutosAMais = resultado.SEGURA.minutos - resultado.RAPIDA.minutos;

  return (
    <div className="rota-resultado">
      <fieldset className="rota-opcoes">
        <legend className="visualmente-oculto">Escolha a rota</legend>
        {["SEGURA", "RAPIDA"].map((codigo) => {
          const opcao = resultado[codigo];
          return (
            <label key={codigo} className={`rota-opcao ${criterio === codigo ? "rota-opcao--ativa" : ""}`}>
              <input
                type="radio"
                name="criterio-rota"
                value={codigo}
                checked={criterio === codigo}
                onChange={() => aoEscolher(codigo)}
              />
              <span className="rota-opcao__topo">
                <Icone nome={codigo === "SEGURA" ? "ESCUDO" : "RELOGIO"} tamanho={18} />
                {codigo === "SEGURA" ? "Mais segura" : "Mais rápida"}
              </span>
              <span className="rota-opcao__tempo">{formatarMinutos(opcao.minutos)}</span>
              <span className="rota-opcao__meta">
                {formatarNumero(opcao.rota.distanciaMetros / 1000, 1)} km ·{" "}
                {opcao.alertas.length === 0
                  ? "sem ocorrências"
                  : `${opcao.alertas.length} ${opcao.alertas.length === 1 ? "ocorrência" : "ocorrências"}`}
              </span>
              {opcao.pior && (
                <span className={`rota-opcao__risco rota-opcao__risco--${opcao.pior.nivel}`}>
                  Pior trecho: risco {rotulo("niveisRisco", opcao.pior.nivel).toLowerCase()}
                </span>
              )}
            </label>
          );
        })}
      </fieldset>

      <p className="rota-resultado__comparacao" aria-live="polite">
        {resultado.iguais
          ? "A rota mais rápida já é a mais segura para o seu modal."
          : evitadas > 0
            ? `A rota mais segura evita ${evitadas} ${evitadas === 1 ? "ocorrência" : "ocorrências"}${
                minutosAMais > 0 ? ` e leva ${formatarMinutos(minutosAMais)} a mais` : ""
              }.`
            : "As duas rotas passam pelas mesmas ocorrências; a mais segura desvia dos trechos mais perigosos."}
      </p>

      <h2 className="rota-resultado__titulo">
        {escolhida.alertas.length === 0
          ? "Caminho livre"
          : `${escolhida.alertas.length} ${escolhida.alertas.length === 1 ? "ocorrência" : "ocorrências"} no caminho`}
      </h2>
      {escolhida.alertas.length === 0 ? (
        <p className="texto-apoio">Nenhuma ocorrência ativa nas vias desta rota agora.</p>
      ) : (
        <ol className="rota-alertas">
          {escolhida.alertas.map((alerta) => (
            <li key={alerta.id} className={`rota-alerta rota-alerta--${alerta.risco.nivel}`}>
              <SinalRisco risco={alerta.risco} tamanho="pequeno" />
              <div className="rota-alerta__texto">
                <p className="rota-alerta__nivel">
                  Risco {rotulo("niveisRisco", alerta.risco.nivel).toLowerCase()} para quem {frase}
                </p>
                <a className="rota-alerta__titulo" href={`#/mobilidade/mapa?modal=${modal}&ocorrencia=${alerta.id}`}>
                  {alerta.titulo}
                </a>
                <div className="lista-placas">
                  <PlacaVia via={alerta.via} semRegiao />
                </div>
              </div>
            </li>
          ))}
        </ol>
      )}

      {escolhida.vias.length > 0 && (
        <details className="rota-vias">
          <summary>
            Vias acompanhadas pelo sistema neste caminho ({escolhida.vias.length})
          </summary>
          <div className="lista-placas">
            {escolhida.vias.map((via) => (
              <PlacaVia key={via.id} via={via} semRegiao />
            ))}
          </div>
        </details>
      )}
    </div>
  );
}

// ------------------------------------------------------------------- salvar

function SalvarRota({ cidadao, rotaSalva, origem, destino, escolhida, modalDiferente, aoSalvar }) {
  const { sessao, dominio } = useApp();
  const form = useFormulario({ nome: rotaSalva?.nome ?? "" });

  useEffect(() => {
    if (rotaSalva) form.definir("nome", rotaSalva.nome);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rotaSalva?.id]);

  if (!cidadao) {
    return (
      <div className="rota-salvar rota-salvar--convite">
        <p>
          <strong>Quer receber alertas desta rota?</strong> Entre com a sua conta de cidadão para salvar.
        </p>
        <a className="botao botao--secundario botao--pequeno" href="#/entrar">
          Entrar
        </a>
      </div>
    );
  }

  async function salvar(evento) {
    evento.preventDefault();
    const corpo = {
      nome: form.valores.nome.trim(),
      distanciaMetros: escolhida.rota.distanciaMetros,
      origem: { descricao: origem.descricao, x: origem.x, y: origem.y },
      destino: { descricao: destino.descricao, x: destino.x, y: destino.y },
      vias: escolhida.vias.map((via) => via.id),
    };
    let salva = null;
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.texto("nome", valores.nome, "Nome da rota", 2, 60);
        return validador.erros;
      },
      async () => {
        salva = rotaSalva
          ? await api.atualizar(`/cidadaos/${sessao.id}/rotas/${rotaSalva.id}`, corpoDaRota(rotaSalva, corpo))
          : await api.criar(`/cidadaos/${sessao.id}/rotas`, corpo);
      },
    );
    if (ok && salva) {
      if (!rotaSalva) {
        avisarNaProximaPagina("sucesso", `Rota "${salva.nome}" salva. Os alertas dela aparecem aqui.`);
        navegar("/perfil/mobilidade/rotas");
        return;
      }
      aoSalvar(salva, "Rota atualizada.");
    }
  }

  return (
    <form className="rota-salvar formulario formulario--compacto" onSubmit={salvar} noValidate>
      <h2 className="rota-salvar__titulo">{rotaSalva ? "Atualizar a rota salva" : "Salvar esta rota"}</h2>
      <p className="texto-apoio">
        Você recebe alerta quando surgir ocorrência numa das {escolhida.vias.length} vias do caminho acompanhadas pelo sistema.
        {modalDiferente &&
          ` Os alertas usam ${sessao.modal ? "o seu modal do cadastro" : "o modal padrão, já que você ainda não informou o seu"} (${dominio.modais
            .find((item) => item.codigo === (sessao.modal ?? dominio.modalPadraoRisco))
            ?.rotulo.toLowerCase()}).`}
      </p>
      <Aviso aviso={form.aviso} />
      <Campo id="rota-nome" rotulo="Nome da rota" erro={form.erros.nome}>
        <input
          type="text"
          maxLength={60}
          placeholder="Ex.: Casa → Trabalho"
          value={form.valores.nome}
          onChange={form.alterar("nome")}
        />
      </Campo>
      <button type="submit" className="botao botao--primario" disabled={form.enviando}>
        {form.enviando ? "Salvando…" : rotaSalva ? "Salvar alterações" : "Salvar rota"}
      </button>
    </form>
  );
}
