import { useState } from "react";
import { AlterarStatus } from "../components/AcoesGestor.jsx";
import { EsqueletoLista } from "../components/Esqueleto.jsx";
import { Aviso, ErroCarga, ExigirPerfil, Vazio } from "../components/Estados.jsx";
import ItemOcorrencia from "../components/ItemOcorrencia.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api, consulta } from "../services/api.js";
import { retirarAvisoPendente } from "../services/aviso.js";

// Na fila, primeiro o que ainda precisa de triagem; depois, os mais graves.
const PRIORIDADE = { ABERTA: 0, CONFIRMADA: 1, EM_ATENDIMENTO: 2, RESOLVIDA: 3, DESCARTADA: 4 };

export default function GestaoPage() {
  useTitulo("Gestão");
  return (
    <>
      <header className="cabeca-pagina">
        <div>
          <p className="sobretitulo">Para a gestão pública</p>
          <h1>Fila de ocorrências</h1>
          <p className="lead">
            Confirme o que chega dos cidadãos, registre o que está sendo feito e acompanhe até resolver. Cada mudança de
            status fica no histórico da ocorrência.
          </p>
        </div>
      </header>
      <ExigirPerfil tipo="GESTOR" motivo="Entre com uma conta de gestor público para atender ocorrências.">
        <Fila />
      </ExigirPerfil>
    </>
  );
}

function Fila() {
  const { sessao, dominio, rotulo } = useApp();
  const [soMeuOrgao, setSoMeuOrgao] = useState(true);
  const [situacao, setSituacao] = useState("ATIVAS");
  const [aviso, setAviso] = useState(retirarAvisoPendente);

  const lista = useCarregar(
    () =>
      api.buscar(
        `/ocorrencias${consulta({
          orgao: soMeuOrgao ? sessao.orgao : null,
          ativas: situacao === "ATIVAS",
          status: situacao !== "ATIVAS" && situacao !== "TODAS" ? situacao : null,
        })}`,
      ),
    [soMeuOrgao, situacao, sessao.orgao],
  );

  const proximos = (status) => dominio.status.find((item) => item.codigo === status)?.proximos ?? [];
  const ordenadas = [...(lista.dados ?? [])].sort(
    (a, b) => PRIORIDADE[a.status] - PRIORIDADE[b.status] || b.gravidade - a.gravidade,
  );
  const contagem = (status) => ordenadas.filter((ocorrencia) => ocorrencia.status === status).length;

  function concluir(texto) {
    setAviso({ tipo: "sucesso", texto });
    lista.recarregar();
  }

  return (
    <>
      <div className="filtros" role="group" aria-label="Filtrar a fila">
        <div className="campo">
          <label className="campo__rotulo" htmlFor="fila-situacao">
            Situação
          </label>
          <select id="fila-situacao" value={situacao} onChange={(e) => setSituacao(e.target.value)}>
            <option value="ATIVAS">Ativas</option>
            {dominio.status.map((item) => (
              <option key={item.codigo} value={item.codigo}>
                {item.rotulo}
              </option>
            ))}
            <option value="TODAS">Todas</option>
          </select>
        </div>
        <label className="caixa">
          <input type="checkbox" checked={soMeuOrgao} onChange={(e) => setSoMeuOrgao(e.target.checked)} />
          Só a fila de {rotulo("orgaos", sessao.orgao)}
        </label>
      </div>

      <Aviso aviso={aviso} aoFechar={() => setAviso(null)} />

      <section className="secao" aria-labelledby="fila-titulo">
        <h2 id="fila-titulo" className="contagem" aria-live="polite">
          {lista.dados
            ? `${ordenadas.length} na fila · ${contagem("ABERTA")} para confirmar · ${contagem("EM_ATENDIMENTO")} em atendimento`
            : "Fila"}
        </h2>
        {lista.carregando && !lista.dados && <EsqueletoLista />}
        {lista.erro && <ErroCarga erro={lista.erro} aoTentar={lista.recarregar} />}
        {lista.dados?.length === 0 && (
          <Vazio titulo="Fila vazia.">
            <p>
              {soMeuOrgao
                ? `Nenhuma ocorrência para ${rotulo("orgaos", sessao.orgao)} nesta situação. Desmarque o filtro para ver as de outros órgãos.`
                : "Nenhuma ocorrência nesta situação."}
            </p>
          </Vazio>
        )}
        <div className="lista-ocorrencias">
          {ordenadas.map((ocorrencia, indice) => (
            <ItemOcorrencia key={ocorrencia.id} ocorrencia={ocorrencia} indice={indice}>
              <div className="fila__acoes">
                <AlterarStatus
                  idOcorrencia={ocorrencia.id}
                  proximos={proximos(ocorrencia.status)}
                  aoConcluir={concluir}
                />
                <a className="link-seta" href={`#/ocorrencias/${ocorrencia.id}`}>
                  Histórico e andamentos
                </a>
              </div>
            </ItemOcorrencia>
          ))}
        </div>
      </section>
    </>
  );
}
