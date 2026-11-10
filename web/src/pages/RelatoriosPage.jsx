import { Carregando, ErroCarga, Vazio } from "../components/Estados.jsx";
import PlacaVia from "../components/PlacaVia.jsx";
import SinalRisco from "../components/SinalRisco.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api } from "../services/api.js";
import { formatarMinutos, formatarNumero } from "../utils/formato.js";

export default function RelatoriosPage() {
  useTitulo("Relatórios");
  return (
    <>
      <header className="cabeca-pagina">
        <div>
          <p className="sobretitulo">Para quem planeja a cidade</p>
          <h1>Relatórios</h1>
          <p className="lead">
            Onde os problemas se repetem, quanto a gestão demora para responder e quem fica mais exposto. Os números são
            calculados no banco a partir de todas as ocorrências, exceto as descartadas.
          </p>
        </div>
      </header>
      <div className="relatorios">
        <ViasCriticas />
        <PorRegiao />
        <TempoResposta />
        <ExposicaoModal />
      </div>
    </>
  );
}

/** Moldura comum: título, explicação curta e os estados de carregamento e erro. */
function Relatorio({ id, titulo, descricao, estado, children, className = "" }) {
  return (
    <section className={`cartao relatorio ${className}`} aria-labelledby={id}>
      <h2 id={id}>{titulo}</h2>
      <p className="relatorio__descricao">{descricao}</p>
      {estado.carregando && !estado.dados && <Carregando />}
      {estado.erro && <ErroCarga erro={estado.erro} aoTentar={estado.recarregar} />}
      {estado.dados && estado.dados.length === 0 && <Vazio titulo="Ainda não há dados para este relatório." />}
      {estado.dados && estado.dados.length > 0 && children(estado.dados)}
    </section>
  );
}

function plural(quantidade, singular, pluralTexto) {
  return `${quantidade} ${quantidade === 1 ? singular : pluralTexto}`;
}

/** Barra horizontal proporcional ao maior valor. É decorativa: o número aparece ao lado. */
function Barra({ valor, maximo }) {
  const largura = maximo > 0 ? Math.max(4, (valor / maximo) * 100) : 0;
  return (
    <span className="barra" aria-hidden="true">
      <span className="barra__preenchimento" style={{ width: `${largura}%` }} />
    </span>
  );
}

