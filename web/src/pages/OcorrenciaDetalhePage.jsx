import { useId, useState } from "react";
import { AlterarStatus, RegistrarAndamento } from "../components/AcoesGestor.jsx";
import Campo from "../components/Campo.jsx";
import { EtiquetaStatus, Gravidade } from "../components/Etiquetas.jsx";
import { Aviso, Carregando, ErroCarga } from "../components/Estados.jsx";
import PlacaVia from "../components/PlacaVia.jsx";
import SinalRisco from "../components/SinalRisco.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useFormulario } from "../hooks/useFormulario.js";
import { navegar } from "../hooks/useRota.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api } from "../services/api.js";
import { avisarNaProximaPagina, retirarAvisoPendente } from "../services/aviso.js";
import { formatarDataHora, formatarNumero, tempoRelativo } from "../utils/formato.js";
import { criarValidador } from "../utils/validacao.js";

export default function OcorrenciaDetalhePage({ parametros }) {
  const { id } = parametros;
  const { sessao, rotulo, dominio } = useApp();
  const detalhe = useCarregar(() => api.buscar(`/ocorrencias/${id}`), [id]);
  const [aviso, setAviso] = useState(retirarAvisoPendente);
  useTitulo(detalhe.dados?.titulo ?? "Ocorrência");

  if (detalhe.carregando && !detalhe.dados) {
    return <Carregando texto="Carregando a ocorrência…" />;
  }
  if (detalhe.erro) {
    return (
      <section className="secao">
        <a className="voltar" href="#/ocorrencias">
          Todas as ocorrências
        </a>
        <h1>Ocorrência indisponível</h1>
        <ErroCarga erro={detalhe.erro} aoTentar={detalhe.recarregar} />
      </section>
    );
  }

  const ocorrencia = detalhe.dados;
  const ehAutor = sessao?.tipo === "CIDADAO" && sessao.id === ocorrencia.autor.id;
  const ehGestor = sessao?.tipo === "GESTOR";
  const podeEditar = ehAutor && ocorrencia.status === "ABERTA";
  const podeExcluir =
    (ehAutor && ocorrencia.status === "ABERTA") || (ehGestor && ["ABERTA", "DESCARTADA"].includes(ocorrencia.status));
  const tipo = dominio.tiposOcorrencia.find((item) => item.codigo === ocorrencia.tipo);
  const modalDoUsuario = sessao?.tipo === "CIDADAO" ? sessao.modal : null;

  function concluir(texto) {
    setAviso({ tipo: "sucesso", texto });
    detalhe.recarregar();
  }

  async function excluir() {
    if (!window.confirm(`Excluir a ocorrência "${ocorrencia.titulo}"? Esta ação não pode ser desfeita.`)) {
      return;
    }
    try {
      await api.excluir(`/ocorrencias/${ocorrencia.id}`);
      avisarNaProximaPagina("sucesso", "Ocorrência excluída.");
      navegar("/ocorrencias");
    } catch (erro) {
      setAviso({ tipo: "erro", texto: erro.message });
    }
  }

  return (
    <>
      <a className="voltar" href="#/ocorrencias">
        Todas as ocorrências
      </a>

      <header className="cabeca-pagina cabeca-pagina--detalhe">
        <div>
          <p className="sobretitulo">
            {rotulo("tiposOcorrencia", ocorrencia.tipo)} · registrada {tempoRelativo(ocorrencia.dataRegistro)}
          </p>
          <h1>{ocorrencia.titulo}</h1>
          <div className="linha-etiquetas">
            <EtiquetaStatus status={ocorrencia.status} />
            <Gravidade valor={ocorrencia.gravidade} />
          </div>
        </div>
        {(podeEditar || podeExcluir) && (
          <div className="cabeca-pagina__acoes">
            {podeEditar && (
              <a className="botao botao--secundario" href={`#/ocorrencias/${ocorrencia.id}/editar`}>
                Editar relato
              </a>
            )}
            {podeExcluir && (
              <button type="button" className="botao botao--perigo" onClick={excluir}>
                Excluir ocorrência
              </button>
            )}
          </div>
        )}
      </header>

      <Aviso aviso={aviso} aoFechar={() => setAviso(null)} />

      <div className="detalhe">
        <div className="detalhe__principal">
          <section className="cartao" aria-labelledby="relato-titulo">
            <h2 id="relato-titulo">Relato</h2>
            <p className="relato">{ocorrencia.descricao}</p>
            <dl className="ficha">
              <div>
                <dt>Reportada por</dt>
                <dd>
                  {ocorrencia.autor.nome} ({rotulo("modais", ocorrencia.autor.modal).toLowerCase()})
                </dd>
              </div>
              <div>
                <dt>Registrada em</dt>
                <dd>{formatarDataHora(ocorrencia.dataRegistro)}</dd>
              </div>
              <div>
                <dt>Órgão responsável</dt>
                <dd>{rotulo("orgaos", ocorrencia.orgaoResponsavel)}</dd>
              </div>
            </dl>
            <h3 className="subtitulo">Vias atingidas</h3>
            <div className="lista-placas">
              {ocorrencia.vias.map((item) => (
                <PlacaVia key={item.via.id} via={item.via} impacto={item.nivelImpacto} />
              ))}
            </div>
          </section>

          {ehGestor && (
            <section className="cartao cartao--gestao" aria-labelledby="gestao-titulo">
              <h2 id="gestao-titulo">Atender esta ocorrência</h2>
              {ocorrencia.ativa ? (
                <>
                  <AlterarStatus
                    idOcorrencia={ocorrencia.id}
                    proximos={ocorrencia.proximosStatus}
                    aoConcluir={concluir}
                  />
                  <RegistrarAndamento idOcorrencia={ocorrencia.id} aoConcluir={concluir} />
                </>
              ) : (
                <p>Esta ocorrência foi encerrada e não recebe novas ações.</p>
              )}
            </section>
          )}

          <section className="cartao" aria-labelledby="historico-titulo">
            <h2 id="historico-titulo">Linha do tempo</h2>
            <ol className="linha-tempo">
              <li className="linha-tempo__item">
                <p className="linha-tempo__quando">{formatarDataHora(ocorrencia.dataRegistro)}</p>
                <p className="linha-tempo__quem">{ocorrencia.autor.nome} reportou a ocorrência</p>
              </li>
              {ocorrencia.acoes.map((acao) => (
                <ItemLinhaTempo
                  key={acao.id}
                  acao={acao}
                  podeAlterar={ehGestor && sessao.id === acao.gestor.id && !acao.statusNovo}
                  aoConcluir={concluir}
                  aoErro={(texto) => setAviso({ tipo: "erro", texto })}
                />
              ))}
            </ol>
          </section>
        </div>

        <aside className="cartao risco-modais" aria-labelledby="risco-titulo">
          <h2 id="risco-titulo">Risco para cada modal</h2>
          <p className="risco-modais__formula">
            Gravidade {ocorrencia.gravidade} × peso {formatarNumero(tipo?.peso, 1)} do tipo × exposição do modal
          </p>
          <ul className="risco-modais__lista">
            {ocorrencia.riscoPorModal.map((linha) => (
              <li
                key={linha.modal}
                className={`risco-modais__item ${linha.modal === modalDoUsuario ? "risco-modais__item--voce" : ""}`}
              >
                <SinalRisco risco={linha} tamanho="pequeno" />
                <span className="risco-modais__modal">
                  {rotulo("modais", linha.modal)}
                  {linha.modal === modalDoUsuario && <span className="risco-modais__voce">você</span>}
                </span>
                <span className="risco-modais__nivel">
                  {rotulo("niveisRisco", linha.nivel)}
                  {linha.alerta ? " · alerta" : ""}
                </span>
              </li>
            ))}
          </ul>
        </aside>
      </div>
    </>
  );
}

