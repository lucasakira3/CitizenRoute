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
import { tempoRelativo } from "../utils/formato.js";
import { MODAL_FRASE } from "../utils/rotulos.js";
import { criarValidador } from "../utils/validacao.js";

export default function MeuTrajetoPage() {
  useTitulo("Meu trajeto");
  return (
    <>
      <header className="cabeca-pagina">
        <div>
          <p className="sobretitulo">Para quem circula</p>
          <h1>Meu trajeto</h1>
          <p className="lead">
            Salve as vias por onde você passa. Quando surgir uma ocorrência nelas, o risco aparece aqui, calculado para o
            seu jeito de se locomover.
          </p>
        </div>
      </header>
      <ExigirPerfil tipo="CIDADAO" motivo="Entre com a sua conta de cidadão para salvar vias e receber alertas.">
        <ConteudoTrajeto />
      </ExigirPerfil>
    </>
  );
}

function ConteudoTrajeto() {
  const { sessao } = useApp();
  const [aviso, setAviso] = useState(retirarAvisoPendente);
  const alertas = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/alertas`), [sessao.id, sessao.modal]);
  const vias = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/vias-monitoradas`), [sessao.id]);

  function aposMudarVias(texto) {
    setAviso({ tipo: "sucesso", texto });
    vias.recarregar();
    alertas.recarregar();
  }

  return (
    <>
      <Aviso aviso={aviso} aoFechar={() => setAviso(null)} />
      <div className="trajeto">
        <Alertas estado={alertas} temVias={(vias.dados ?? []).length > 0} />
        <ViasMonitoradas estado={vias} aoMudar={aposMudarVias} aoErro={(texto) => setAviso({ tipo: "erro", texto })} />
        <Perfil aoAvisar={setAviso} />
      </div>
    </>
  );
}

