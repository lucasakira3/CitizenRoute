import { useCallback, useEffect, useRef, useState } from "react";
import { EtiquetaStatus, Gravidade } from "../components/Etiquetas.jsx";
import { ErroCarga, Vazio } from "../components/Estados.jsx";
import Icone from "../components/Icone.jsx";
import MapaCidade, { zonaDaRegiao } from "../components/MapaCidade.jsx";
import PlacaVia from "../components/PlacaVia.jsx";
import SeletorModal from "../components/SeletorModal.jsx";
import SinalRisco from "../components/SinalRisco.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api, consulta } from "../services/api.js";
import { formatarNumero, tempoRelativo } from "../utils/formato.js";
import { MODAL_FRASE } from "../utils/rotulos.js";

const ZONAS = [
  ["CENTRO", "Centro"],
  ["NORTE", "Norte"],
  ["SUL", "Sul"],
  ["LESTE", "Leste"],
  ["OESTE", "Oeste"],
];
const CELULAR = "(max-width: 860px)";

/** Modal pedido na URL (#/mapa?modal=MOTO), usado quando a pessoa vem do Painel. */
function modalDaUrl() {
  const busca = new URLSearchParams(window.location.hash.split("?")[1] ?? "");
  return busca.get("modal");
}

export default function MapaPage() {
  useTitulo("Mapa da cidade");
  const { sessao, dominio, rotulo } = useApp();
  const modalInicial = modalDaUrl();
  const [modal, setModal] = useState(
    dominio.modais.some((item) => item.codigo === modalInicial)
      ? modalInicial
      : sessao?.tipo === "CIDADAO"
        ? sessao.modal
        : "MOTO",
  );
  const [zona, setZona] = useState(null);
  const [selecionada, setSelecionada] = useState(null);
  const [celular, setCelular] = useState(() => window.matchMedia(CELULAR).matches);
  const [painelAberto, setPainelAberto] = useState(() => !window.matchMedia(CELULAR).matches);
  const [folga, setFolga] = useState({ esquerda: 0, inferior: 0 });
  const refMapa = useRef(null);
  const refPainel = useRef(null);

  const ativas = useCarregar(() => api.buscar(`/ocorrencias${consulta({ ativas: true, modal })}`), [modal]);
  const trajeto = useCarregar(
    () =>
      sessao?.tipo === "CIDADAO" ? api.buscar(`/cidadaos/${sessao.id}/vias-monitoradas`) : Promise.resolve([]),
    [sessao?.id, sessao?.tipo],
  );

  // Celular: o painel vira uma gaveta embaixo; computador: uma coluna à esquerda.
  useEffect(() => {
    const consultaMidia = window.matchMedia(CELULAR);
    const aoMudar = () => setCelular(consultaMidia.matches);
    consultaMidia.addEventListener("change", aoMudar);
    return () => consultaMidia.removeEventListener("change", aoMudar);
  }, []);

  // A câmera centraliza a cidade no espaço que sobra ao lado (ou acima) do painel.
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

  const ordenadas = ativas.dados ? [...ativas.dados].sort((a, b) => b.risco.valor - a.risco.valor) : [];
  const visiveis = zona
    ? ordenadas.filter((ocorrencia) => ocorrencia.vias.some((item) => zonaDaRegiao(item.via.regiao?.nome) === zona))
    : ordenadas;
  const comAlerta = ordenadas.filter((ocorrencia) => ocorrencia.risco.alerta).length;
  const escolhida = ordenadas.find((ocorrencia) => ocorrencia.id === selecionada) ?? null;

  const selecionar = useCallback(
    (id) => {
      setSelecionada(id);
      if (id && celular) setPainelAberto(true);
    },
    [celular],
  );

  function abrirDaLista(id) {
    setSelecionada(id);
    refMapa.current?.focarOcorrencia(id);
  }

  // Filtro de zona: a lista mostra só a zona e a câmera voa até ela
  function escolherZona(chave) {
    setZona(chave);
    setSelecionada(null);
    if (chave) {
      refMapa.current?.verZona(chave);
    } else {
      refMapa.current?.verCidade();
    }
  }

  return (
    <section className="mapa-tela" aria-labelledby="mapa-titulo" data-tela-cheia>
      {ativas.erro ? (
        <div className="mapa-tela__erro">
          <ErroCarga erro={ativas.erro} aoTentar={ativas.recarregar} />
        </div>
      ) : (
        <MapaCidade
          ref={refMapa}
          ocorrencias={ordenadas}
          modo="tela"
          zonaSelecionada={zona}
          selecionada={selecionada}
          aoSelecionar={selecionar}
          trajeto={trajeto.dados ?? []}
          folga={folga}
        />
      )}

      <aside
        ref={refPainel}
        className={`mapa-painel ${painelAberto ? "mapa-painel--aberto" : ""}`}
        aria-label="Ocorrências no mapa"
      >
        <header className="mapa-painel__cabeca">
          {celular && (
            <button
              type="button"
              className="mapa-painel__puxador"
              aria-expanded={painelAberto}
              aria-controls="mapa-painel-corpo"
              onClick={() => setPainelAberto((aberto) => !aberto)}
            >
              <span className="visualmente-oculto">{painelAberto ? "Recolher o painel" : "Abrir o painel"}</span>
            </button>
          )}
          <p className="sobretitulo">Mapa ao vivo · São Paulo</p>
          <h1 id="mapa-titulo" className="mapa-painel__titulo">
            A cidade calculada para quem {MODAL_FRASE[modal]}
          </h1>
          <SeletorModal valor={modal} aoMudar={setModal} legenda="Calcular o risco para" />
          {ativas.dados && (
            <p className="mapa-painel__resumo" aria-live="polite">
              <strong>
                {comAlerta} de {ordenadas.length}
              </strong>{" "}
              ocorrências ativas pedem atenção.
            </p>
          )}
        </header>

        <div id="mapa-painel-corpo" className="mapa-painel__corpo" hidden={celular && !painelAberto}>
          {escolhida ? (
            <DetalheNoMapa
              key={escolhida.id}
              ocorrencia={escolhida}
              rotulo={rotulo}
              aoVoltar={() => setSelecionada(null)}
            />
          ) : (
            <>
              <div className="mapa-zonas" role="group" aria-label="Filtrar por zona">
                <button type="button" aria-pressed={!zona} onClick={() => escolherZona(null)}>
                  Todas
                </button>
                {ZONAS.map(([chave, nome]) => (
                  <button
                    key={chave}
                    type="button"
                    aria-pressed={zona === chave}
                    onClick={() => escolherZona(zona === chave ? null : chave)}
                  >
                    {nome}
                  </button>
                ))}
              </div>
              {zona && (
                <p className="mapa-zonas__conta" role="status">
                  {visiveis.length} {visiveis.length === 1 ? "ocorrência ativa" : "ocorrências ativas"} nesta zona.
                </p>
              )}

              {!ativas.dados && !ativas.erro && <p className="mapa-painel__carregando">Carregando ocorrências…</p>}
              {ativas.dados && visiveis.length === 0 && (
                <Vazio titulo={zona ? "Nenhuma ocorrência ativa nesta zona." : "Nenhuma ocorrência ativa agora."} />
              )}

              <ol className="mapa-lista">
                {visiveis.map((ocorrencia, indice) => (
                  <li key={ocorrencia.id} style={{ "--i": Math.min(indice, 10) }}>
                    <button type="button" className="mapa-lista__item" onClick={() => abrirDaLista(ocorrencia.id)}>
                      <SinalRisco key={`${ocorrencia.risco.nivel}-${ocorrencia.risco.valor}`} risco={ocorrencia.risco} tamanho="pequeno" />
                      <span className="mapa-lista__texto">
                        <span className="mapa-lista__tipo">
                          <Icone nome={ocorrencia.tipo} tamanho={14} />
                          {rotulo("tiposOcorrencia", ocorrencia.tipo)} · {tempoRelativo(ocorrencia.dataRegistro)}
                        </span>
                        <span className="mapa-lista__titulo">{ocorrencia.titulo}</span>
                        <span className="mapa-lista__vias">
                          {ocorrencia.vias.map((item) => item.via.nome).join(" · ")}
                        </span>
                      </span>
                    </button>
                  </li>
                ))}
              </ol>

              <p className="mapa-painel__dica">
                Arraste para mover, use a roda ou a pinça para aproximar e o botão direito (ou dois dedos) para girar
                e inclinar. Toque numa placa para ver a ocorrência.
              </p>
            </>
          )}
        </div>
      </aside>
    </section>
  );
}

