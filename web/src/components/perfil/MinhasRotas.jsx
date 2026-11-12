import { useState } from "react";
import { useApp } from "../../contexto.js";
import { useCarregar } from "../../hooks/useCarregar.js";
import { useFormulario } from "../../hooks/useFormulario.js";
import { navegar } from "../../hooks/useRota.js";
import { api } from "../../services/api.js";
import { formatarMinutos, formatarNumero } from "../../utils/formato.js";
import { corpoDaRota, minutosPara } from "../../utils/rotas.js";
import { criarValidador } from "../../utils/validacao.js";
import Campo from "../Campo.jsx";
import { Carregando, ErroCarga, Vazio } from "../Estados.jsx";
import Icone from "../Icone.jsx";
import PlacaVia from "../PlacaVia.jsx";
import SinalRisco from "../SinalRisco.jsx";
import IntroAba from "./IntroAba.jsx";

const ORDEM_RISCO = { BAIXO: 1, MODERADO: 2, ALTO: 3, CRITICO: 4 };

/** Mobilidade › Minhas rotas: os alertas do dia e o CRUD das rotas salvas. */
export default function MinhasRotas({ setAviso }) {
  const { sessao } = useApp();
  const alertas = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/alertas`), [sessao.id, sessao.modal]);
  const rotas = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/rotas`), [sessao.id]);

  function aposMudarRotas(texto) {
    setAviso({ tipo: "sucesso", texto });
    rotas.recarregar();
    alertas.recarregar();
  }

  return (
    <>
      <IntroAba>
        <a className="botao botao--primario" href="#/mobilidade/rota">
          <Icone nome="MAIS" tamanho={18} />
          Nova rota
        </a>
      </IntroAba>
      <Alertas estado={alertas} temRotas={(rotas.dados ?? []).length > 0} />
      <RotasSalvas
        estado={rotas}
        alertas={alertas.dados ?? []}
        aoMudar={aposMudarRotas}
        aoErro={(texto) => setAviso({ tipo: "erro", texto })}
      />
    </>
  );
}