/** Uma ação no histórico. O gestor que registrou um andamento pode corrigi-lo ou removê-lo. */
function ItemLinhaTempo({ acao, podeAlterar, aoConcluir, aoErro }) {
  const { rotulo } = useApp();
  const [editando, setEditando] = useState(false);
  const form = useFormulario({ descricao: acao.descricao });
  const idCampo = useId();

  async function salvar(evento) {
    evento.preventDefault();
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.texto("descricao", valores.descricao, "Descrição", 5, 500);
        return validador.erros;
      },
      (valores) => api.atualizar(`/acoes/${acao.id}`, { descricao: valores.descricao }),
    );
    if (ok) {
      setEditando(false);
      aoConcluir("Andamento corrigido.");
    }
  }

  async function remover() {
    if (!window.confirm("Remover este andamento do histórico?")) {
      return;
    }
    try {
      await api.excluir(`/acoes/${acao.id}`);
      aoConcluir("Andamento removido.");
    } catch (erro) {
      aoErro(erro.message);
    }
  }

  return (
    <li className={`linha-tempo__item ${acao.statusNovo ? `linha-tempo__item--${acao.statusNovo}` : ""}`}>
      <p className="linha-tempo__quando">{formatarDataHora(acao.dataAcao)}</p>
      <p className="linha-tempo__quem">
        {acao.gestor.nome} · {rotulo("orgaos", acao.gestor.orgao)}
        {acao.statusNovo && (
          <>
            {" "}
            mudou para <EtiquetaStatus status={acao.statusNovo} />
          </>
        )}
      </p>
      {editando ? (
        <form className="formulario formulario--compacto" onSubmit={salvar} noValidate>
          <Aviso aviso={form.aviso} />
          <Campo id={idCampo} rotulo="Descrição do andamento" erro={form.erros.descricao}>
            <textarea rows={2} maxLength={500} value={form.valores.descricao} onChange={form.alterar("descricao")} />
          </Campo>
          <div className="acoes">
            <button type="submit" className="botao botao--primario botao--pequeno" disabled={form.enviando}>
              Salvar correção
            </button>
            <button
              type="button"
              className="botao botao--discreto botao--pequeno"
              onClick={() => {
                setEditando(false);
                form.reiniciar({ descricao: acao.descricao });
              }}
            >
              Cancelar
            </button>
          </div>
        </form>
      ) : (
        <p className="linha-tempo__texto">{acao.descricao}</p>
      )}
      {podeAlterar && !editando && (
        <div className="acoes acoes--linha">
          <button type="button" className="botao-texto" onClick={() => setEditando(true)}>
            Corrigir
          </button>
          <button type="button" className="botao-texto botao-texto--perigo" onClick={remover}>
            Remover
          </button>
        </div>
      )}
    </li>
  );
}