function DetalheNoMapa({ ocorrencia, rotulo, aoVoltar }) {
  const { risco } = ocorrencia;
  return (
    <article className="mapa-detalhe" aria-labelledby="mapa-detalhe-titulo">
      <button type="button" className="mapa-detalhe__voltar" onClick={aoVoltar}>
        <Icone nome="VOLTAR" tamanho={16} />
        Todas as ocorrências
      </button>
      <div className="mapa-detalhe__topo">
        <SinalRisco risco={risco} tamanho="grande" />
        <div>
          <p className="mapa-detalhe__tipo">
            <Icone nome={ocorrencia.tipo} tamanho={16} />
            {rotulo("tiposOcorrencia", ocorrencia.tipo)}
          </p>
          <h2 id="mapa-detalhe-titulo" className="mapa-detalhe__titulo">
            {ocorrencia.titulo}
          </h2>
        </div>
      </div>
      <p className="mapa-detalhe__risco">
        Risco <strong>{rotulo("niveisRisco", risco.nivel).toLowerCase()}</strong> ({formatarNumero(risco.valor, 1)}
        ){risco.alerta ? ": gera alerta para quem passa por aqui." : "."}
      </p>
      {ocorrencia.descricao && <p className="mapa-detalhe__descricao">{ocorrencia.descricao}</p>}
      <div className="mapa-detalhe__etiquetas">
        <EtiquetaStatus status={ocorrencia.status} />
        <Gravidade valor={ocorrencia.gravidade} />
      </div>
      <div className="mapa-detalhe__vias">
        {ocorrencia.vias.map((item) => (
          <PlacaVia key={item.via.id} via={item.via} impacto={item.nivelImpacto} />
        ))}
      </div>
      <p className="mapa-detalhe__quando">
        Registrada {tempoRelativo(ocorrencia.dataRegistro)} por {ocorrencia.autor.nome.split(" ")[0]} · responsável:{" "}
        {rotulo("orgaos", ocorrencia.orgaoResponsavel)}
      </p>
      <a className="botao botao--primario" href={`#/ocorrencias/${ocorrencia.id}`}>
        Ver detalhes e ações
      </a>
    </article>
  );
}
