import { useState } from "react";
import Campo from "../components/Campo.jsx";
import { EtiquetaStatus } from "../components/Etiquetas.jsx";
import { Aviso, Carregando, ErroCarga, ExigirPerfil, Vazio } from "../components/Estados.jsx";
import PlacaVia from "../components/PlacaVia.jsx";
import SinalRisco from "../components/SinalRisco.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useFormulario } from "../hooks/useFormulario.js";
import { navegar } from "../hooks/useRota.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api } from "../services/api.js";
import { avisarNaProximaPagina, retirarAvisoPendente } from "../services/aviso.js";
import { formatarMinutos, formatarNumero, tempoRelativo } from "../utils/formato.js";
import { MODAL_FRASE } from "../utils/rotulos.js";
import { corpoDaRota, minutosPara } from "../utils/rotas.js";
import { criarValidador } from "../utils/validacao.js";

const ORDEM_RISCO = { BAIXO: 1, MODERADO: 2, ALTO: 3, CRITICO: 4 };

export default function MinhasRotasPage() {
  useTitulo("Minhas rotas");
  return (
    <>
      <header className="cabeca-pagina">
        <div>
          <p className="sobretitulo">Para quem circula</p>
          <h1>Minhas rotas</h1>
          <p className="lead">
            Salve os caminhos que você faz no dia a dia. Quando surgir uma ocorrência em alguma via deles, o risco
            aparece aqui, calculado para o seu jeito de se locomover.
          </p>
        </div>
        <a className="botao botao--primario" href="#/rota">
          Planejar uma rota
        </a>
      </header>
      <ExigirPerfil tipo="CIDADAO" motivo="Entre com a sua conta de cidadão para salvar rotas e receber alertas.">
        <ConteudoRotas />
      </ExigirPerfil>
    </>
  );
}

function ConteudoRotas() {
  const { sessao } = useApp();
  const [aviso, setAviso] = useState(retirarAvisoPendente);
  const alertas = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/alertas`), [sessao.id, sessao.modal]);
  const rotas = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/rotas`), [sessao.id]);

  function aposMudarRotas(texto) {
    setAviso({ tipo: "sucesso", texto });
    rotas.recarregar();
    alertas.recarregar();
  }

  return (
    <>
      <Aviso aviso={aviso} aoFechar={() => setAviso(null)} />
      <div className="trajeto">
        <Alertas estado={alertas} temRotas={(rotas.dados ?? []).length > 0} />
        <RotasSalvas
          estado={rotas}
          alertas={alertas.dados ?? []}
          aoMudar={aposMudarRotas}
          aoErro={(texto) => setAviso({ tipo: "erro", texto })}
        />
        <Perfil aoAvisar={setAviso} />
      </div>
    </>
  );
}

