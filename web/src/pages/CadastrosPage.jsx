import { useMemo, useRef, useState } from "react";
import Campo from "../components/Campo.jsx";
import { Aviso, Carregando, ErroCarga, Vazio } from "../components/Estados.jsx";
import PlacaVia from "../components/PlacaVia.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useFormulario } from "../hooks/useFormulario.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api } from "../services/api.js";
import { formatarData } from "../utils/formato.js";
import { criarValidador } from "../utils/validacao.js";

const ABAS = [
  { id: "vias", rotulo: "Vias" },
  { id: "regioes", rotulo: "Regiões" },
  { id: "cidadaos", rotulo: "Cidadãos" },
  { id: "gestores", rotulo: "Gestores" },
];

export default function CadastrosPage() {
  useTitulo("Cadastros");
  const { sessao } = useApp();
  const [aba, setAba] = useState("vias");
  const referencias = useRef({});
  const podeEditar = sessao?.tipo === "GESTOR";

  // Setas esquerda e direita trocam de aba, como em qualquer grupo de abas acessível.
  function aoTeclar(evento, indice) {
    const deslocamento = { ArrowRight: 1, ArrowLeft: -1 }[evento.key];
    if (!deslocamento) {
      return;
    }
    evento.preventDefault();
    const proxima = ABAS[(indice + deslocamento + ABAS.length) % ABAS.length];
    setAba(proxima.id);
    referencias.current[proxima.id]?.focus();
  }

  return (
    <>
      <header className="cabeca-pagina">
        <div>
          <p className="sobretitulo">Base de dados</p>
          <h1>Cadastros</h1>
          <p className="lead">Vias, regiões e pessoas que usam o CitizenRoute.</p>
        </div>
      </header>

      {!podeEditar && (
        <Aviso
          aviso={{
            tipo: "info",
            texto: "Você está vendo os cadastros em modo de leitura. Entre como gestor para incluir, editar ou excluir.",
          }}
        />
      )}

      <div className="abas" role="tablist" aria-label="Tipo de cadastro">
        {ABAS.map((item, indice) => (
          <button
            key={item.id}
            ref={(elemento) => {
              referencias.current[item.id] = elemento;
            }}
            type="button"
            role="tab"
            id={`aba-${item.id}`}
            aria-selected={aba === item.id}
            aria-controls={`painel-${item.id}`}
            tabIndex={aba === item.id ? 0 : -1}
            className="aba"
            onClick={() => setAba(item.id)}
            onKeyDown={(evento) => aoTeclar(evento, indice)}
          >
            {item.rotulo}
          </button>
        ))}
      </div>

      <div role="tabpanel" id={`painel-${aba}`} aria-labelledby={`aba-${aba}`} className="painel-aba">
        {aba === "vias" && <AbaVias podeEditar={podeEditar} />}
        {aba === "regioes" && <AbaRegioes podeEditar={podeEditar} />}
        {aba === "cidadaos" && <AbaCidadaos podeEditar={podeEditar} />}
        {aba === "gestores" && <AbaGestores podeEditar={podeEditar} />}
      </div>
    </>
  );
}

/**
 * Estado comum dos cadastros: lista, formulário, item em edição e mensagens.
 * textos: { criado, atualizado, excluido }
 */
