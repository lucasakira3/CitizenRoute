import { useState } from "react";
import ItemOcorrencia from "../components/ItemOcorrencia.jsx";
import { EsqueletoLista } from "../components/Esqueleto.jsx";
import { Aviso, ErroCarga, Vazio } from "../components/Estados.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api, consulta } from "../services/api.js";
import { retirarAvisoPendente } from "../services/aviso.js";
import { MODAL_FRASE } from "../utils/rotulos.js";

const FILTROS_INICIAIS = { status: "", tipo: "", regiao: "", ativas: true };

export default function OcorrenciasPage() {
  useTitulo("Ocorrências");
  const { sessao, dominio } = useApp();
  const [aviso, setAviso] = useState(retirarAvisoPendente);
  const [filtros, setFiltros] = useState(FILTROS_INICIAIS);
  const modal = sessao?.tipo === "CIDADAO" ? sessao.modal : null;

  const regioes = useCarregar(() => api.buscar("/regioes"), []);
  const lista = useCarregar(
    () => api.buscar(`/ocorrencias${consulta({ ...filtros, modal })}`),
    [filtros.status, filtros.tipo, filtros.regiao, filtros.ativas, modal],
  );

  function alterarFiltro(campo, valor) {
    setFiltros((anteriores) => {
      const novos = { ...anteriores, [campo]: valor };
      // Escolher um status encerrado com "só ativas" marcado daria sempre lista vazia.
      if (campo === "status" && valor) {
        novos.ativas = false;
      }
      return novos;
    });
  }

  const filtrosAlterados = JSON.stringify(filtros) !== JSON.stringify(FILTROS_INICIAIS);

  return (
    <>
      <header className="cabeca-pagina">
        <div>
          <p className="sobretitulo">Relatos de quem circula</p>
          <h1>Ocorrências</h1>
          <p className="lead">
            Tudo o que os cidadãos reportaram nas vias e o andamento de cada caso na gestão pública.
            {modal && ` O losango mostra o risco para quem ${MODAL_FRASE[modal]}, como você.`}
          </p>
        </div>
        <div className="cabeca-pagina__acoes">
          {sessao?.tipo === "CIDADAO" && (
            <a className="botao botao--primario" href="#/ocorrencias/nova">
              Reportar ocorrência
            </a>
          )}
          {!sessao && (
            <a className="botao botao--secundario" href="#/entrar">
              Entrar para reportar
            </a>
          )}
          {sessao?.tipo === "GESTOR" && (
            <a className="botao botao--secundario" href="#/gestao">
              Abrir a fila da gestão
            </a>
          )}
        </div>
      </header>

      <Aviso aviso={aviso} aoFechar={() => setAviso(null)} />

      <form className="filtros" role="search" aria-label="Filtrar ocorrências" onSubmit={(e) => e.preventDefault()}>
        <div className="campo">
          <label className="campo__rotulo" htmlFor="filtro-status">
            Status
          </label>
          <select id="filtro-status" value={filtros.status} onChange={(e) => alterarFiltro("status", e.target.value)}>
            <option value="">Todos</option>
            {dominio.status.map((item) => (
              <option key={item.codigo} value={item.codigo}>
                {item.rotulo}
              </option>
            ))}
          </select>
        </div>
        <div className="campo">
          <label className="campo__rotulo" htmlFor="filtro-tipo">
            Tipo
          </label>
          <select id="filtro-tipo" value={filtros.tipo} onChange={(e) => alterarFiltro("tipo", e.target.value)}>
            <option value="">Todos</option>
            {dominio.tiposOcorrencia.map((item) => (
              <option key={item.codigo} value={item.codigo}>
                {item.rotulo}
              </option>
            ))}
          </select>
        </div>
        <div className="campo">
          <label className="campo__rotulo" htmlFor="filtro-regiao">
            Região
          </label>
          <select id="filtro-regiao" value={filtros.regiao} onChange={(e) => alterarFiltro("regiao", e.target.value)}>
            <option value="">Todas</option>
            {(regioes.dados ?? []).map((regiao) => (
              <option key={regiao.id} value={regiao.id}>
                {regiao.nome}
              </option>
            ))}
          </select>
        </div>
        <label className="caixa">
          <input type="checkbox" checked={filtros.ativas} onChange={(e) => alterarFiltro("ativas", e.target.checked)} />
          Só ativas
        </label>
        {filtrosAlterados && (
          <button type="button" className="botao botao--discreto botao--pequeno" onClick={() => setFiltros(FILTROS_INICIAIS)}>
            Limpar filtros
          </button>
        )}
      </form>

      <section className="secao" aria-labelledby="resultado-titulo">
        <h2 id="resultado-titulo" className="contagem" aria-live="polite">
          {lista.dados ? `${lista.dados.length} ${lista.dados.length === 1 ? "ocorrência" : "ocorrências"}` : "Ocorrências"}
        </h2>
        {lista.carregando && !lista.dados && <EsqueletoLista />}
        {lista.erro && <ErroCarga erro={lista.erro} aoTentar={lista.recarregar} />}
        {lista.dados?.length === 0 && (
          <Vazio titulo="Nenhuma ocorrência com esses filtros.">
            <p>Tente outro status ou desmarque &ldquo;Só ativas&rdquo; para ver o histórico.</p>
          </Vazio>
        )}
        <div className="lista-ocorrencias">
          {lista.dados?.map((ocorrencia, indice) => (
            <ItemOcorrencia key={ocorrencia.id} ocorrencia={ocorrencia} risco={ocorrencia.risco} indice={indice} />
          ))}
        </div>
      </section>
    </>
  );
}