function Alertas({ estado, temRotas }) {
  const { sessao, rotulo } = useApp();
  const frase = MODAL_FRASE[sessao.modal];
  const alertas = estado.dados ?? [];
  const importantes = alertas.filter((alerta) => alerta.risco.alerta);
  const outros = alertas.filter((alerta) => !alerta.risco.alerta);

  return (
    <section className="secao trajeto__alertas" aria-labelledby="alertas-titulo">
      <h2 id="alertas-titulo">Alertas nas suas rotas</h2>
      {estado.carregando && !estado.dados && <Carregando />}
      {estado.erro && <ErroCarga erro={estado.erro} aoTentar={estado.recarregar} />}
      {estado.dados && !temRotas && (
        <Vazio titulo="Você ainda não salvou nenhuma rota.">
          <p>
            <a href="#/rota">Planeje uma rota</a> com a sua saída e o seu destino e salve para receber alertas.
          </p>
        </Vazio>
      )}
      {estado.dados && temRotas && alertas.length === 0 && (
        <Vazio titulo="Caminho livre.">
          <p>Nenhuma ocorrência ativa nas suas rotas agora.</p>
        </Vazio>
      )}

      {importantes.map((alerta) => (
        <article key={alerta.ocorrencia.id} className={`alerta alerta--${alerta.risco.nivel}`}>
          <SinalRisco risco={alerta.risco} tamanho="grande" />
          <div className="alerta__corpo">
            <p className="alerta__nivel">
              Risco {rotulo("niveisRisco", alerta.risco.nivel).toLowerCase()} para quem {frase}
            </p>
            <h3 className="alerta__titulo">
              <a href={`#/ocorrencias/${alerta.ocorrencia.id}`}>{alerta.ocorrencia.titulo}</a>
            </h3>
            <p className="alerta__rotas">
              Na rota {alerta.rotas.map((rota) => rota.nome).join(", ")}
            </p>
            <div className="lista-placas">
              {alerta.viasAfetadas.map((via) => (
                <PlacaVia key={via.id} via={via} semRegiao />
              ))}
            </div>
            <p className="alerta__rodape">
              <EtiquetaStatus status={alerta.ocorrencia.status} /> reportada{" "}
              {tempoRelativo(alerta.ocorrencia.dataRegistro)}
            </p>
          </div>
        </article>
      ))}

      {outros.length > 0 && (
        <div className="alertas-baixos">
          <h3 className="subtitulo">Também nas suas rotas, com risco baixo para quem {frase}</h3>
          <ul>
            {outros.map((alerta) => (
              <li key={alerta.ocorrencia.id}>
                <SinalRisco risco={alerta.risco} tamanho="pequeno" />
                <a href={`#/ocorrencias/${alerta.ocorrencia.id}`}>{alerta.ocorrencia.titulo}</a>
                <span className="alertas-baixos__via">{alerta.rotas.map((rota) => rota.nome).join(", ")}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

/** Pior alerta de cada rota, para o resumo da lista. */
function piorPorRota(alertas) {
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
  const { sessao, dominio, rotulo } = useApp();
  const [editando, setEditando] = useState(null);
  const rotas = estado.dados ?? [];
  const pior = piorPorRota(alertas);
  const velocidade = dominio.modais.find((modal) => modal.codigo === sessao.modal)?.velocidadeMediaKmh ?? 20;

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
    <section className="secao cartao trajeto__vias" aria-labelledby="rotas-titulo">
      <h2 id="rotas-titulo">Rotas salvas</h2>
      {estado.carregando && !estado.dados && <Carregando />}
      {estado.erro && <ErroCarga erro={estado.erro} aoTentar={estado.recarregar} />}
      {estado.dados && rotas.length === 0 && <p className="texto-apoio">Nenhuma rota salva ainda.</p>}

      <ul className="rotas-salvas">
        {rotas.map((rota) => {
          const alerta = pior.get(rota.id);
          return (
            <li key={rota.id} className="rotas-salvas__item">
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
                    {formatarMinutos(minutosPara(rota.distanciaMetros, velocidade))} ·{" "}
                    {rotulo("criteriosRota", rota.criterio).toLowerCase()} · {rota.vias.length}{" "}
                    {rota.vias.length === 1 ? "via acompanhada" : "vias acompanhadas"}
                  </p>
                  <div className="acoes acoes--linha">
                    <button type="button" className="botao-texto" onClick={() => navegar(`/rota?id=${rota.id}`)}>
                      Abrir no mapa<span className="visualmente-oculto"> {rota.nome}</span>
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
    </section>
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

function Perfil({ aoAvisar }) {
  const { sessao, dominio, entrar, sair } = useApp();
  const form = useFormulario({ nome: sessao.nome, email: sessao.email, modal: sessao.modal, senha: "" });

  async function salvar(evento) {
    evento.preventDefault();
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.texto("nome", valores.nome, "Nome", 3, 100);
        validador.email("email", valores.email);
        validador.escolha("modal", valores.modal, "Escolha como você se locomove.");
        validador.senha("senha", valores.senha, false);
        return validador.erros;
      },
      async (valores) => {
        const atualizado = await api.atualizar(`/cidadaos/${sessao.id}`, {
          nome: valores.nome.trim(),
          email: valores.email.trim(),
          modal: valores.modal,
          senha: valores.senha || undefined,
        });
        entrar(atualizado);
      },
    );
    if (ok) {
      form.definir("senha", "");
      aoAvisar({ tipo: "sucesso", texto: "Seus dados foram atualizados. Os alertas já usam o novo modal." });
    }
  }

  async function excluirConta() {
    if (!window.confirm("Excluir a sua conta? Suas rotas salvas também serão apagadas.")) {
      return;
    }
    try {
      await api.excluir(`/cidadaos/${sessao.id}`);
      sair();
      avisarNaProximaPagina("sucesso", "Sua conta foi excluída.");
      navegar("/entrar");
    } catch (erro) {
      aoAvisar({ tipo: "erro", texto: erro.message });
    }
  }

  return (
    <section className="secao cartao trajeto__perfil" aria-labelledby="perfil-titulo">
      <h2 id="perfil-titulo">Seus dados</h2>
      <form className="formulario formulario--compacto" onSubmit={salvar} noValidate>
        <Aviso aviso={form.aviso} />
        <Campo id="perfil-nome" rotulo="Nome" erro={form.erros.nome}>
          <input type="text" autoComplete="name" maxLength={100} value={form.valores.nome} onChange={form.alterar("nome")} />
        </Campo>
        <Campo id="perfil-email" rotulo="E-mail" erro={form.erros.email}>
          <input type="email" autoComplete="email" maxLength={120} value={form.valores.email} onChange={form.alterar("email")} />
        </Campo>
        <Campo id="perfil-modal" rotulo="Como você se locomove" erro={form.erros.modal} dica="O risco dos alertas depende disto.">
          <select value={form.valores.modal} onChange={form.alterar("modal")}>
            {dominio.modais.map((modal) => (
              <option key={modal.codigo} value={modal.codigo}>
                {modal.rotulo}
              </option>
            ))}
          </select>
        </Campo>
        <Campo id="perfil-senha" rotulo="Nova senha" opcional erro={form.erros.senha} dica="Deixe em branco para manter a atual.">
          <input type="password" autoComplete="new-password" maxLength={64} value={form.valores.senha} onChange={form.alterar("senha")} />
        </Campo>
        <div className="acoes">
          <button type="submit" className="botao botao--primario" disabled={form.enviando}>
            {form.enviando ? "Salvando…" : "Salvar dados"}
          </button>
          <button type="button" className="botao botao--perigo" onClick={excluirConta}>
            Excluir minha conta
          </button>
        </div>
      </form>
    </section>
  );
}