function ViasCriticas() {
  const estado = useCarregar(() => api.buscar("/relatorios/vias-criticas"), []);
  return (
    <Relatorio
      id="rel-vias"
      titulo="Vias mais críticas"
      descricao="As dez vias com mais ocorrências ativas e, no empate, com mais ocorrências no total."
      estado={estado}
      className="relatorio--largo"
    >
      {(linhas) => {
        const maximo = Math.max(...linhas.map((linha) => linha.totalOcorrencias));
        return (
          <div className="tabela-rolagem">
            <table className="tabela">
              <thead>
                <tr>
                  <th scope="col">Via</th>
                  <th scope="col" className="numero">Ativas</th>
                  <th scope="col">Total de ocorrências</th>
                  <th scope="col" className="numero">Gravidade média</th>
                  <th scope="col" className="numero">Máxima</th>
                </tr>
              </thead>
              <tbody>
                {linhas.map((linha) => (
                  <tr key={linha.idVia}>
                    <th scope="row">
                      <PlacaVia via={{ id: linha.idVia, nome: linha.via, regiao: { nome: linha.regiao } }} />
                    </th>
                    <td className="numero numero--destaque">{linha.ocorrenciasAtivas}</td>
                    <td>
                      <span className="celula-barra">
                        <Barra valor={linha.totalOcorrencias} maximo={maximo} />
                        <span className="numero">{linha.totalOcorrencias}</span>
                      </span>
                    </td>
                    <td className="numero">{formatarNumero(linha.gravidadeMedia, 1)}</td>
                    <td className="numero">{linha.gravidadeMaxima}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
      }}
    </Relatorio>
  );
}

function TempoResposta() {
  const { rotulo } = useApp();
  const estado = useCarregar(() => api.buscar("/relatorios/tempo-resposta"), []);
  return (
    <Relatorio
      id="rel-tempo"
      titulo="Tempo até a primeira resposta"
      descricao="Média de minutos entre o relato do cidadão e a primeira ação de cada órgão."
      estado={estado}
    >
      {(linhas) => {
        const maximo = Math.max(...linhas.map((linha) => linha.minutosMedios));
        return (
          <ul className="barras">
            {linhas.map((linha) => (
              <li key={linha.orgao} className="barras__item">
                <span className="barras__rotulo">{rotulo("orgaos", linha.orgao)}</span>
                <Barra valor={linha.minutosMedios} maximo={maximo} />
                <span className="barras__valor">{formatarMinutos(linha.minutosMedios)}</span>
                <span className="barras__detalhe">
                  {plural(linha.ocorrenciasAtendidas, "ocorrência atendida", "ocorrências atendidas")} ·{" "}
                  {plural(linha.resolvidas, "resolvida", "resolvidas")}
                </span>
              </li>
            ))}
          </ul>
        );
      }}
    </Relatorio>
  );
}

function PorRegiao() {
  const { rotulo } = useApp();
  const estado = useCarregar(() => api.buscar("/relatorios/ocorrencias-por-regiao"), []);
  return (
    <Relatorio
      id="rel-regiao"
      titulo="Ocorrências por região e tipo"
      descricao="Quanto mais escura a célula, mais ocorrências daquele tipo na região. Entre parênteses, as ainda ativas."
      estado={estado}
      className="relatorio--largo"
    >
      {(linhas) => {
        const regioes = [...new Set(linhas.map((linha) => linha.regiao))];
        const tipos = [...new Set(linhas.map((linha) => linha.tipo))];
        const celula = (regiao, tipo) => linhas.find((linha) => linha.regiao === regiao && linha.tipo === tipo);
        const maximo = Math.max(...linhas.map((linha) => linha.total));
        return (
          <div className="tabela-rolagem">
            <table className="tabela tabela--matriz">
              <thead>
                <tr>
                  <th scope="col">Região</th>
                  {tipos.map((tipo) => (
                    <th key={tipo} scope="col">
                      {rotulo("tiposOcorrencia", tipo)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {regioes.map((regiao) => (
                  <tr key={regiao}>
                    <th scope="row">{regiao}</th>
                    {tipos.map((tipo) => {
                      const dado = celula(regiao, tipo);
                      return (
                        <td
                          key={tipo}
                          className={`matriz__celula ${dado && dado.total / maximo > 0.55 ? "matriz__celula--escura" : ""}`}
                          style={{ "--intensidade": dado ? dado.total / maximo : 0 }}
                        >
                          {dado ? (
                            <>
                              {dado.total}
                              {dado.ativas > 0 && <span className="matriz__ativas"> ({dado.ativas})</span>}
                            </>
                          ) : (
                            <span className="matriz__vazio" aria-label="nenhuma">
                              ·
                            </span>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
      }}
    </Relatorio>
  );
}

function ExposicaoModal() {
  const { rotulo, dominio } = useApp();
  const estado = useCarregar(() => api.buscar("/relatorios/exposicao-modal"), []);
  const limites = dominio.limitesRisco;
  const nivel = (valor) =>
    valor >= limites.critico ? "CRITICO" : valor >= limites.alto ? "ALTO" : valor >= limites.moderado ? "MODERADO" : "BAIXO";

  return (
    <Relatorio
      id="rel-modal"
      titulo="Quem está mais exposto agora"
      descricao="Risco médio das ocorrências ativas para cada modal, com quantos cidadãos e rotas salvas cada um tem."
      estado={estado}
    >
      {(linhas) => (
        <ul className="exposicao">
          {linhas.map((linha) => (
            <li key={linha.modal} className="exposicao__item">
              <SinalRisco
                risco={{ valor: linha.riscoMedioAtual, nivel: nivel(linha.riscoMedioAtual) }}
                tamanho="pequeno"
              />
              <span className="exposicao__modal">{rotulo("modais", linha.modal)}</span>
              <span className="exposicao__detalhe">
                {plural(linha.cidadaos, "cidadão", "cidadãos")} ·{" "}
                {plural(linha.rotas, "rota salva", "rotas salvas")} ·{" "}
                {plural(linha.ocorrenciasAtivasNasVias, "ocorrência ativa nelas", "ocorrências ativas nelas")}
              </span>
            </li>
          ))}
        </ul>
      )}
    </Relatorio>
  );
}