function useCadastro(caminho, valoresIniciais, textos) {
  const lista = useCarregar(() => api.buscar(caminho), [caminho]);
  const form = useFormulario(valoresIniciais);
  const [editando, setEditando] = useState(null);
  const [aviso, setAviso] = useState(null);

  function editar(item, valores) {
    setEditando(item);
    form.reiniciar(valores);
    setAviso(null);
    setTimeout(() => document.querySelector(".cadastro__formulario input, .cadastro__formulario select")?.focus(), 0);
  }

  function cancelar() {
    setEditando(null);
    form.reiniciar(valoresIniciais);
  }

  async function salvar(evento, validar, montarCorpo) {
    evento.preventDefault();
    const estavaEditando = editando;
    const ok = await form.enviar(
      (valores) => validar(valores, Boolean(estavaEditando)),
      (valores) =>
        estavaEditando
          ? api.atualizar(`${caminho}/${estavaEditando.id}`, montarCorpo(valores))
          : api.criar(caminho, montarCorpo(valores)),
    );
    if (ok) {
      setAviso({ tipo: "sucesso", texto: estavaEditando ? textos.atualizado : textos.criado });
      cancelar();
      lista.recarregar();
    }
  }

  async function excluir(item, nome) {
    if (!window.confirm(`Excluir ${nome}? Esta ação não pode ser desfeita.`)) {
      return;
    }
    try {
      await api.excluir(`${caminho}/${item.id}`);
      setAviso({ tipo: "sucesso", texto: textos.excluido });
      if (editando?.id === item.id) {
        cancelar();
      }
      lista.recarregar();
    } catch (erro) {
      setAviso({ tipo: "erro", texto: erro.message });
    }
  }

  return { lista, form, editando, aviso, setAviso, editar, cancelar, salvar, excluir };
}

function EstadoLista({ lista, vazio }) {
  if (lista.carregando && !lista.dados) {
    return <Carregando />;
  }
  if (lista.erro) {
    return <ErroCarga erro={lista.erro} aoTentar={lista.recarregar} />;
  }
  if (lista.dados?.length === 0) {
    return <Vazio titulo={vazio} />;
  }
  return null;
}

function BotoesLinha({ nome, aoEditar, aoExcluir }) {
  return (
    <div className="acoes acoes--linha">
      <button type="button" className="botao-texto" onClick={aoEditar}>
        Editar<span className="visualmente-oculto"> {nome}</span>
      </button>
      <button type="button" className="botao-texto botao-texto--perigo" onClick={aoExcluir}>
        Excluir<span className="visualmente-oculto"> {nome}</span>
      </button>
    </div>
  );
}

function RodapeFormulario({ cadastro, rotuloNovo }) {
  return (
    <div className="acoes">
      <button type="submit" className="botao botao--primario" disabled={cadastro.form.enviando}>
        {cadastro.form.enviando ? "Salvando…" : cadastro.editando ? "Salvar alterações" : rotuloNovo}
      </button>
      {cadastro.editando && (
        <button type="button" className="botao botao--discreto" onClick={cadastro.cancelar}>
          Cancelar edição
        </button>
      )}
    </div>
  );
}

// ------------------------------------------------------------------ Vias