function Alertas({ estado, temRotas }) {
  const { sessao, dominio, rotulo } = useApp();
  // Sem modal no cadastro, o Java calcula o risco para o modal padrão (quem anda a pé)
  const modalRisco = sessao.modal ?? dominio.modalPadraoRisco;
  const alertas = estado.dados ?? [];
  const importantes = alertas.filter((alerta) => alerta.risco.alerta);
  const outros = alertas.filter((alerta) => !alerta.risco.alerta);

  if (estado.carregando && !estado.dados) return <Carregando texto="Conferindo as suas rotas…" />;
  if (estado.erro) return <ErroCarga erro={estado.erro} aoTentar={estado.recarregar} />;
  if (!temRotas) return null;

  return (
    <div className="perfil-alertas">
      <h2 className="perfil-subtitulo">Alertas</h2>
      {alertas.length === 0 && <Vazio titulo="Caminho livre." />}

      {importantes.map((alerta) => (
        <article key={alerta.ocorrencia.id} className={`alerta alerta--${alerta.risco.nivel}`}>
          <SinalRisco risco={alerta.risco} tamanho="grande" />
          <div className="alerta__corpo">
            <p className="alerta__nivel">
              Risco {rotulo("niveisRisco", alerta.risco.nivel).toLowerCase()}
            </p>
            <h3 className="alerta__titulo">
              <a href={`#/mobilidade/mapa?modal=${modalRisco}&ocorrencia=${alerta.ocorrencia.id}`}>{alerta.ocorrencia.titulo}</a>
            </h3>
            <p className="alerta__rotas">{alerta.rotas.map((rota) => rota.nome).join(" · ")}</p>
            <div className="lista-placas">
              {alerta.viasAfetadas.map((via) => (
                <PlacaVia key={via.id} via={via} semRegiao />
              ))}
            </div>
          </div>
        </article>
      ))}

      {outros.length > 0 && (
        <div className="alertas-baixos">
          <h3 className="subtitulo">Risco baixo</h3>
          <ul>
            {outros.map((alerta) => (
              <li key={alerta.ocorrencia.id}>
                <SinalRisco risco={alerta.risco} tamanho="pequeno" />
                <a href={`#/mobilidade/mapa?modal=${modalRisco}&ocorrencia=${alerta.ocorrencia.id}`}>{alerta.ocorrencia.titulo}</a>
                <span className="alertas-baixos__via">{alerta.rotas.map((rota) => rota.nome).join(", ")}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

/** Pior alerta de cada rota, para o resumo da lista. */
export function piorPorRota(alertas) {
  const pior = new Map();
  alertas.forEach((alerta) => {
    alerta.rotas.forEach((rota) => {
      const atual = pior.get(rota.id);
      if (!atual || ORDEM_RISCO[alerta.risco.nivel] > ORDEM_RISCO[atual.risco.nivel]) pior.set(rota.id, alerta);
    });
  });
  return pior;
}

function RotasSalvas({ estado, alertas, aoMudar, aoErro }) {
  const { sessao, dominio } = useApp();
  const [editando, setEditando] = useState(null);
  const rotas = estado.dados ?? [];
  const pior = piorPorRota(alertas);
  const modalRota = sessao.modal ?? dominio.modalPadraoRisco;
  const velocidade = dominio.modais.find((modal) => modal.codigo === modalRota)?.velocidadeMediaKmh ?? 20;

  async function excluir(rota) {
    if (!window.confirm(`Excluir a rota "${rota.nome}"?`)) {
      return;
    }
    try {
      await api.excluir(`/cidadaos/${sessao.id}/rotas/${rota.id}`);
      aoMudar("Rota excluída.");
    } catch (erro) {
      aoErro(erro.message);
    }
  }

  return (
    <div className="perfil-bloco">
      <h2 className="perfil-subtitulo">
        Rotas{" "}
        {estado.dados && (
          <span className="perfil-contador">
            {rotas.length}/{dominio.maximoRotas}
          </span>
        )}
      </h2>
      {estado.carregando && !estado.dados && <Carregando />}
      {estado.erro && <ErroCarga erro={estado.erro} aoTentar={estado.recarregar} />}
      {estado.dados && rotas.length === 0 && (
        <Vazio titulo="Nenhuma rota salva.">
          <a className="botao botao--secundario botao--pequeno" href="#/mobilidade/rota">
            Planejar rota
          </a>
        </Vazio>
      )}

      <ul className="rotas-salvas cartoes-perfil">
        {rotas.map((rota) => {
          const alerta = pior.get(rota.id);
          return (
            <li key={rota.id} className="rotas-salvas__item cartao-perfil">
              {editando === rota.id ? (
                <RenomearRota
                  rota={rota}
                  aoSalvar={() => {
                    setEditando(null);
                    aoMudar("Rota renomeada.");
                  }}
                  aoCancelar={() => setEditando(null)}
                />
              ) : (
                <>
                  <div className="rotas-salvas__cabeca">
                    <span className="rotas-salvas__nome">{rota.nome}</span>
                    {alerta ? (
                      <SinalRisco risco={alerta.risco} tamanho="pequeno" />
                    ) : (
                      <span className="rotas-salvas__livre">Livre</span>
                    )}
                  </div>
                  <p className="rotas-salvas__trajeto">
                    <span className="rotas-salvas__ponto rotas-salvas__ponto--a" aria-label="Saída">
                      A
                    </span>
                    {rota.origem.descricao}
                    <span className="rotas-salvas__ponto rotas-salvas__ponto--b" aria-label="Destino">
                      B
                    </span>
                    {rota.destino.descricao}
                  </p>
                  <p className="rotas-salvas__meta">
                    {formatarNumero(rota.distanciaMetros / 1000, 1)} km · cerca de{" "}
                    {formatarMinutos(minutosPara(rota.distanciaMetros, velocidade))}
                  </p>
                  <div className="acoes acoes--linha">
                    <button type="button" className="botao-texto" onClick={() => navegar(`/mobilidade/rota?id=${rota.id}`)}>
                      Abrir<span className="visualmente-oculto"> {rota.nome} no mapa</span>
                    </button>
                    <button type="button" className="botao-texto" onClick={() => setEditando(rota.id)}>
                      Renomear<span className="visualmente-oculto"> {rota.nome}</span>
                    </button>
                    <button type="button" className="botao-texto botao-texto--perigo" onClick={() => excluir(rota)}>
                      Excluir<span className="visualmente-oculto"> {rota.nome}</span>
                    </button>
                  </div>
                </>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function RenomearRota({ rota, aoSalvar, aoCancelar }) {
  const { sessao } = useApp();
  const form = useFormulario({ nome: rota.nome });
  const idCampo = `renomear-rota-${rota.id}`;

  async function salvar(evento) {
    evento.preventDefault();
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.texto("nome", valores.nome, "Nome da rota", 2, 60);
        return validador.erros;
      },
      (valores) =>
        api.atualizar(`/cidadaos/${sessao.id}/rotas/${rota.id}`, corpoDaRota(rota, { nome: valores.nome.trim() })),
    );
    if (ok) {
      aoSalvar();
    }
  }

  return (
    <form className="formulario formulario--linha" onSubmit={salvar} noValidate>
      <Campo id={idCampo} rotulo={`Novo nome para ${rota.nome}`} erro={form.erros.nome ?? form.aviso?.texto}>
        <input type="text" maxLength={60} value={form.valores.nome} onChange={form.alterar("nome")} />
      </Campo>
      <div className="acoes acoes--linha">
        <button type="submit" className="botao botao--primario botao--pequeno" disabled={form.enviando}>
          Salvar
        </button>
        <button type="button" className="botao botao--discreto botao--pequeno" onClick={aoCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