function Alertas({ estado, temVias }) {
  const { sessao, rotulo } = useApp();
  const frase = MODAL_FRASE[sessao.modal];
  const alertas = estado.dados ?? [];
  const importantes = alertas.filter((alerta) => alerta.risco.alerta);
  const outros = alertas.filter((alerta) => !alerta.risco.alerta);

  return (
    <section className="secao trajeto__alertas" aria-labelledby="alertas-titulo">
      <h2 id="alertas-titulo">Alertas nas suas vias</h2>
      {estado.carregando && !estado.dados && <Carregando />}
      {estado.erro && <ErroCarga erro={estado.erro} aoTentar={estado.recarregar} />}
      {estado.dados && !temVias && (
        <Vazio titulo="Você ainda não salvou nenhuma via.">
          <p>Adicione as vias do seu caminho em &ldquo;Vias monitoradas&rdquo; para receber alertas.</p>
        </Vazio>
      )}
      {estado.dados && temVias && alertas.length === 0 && (
        <Vazio titulo="Caminho livre.">
          <p>Nenhuma ocorrência ativa nas suas vias agora.</p>
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
            <div className="lista-placas">
              {alerta.viasAfetadas.map((vm) => (
                <PlacaVia key={vm.via.id} via={vm.via} apelido={vm.apelido} semRegiao />
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
          <h3 className="subtitulo">Também nas suas vias, com risco baixo para quem {frase}</h3>
          <ul>
            {outros.map((alerta) => (
              <li key={alerta.ocorrencia.id}>
                <SinalRisco risco={alerta.risco} tamanho="pequeno" />
                <a href={`#/ocorrencias/${alerta.ocorrencia.id}`}>{alerta.ocorrencia.titulo}</a>
                <span className="alertas-baixos__via">{alerta.viasAfetadas.map((vm) => vm.apelido).join(", ")}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function ViasMonitoradas({ estado, aoMudar, aoErro }) {
  const { sessao } = useApp();
  const todasVias = useCarregar(() => api.buscar("/vias"), []);
  const form = useFormulario({ idVia: "", apelido: "" });
  const [editando, setEditando] = useState(null);
  const monitoradas = estado.dados ?? [];
  const idsMonitorados = new Set(monitoradas.map((vm) => vm.via.id));
  const disponiveis = (todasVias.dados ?? []).filter((via) => !idsMonitorados.has(via.id));

  async function adicionar(evento) {
    evento.preventDefault();
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.escolha("idVia", valores.idVia, "Escolha a via que você quer monitorar.");
        validador.texto("apelido", valores.apelido, "Apelido", 2, 60);
        return validador.erros;
      },
      (valores) =>
        api.criar(`/cidadaos/${sessao.id}/vias-monitoradas`, {
          idVia: Number(valores.idVia),
          apelido: valores.apelido.trim(),
        }),
    );
    if (ok) {
      form.reiniciar();
      aoMudar("Via adicionada ao seu trajeto.");
    }
  }

  async function remover(vm) {
    if (!window.confirm(`Parar de monitorar ${vm.via.nome}?`)) {
      return;
    }
    try {
      await api.excluir(`/cidadaos/${sessao.id}/vias-monitoradas/${vm.via.id}`);
      aoMudar("Via removida do seu trajeto.");
    } catch (erro) {
      aoErro(erro.message);
    }
  }

  return (
    <section className="secao cartao trajeto__vias" aria-labelledby="vias-titulo">
      <h2 id="vias-titulo">Vias monitoradas</h2>
      {estado.carregando && !estado.dados && <Carregando />}
      {estado.erro && <ErroCarga erro={estado.erro} aoTentar={estado.recarregar} />}

      <ul className="vias-monitoradas">
        {monitoradas.map((vm) => (
          <li key={vm.via.id} className="vias-monitoradas__item">
            {editando === vm.via.id ? (
              <RenomearVia
                vm={vm}
                aoSalvar={() => {
                  setEditando(null);
                  aoMudar("Apelido atualizado.");
                }}
                aoCancelar={() => setEditando(null)}
              />
            ) : (
              <>
                <div className="vias-monitoradas__info">
                  <span className="vias-monitoradas__apelido">{vm.apelido}</span>
                  <PlacaVia via={vm.via} />
                </div>
                <div className="acoes acoes--linha">
                  <button type="button" className="botao-texto" onClick={() => setEditando(vm.via.id)}>
                    Renomear<span className="visualmente-oculto"> {vm.apelido}</span>
                  </button>
                  <button type="button" className="botao-texto botao-texto--perigo" onClick={() => remover(vm)}>
                    Remover<span className="visualmente-oculto"> {vm.apelido}</span>
                  </button>
                </div>
              </>
            )}
          </li>
        ))}
      </ul>

      <form className="formulario formulario--compacto" onSubmit={adicionar} noValidate>
        <h3 className="subtitulo">Adicionar via</h3>
        <Aviso aviso={form.aviso} />
        <Campo id="vm-via" rotulo="Via" erro={form.erros.idVia}>
          <select value={form.valores.idVia} onChange={form.alterar("idVia")}>
            <option value="">Escolha a via</option>
            {disponiveis.map((via) => (
              <option key={via.id} value={via.id}>
                {via.nome} · {via.regiao.nome}
              </option>
            ))}
          </select>
        </Campo>
        <Campo id="vm-apelido" rotulo="Apelido" erro={form.erros.apelido} dica="Como você chama esse trecho.">
          <input
            type="text"
            maxLength={60}
            placeholder="Ex.: Caminho do trabalho"
            value={form.valores.apelido}
            onChange={form.alterar("apelido")}
          />
        </Campo>
        <button type="submit" className="botao botao--primario" disabled={form.enviando}>
          {form.enviando ? "Salvando…" : "Monitorar via"}
        </button>
      </form>
    </section>
  );
}

function RenomearVia({ vm, aoSalvar, aoCancelar }) {
  const { sessao } = useApp();
  const form = useFormulario({ apelido: vm.apelido });
  const idCampo = `renomear-${vm.via.id}`;

  async function salvar(evento) {
    evento.preventDefault();
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.texto("apelido", valores.apelido, "Apelido", 2, 60);
        return validador.erros;
      },
      (valores) =>
        api.atualizar(`/cidadaos/${sessao.id}/vias-monitoradas/${vm.via.id}`, { apelido: valores.apelido.trim() }),
    );
    if (ok) {
      aoSalvar();
    }
  }

  return (
    <form className="formulario formulario--linha" onSubmit={salvar} noValidate>
      <Campo id={idCampo} rotulo={`Novo apelido para ${vm.via.nome}`} erro={form.erros.apelido ?? form.aviso?.texto}>
        <input type="text" maxLength={60} value={form.valores.apelido} onChange={form.alterar("apelido")} />
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
    if (!window.confirm("Excluir a sua conta? Suas vias monitoradas também serão apagadas.")) {
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