function AbaVias({ podeEditar }) {
  const cadastro = useCadastro(
    "/vias",
    { nome: "", idRegiao: "" },
    { criado: "Via cadastrada.", atualizado: "Via atualizada.", excluido: "Via excluída." },
  );
  const regioes = useCarregar(() => api.buscar("/regioes"), []);
  const [busca, setBusca] = useState("");
  const [filtroRegiao, setFiltroRegiao] = useState("");
  const { form } = cadastro;

  const filtradas = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    return (cadastro.lista.dados ?? []).filter(
      (via) =>
        (!termo || via.nome.toLowerCase().includes(termo)) &&
        (!filtroRegiao || String(via.regiao.id) === filtroRegiao),
    );
  }, [cadastro.lista.dados, busca, filtroRegiao]);

  const validar = (valores) => {
    const validador = criarValidador();
    validador.texto("nome", valores.nome, "Nome da via", 3, 120);
    validador.escolha("idRegiao", valores.idRegiao, "Escolha a região da via.");
    return validador.erros;
  };

  return (
    <div className="cadastro">
      <section className="cadastro__lista" aria-labelledby="vias-lista-titulo">
        <h2 id="vias-lista-titulo" className="visualmente-oculto">
          Lista de vias
        </h2>
        <Aviso aviso={cadastro.aviso} aoFechar={() => cadastro.setAviso(null)} />
        <div className="filtros filtros--compactos">
          <div className="campo">
            <label className="campo__rotulo" htmlFor="vias-busca">
              Buscar pelo nome
            </label>
            <input id="vias-busca" type="search" value={busca} onChange={(e) => setBusca(e.target.value)} />
          </div>
          <div className="campo">
            <label className="campo__rotulo" htmlFor="vias-regiao">
              Região
            </label>
            <select id="vias-regiao" value={filtroRegiao} onChange={(e) => setFiltroRegiao(e.target.value)}>
              <option value="">Todas</option>
              {(regioes.dados ?? []).map((regiao) => (
                <option key={regiao.id} value={regiao.id}>
                  {regiao.nome}
                </option>
              ))}
            </select>
          </div>
        </div>
        <EstadoLista lista={cadastro.lista} vazio="Nenhuma via cadastrada." />
        {cadastro.lista.dados && (
          <div className="tabela-rolagem">
            <table className="tabela">
              <caption className="visualmente-oculto">Vias cadastradas</caption>
              <thead>
                <tr>
                  <th scope="col">Via</th>
                  <th scope="col">Região</th>
                  {podeEditar && <th scope="col">Ações</th>}
                </tr>
              </thead>
              <tbody>
                {filtradas.map((via) => (
                  <tr key={via.id} className={cadastro.editando?.id === via.id ? "tabela__linha--editando" : ""}>
                    <th scope="row">
                      <PlacaVia via={via} semRegiao />
                    </th>
                    <td>{via.regiao.nome}</td>
                    {podeEditar && (
                      <td>
                        <BotoesLinha
                          nome={via.nome}
                          aoEditar={() => cadastro.editar(via, { nome: via.nome, idRegiao: String(via.regiao.id) })}
                          aoExcluir={() => cadastro.excluir(via, `a via ${via.nome}`)}
                        />
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
            {filtradas.length === 0 && <p className="campo__dica">Nenhuma via com esse filtro.</p>}
          </div>
        )}
      </section>

      {podeEditar && (
        <section className="cartao cadastro__formulario" aria-labelledby="via-form-titulo">
          <h2 id="via-form-titulo">{cadastro.editando ? "Editar via" : "Nova via"}</h2>
          <form
            className="formulario"
            noValidate
            onSubmit={(e) =>
              cadastro.salvar(e, validar, (valores) => ({ nome: valores.nome.trim(), idRegiao: Number(valores.idRegiao) }))
            }
          >
            <Aviso aviso={form.aviso} />
            <Campo id="via-nome" rotulo="Nome da via" erro={form.erros.nome}>
              <input
                type="text"
                maxLength={120}
                placeholder="Ex.: Avenida Paulista"
                value={form.valores.nome}
                onChange={form.alterar("nome")}
              />
            </Campo>
            <Campo id="via-regiao" rotulo="Região" erro={form.erros.idRegiao}>
              <select value={form.valores.idRegiao} onChange={form.alterar("idRegiao")}>
                <option value="">Escolha a região</option>
                {(regioes.dados ?? []).map((regiao) => (
                  <option key={regiao.id} value={regiao.id}>
                    {regiao.nome}
                  </option>
                ))}
              </select>
            </Campo>
            <RodapeFormulario cadastro={cadastro} rotuloNovo="Cadastrar via" />
          </form>
        </section>
      )}
    </div>
  );
}

// ------------------------------------------------------------------ Regiões

function AbaRegioes({ podeEditar }) {
  const cadastro = useCadastro(
    "/regioes",
    { nome: "" },
    { criado: "Região cadastrada.", atualizado: "Região atualizada.", excluido: "Região excluída." },
  );
  const { form } = cadastro;

  const validar = (valores) => {
    const validador = criarValidador();
    validador.texto("nome", valores.nome, "Nome da região", 2, 60);
    return validador.erros;
  };

  return (
    <div className="cadastro">
      <section className="cadastro__lista" aria-labelledby="regioes-lista-titulo">
        <h2 id="regioes-lista-titulo" className="visualmente-oculto">
          Lista de regiões
        </h2>
        <Aviso aviso={cadastro.aviso} aoFechar={() => cadastro.setAviso(null)} />
        <EstadoLista lista={cadastro.lista} vazio="Nenhuma região cadastrada." />
        {cadastro.lista.dados?.length > 0 && (
          <div className="tabela-rolagem">
            <table className="tabela">
              <caption className="visualmente-oculto">Regiões cadastradas</caption>
              <thead>
                <tr>
                  <th scope="col">Região</th>
                  {podeEditar && <th scope="col">Ações</th>}
                </tr>
              </thead>
              <tbody>
                {cadastro.lista.dados.map((regiao) => (
                  <tr key={regiao.id} className={cadastro.editando?.id === regiao.id ? "tabela__linha--editando" : ""}>
                    <th scope="row">{regiao.nome}</th>
                    {podeEditar && (
                      <td>
                        <BotoesLinha
                          nome={regiao.nome}
                          aoEditar={() => cadastro.editar(regiao, { nome: regiao.nome })}
                          aoExcluir={() => cadastro.excluir(regiao, `a região ${regiao.nome}`)}
                        />
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {podeEditar && (
        <section className="cartao cadastro__formulario" aria-labelledby="regiao-form-titulo">
          <h2 id="regiao-form-titulo">{cadastro.editando ? "Editar região" : "Nova região"}</h2>
          <form
            className="formulario"
            noValidate
            onSubmit={(e) => cadastro.salvar(e, validar, (valores) => ({ nome: valores.nome.trim() }))}
          >
            <Aviso aviso={form.aviso} />
            <Campo id="regiao-nome" rotulo="Nome da região" erro={form.erros.nome}>
              <input
                type="text"
                maxLength={60}
                placeholder="Ex.: Zona Sul"
                value={form.valores.nome}
                onChange={form.alterar("nome")}
              />
            </Campo>
            <RodapeFormulario cadastro={cadastro} rotuloNovo="Cadastrar região" />
          </form>
        </section>
      )}
    </div>
  );
}

// ------------------------------------------------------------------ Cidadãos

function AbaCidadaos({ podeEditar }) {
  const { dominio, rotulo } = useApp();
  const cadastro = useCadastro(
    "/cidadaos",
    { nome: "", email: "", modal: "", senha: "" },
    { criado: "Cidadão cadastrado.", atualizado: "Cadastro do cidadão atualizado.", excluido: "Cidadão excluído." },
  );
  const { form } = cadastro;

  const validar = (valores, editando) => {
    const validador = criarValidador();
    validador.texto("nome", valores.nome, "Nome", 3, 100);
    validador.email("email", valores.email);
    validador.escolha("modal", valores.modal, "Escolha o modal.");
    validador.senha("senha", valores.senha, !editando);
    return validador.erros;
  };

  return (
    <div className="cadastro">
      <section className="cadastro__lista" aria-labelledby="cidadaos-lista-titulo">
        <h2 id="cidadaos-lista-titulo" className="visualmente-oculto">
          Lista de cidadãos
        </h2>
        <Aviso aviso={cadastro.aviso} aoFechar={() => cadastro.setAviso(null)} />
        <EstadoLista lista={cadastro.lista} vazio="Nenhum cidadão cadastrado." />
        {cadastro.lista.dados?.length > 0 && (
          <div className="tabela-rolagem">
            <table className="tabela">
              <caption className="visualmente-oculto">Cidadãos cadastrados</caption>
              <thead>
                <tr>
                  <th scope="col">Nome</th>
                  <th scope="col">E-mail</th>
                  <th scope="col">Modal</th>
                  <th scope="col">Desde</th>
                  {podeEditar && <th scope="col">Ações</th>}
                </tr>
              </thead>
              <tbody>
                {cadastro.lista.dados.map((cidadao) => (
                  <tr key={cidadao.id} className={cadastro.editando?.id === cidadao.id ? "tabela__linha--editando" : ""}>
                    <th scope="row">{cidadao.nome}</th>
                    <td>{cidadao.email}</td>
                    <td>{rotulo("modais", cidadao.modal)}</td>
                    <td>{formatarData(cidadao.dataCadastro)}</td>
                    {podeEditar && (
                      <td>
                        <BotoesLinha
                          nome={cidadao.nome}
                          aoEditar={() =>
                            cadastro.editar(cidadao, {
                              nome: cidadao.nome,
                              email: cidadao.email,
                              modal: cidadao.modal,
                              senha: "",
                            })
                          }
                          aoExcluir={() => cadastro.excluir(cidadao, `o cadastro de ${cidadao.nome}`)}
                        />
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {podeEditar && (
        <section className="cartao cadastro__formulario" aria-labelledby="cidadao-form-titulo">
          <h2 id="cidadao-form-titulo">{cadastro.editando ? "Editar cidadão" : "Novo cidadão"}</h2>
          <form
            className="formulario"
            noValidate
            onSubmit={(e) =>
              cadastro.salvar(e, validar, (valores) => ({
                nome: valores.nome.trim(),
                email: valores.email.trim(),
                modal: valores.modal,
                senha: valores.senha || undefined,
              }))
            }
          >
            <Aviso aviso={form.aviso} />
            <Campo id="cidadao-nome" rotulo="Nome" erro={form.erros.nome}>
              <input type="text" maxLength={100} value={form.valores.nome} onChange={form.alterar("nome")} />
            </Campo>
            <Campo id="cidadao-email" rotulo="E-mail" erro={form.erros.email}>
              <input type="email" maxLength={120} value={form.valores.email} onChange={form.alterar("email")} />
            </Campo>
            <Campo id="cidadao-modal" rotulo="Modal" erro={form.erros.modal}>
              <select value={form.valores.modal} onChange={form.alterar("modal")}>
                <option value="">Escolha o modal</option>
                {dominio.modais.map((modal) => (
                  <option key={modal.codigo} value={modal.codigo}>
                    {modal.rotulo}
                  </option>
                ))}
              </select>
            </Campo>
            <Campo
              id="cidadao-senha"
              rotulo={cadastro.editando ? "Nova senha" : "Senha inicial"}
              opcional={Boolean(cadastro.editando)}
              erro={form.erros.senha}
              dica={cadastro.editando ? "Deixe em branco para manter a atual." : "Pelo menos 8 caracteres, com letras e números."}
            >
              <input type="password" autoComplete="new-password" maxLength={64} value={form.valores.senha} onChange={form.alterar("senha")} />
            </Campo>
            <RodapeFormulario cadastro={cadastro} rotuloNovo="Cadastrar cidadão" />
          </form>
        </section>
      )}
    </div>
  );
}

// ------------------------------------------------------------------ Gestores

function AbaGestores({ podeEditar }) {
  const { dominio, rotulo, sessao } = useApp();
  const cadastro = useCadastro(
    "/gestores",
    { nome: "", email: "", orgao: "", matricula: "", senha: "" },
    { criado: "Gestor cadastrado.", atualizado: "Cadastro do gestor atualizado.", excluido: "Gestor excluído." },
  );
  const { form } = cadastro;

  const validar = (valores, editando) => {
    const validador = criarValidador();
    validador.texto("nome", valores.nome, "Nome", 3, 100);
    validador.email("email", valores.email);
    validador.escolha("orgao", valores.orgao, "Escolha o órgão.");
    validador.texto("matricula", valores.matricula, "Matrícula", 3, 20);
    validador.senha("senha", valores.senha, !editando);
    return validador.erros;
  };

  return (
    <div className="cadastro">
      <section className="cadastro__lista" aria-labelledby="gestores-lista-titulo">
        <h2 id="gestores-lista-titulo" className="visualmente-oculto">
          Lista de gestores
        </h2>
        <Aviso aviso={cadastro.aviso} aoFechar={() => cadastro.setAviso(null)} />
        <EstadoLista lista={cadastro.lista} vazio="Nenhum gestor cadastrado." />
        {cadastro.lista.dados?.length > 0 && (
          <div className="tabela-rolagem">
            <table className="tabela">
              <caption className="visualmente-oculto">Gestores cadastrados</caption>
              <thead>
                <tr>
                  <th scope="col">Nome</th>
                  <th scope="col">Órgão</th>
                  <th scope="col">Matrícula</th>
                  <th scope="col">E-mail</th>
                  {podeEditar && <th scope="col">Ações</th>}
                </tr>
              </thead>
              <tbody>
                {cadastro.lista.dados.map((gestor) => (
                  <tr key={gestor.id} className={cadastro.editando?.id === gestor.id ? "tabela__linha--editando" : ""}>
                    <th scope="row">
                      {gestor.nome}
                      {gestor.id === sessao?.id && <span className="selo"> você</span>}
                    </th>
                    <td>{rotulo("orgaos", gestor.orgao)}</td>
                    <td className="numero">{gestor.matricula}</td>
                    <td>{gestor.email}</td>
                    {podeEditar && (
                      <td>
                        <BotoesLinha
                          nome={gestor.nome}
                          aoEditar={() =>
                            cadastro.editar(gestor, {
                              nome: gestor.nome,
                              email: gestor.email,
                              orgao: gestor.orgao,
                              matricula: gestor.matricula,
                              senha: "",
                            })
                          }
                          aoExcluir={() => cadastro.excluir(gestor, `o cadastro de ${gestor.nome}`)}
                        />
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {podeEditar && (
        <section className="cartao cadastro__formulario" aria-labelledby="gestor-form-titulo">
          <h2 id="gestor-form-titulo">{cadastro.editando ? "Editar gestor" : "Novo gestor"}</h2>
          <form
            className="formulario"
            noValidate
            onSubmit={(e) =>
              cadastro.salvar(e, validar, (valores) => ({
                nome: valores.nome.trim(),
                email: valores.email.trim(),
                orgao: valores.orgao,
                matricula: valores.matricula.trim(),
                senha: valores.senha || undefined,
              }))
            }
          >
            <Aviso aviso={form.aviso} />
            <Campo id="gestor-nome" rotulo="Nome" erro={form.erros.nome}>
              <input type="text" maxLength={100} value={form.valores.nome} onChange={form.alterar("nome")} />
            </Campo>
            <Campo id="gestor-email" rotulo="E-mail" erro={form.erros.email}>
              <input type="email" maxLength={120} value={form.valores.email} onChange={form.alterar("email")} />
            </Campo>
            <Campo id="gestor-orgao" rotulo="Órgão" erro={form.erros.orgao}>
              <select value={form.valores.orgao} onChange={form.alterar("orgao")}>
                <option value="">Escolha o órgão</option>
                {dominio.orgaos.map((orgao) => (
                  <option key={orgao.codigo} value={orgao.codigo}>
                    {orgao.rotulo}
                  </option>
                ))}
              </select>
            </Campo>
            <Campo id="gestor-matricula" rotulo="Matrícula" erro={form.erros.matricula} dica="Ex.: CET-10234">
              <input type="text" maxLength={20} value={form.valores.matricula} onChange={form.alterar("matricula")} />
            </Campo>
            <Campo
              id="gestor-senha"
              rotulo={cadastro.editando ? "Nova senha" : "Senha inicial"}
              opcional={Boolean(cadastro.editando)}
              erro={form.erros.senha}
              dica={cadastro.editando ? "Deixe em branco para manter a atual." : "Pelo menos 8 caracteres, com letras e números."}
            >
              <input type="password" autoComplete="new-password" maxLength={64} value={form.valores.senha} onChange={form.alterar("senha")} />
            </Campo>
            <RodapeFormulario cadastro={cadastro} rotuloNovo="Cadastrar gestor" />
          </form>
        </section>
      )}
    </div>
  );
}
