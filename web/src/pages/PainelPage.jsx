import { useRef, useState } from "react";
import { EsqueletoLista, EsqueletoNumeros } from "../components/Esqueleto.jsx";
import { ErroCarga, Vazio } from "../components/Estados.jsx";
import Icone from "../components/Icone.jsx";
import ItemOcorrencia from "../components/ItemOcorrencia.jsx";
import MapaCidade, { zonaDaRegiao } from "../components/MapaCidade.jsx";
import SeletorModal from "../components/SeletorModal.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useContagem } from "../hooks/useContagem.js";
import { useReordenacao } from "../hooks/useReordenacao.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api, consulta } from "../services/api.js";
import { formatarMinutos, formatarNumero } from "../utils/formato.js";
import { MODAL_FRASE } from "../utils/rotulos.js";

const NOMES_ZONA = { NORTE: "Zona Norte", SUL: "Zona Sul", LESTE: "Zona Leste", OESTE: "Zona Oeste", CENTRO: "Centro" };

export default function PainelPage() {
  useTitulo("Painel");
  const { sessao } = useApp();
  const [modal, setModal] = useState(sessao?.tipo === "CIDADAO" ? sessao.modal : "MOTO");
  const [zona, setZona] = useState(null);
  const refLista = useRef(null);

  const ativas = useCarregar(() => api.buscar(`/ocorrencias${consulta({ ativas: true, modal })}`), [modal]);
  const resumo = useCarregar(() => api.buscar("/relatorios/resumo"), []);

  const ordenadas = ativas.dados ? [...ativas.dados].sort((a, b) => b.risco.valor - a.risco.valor) : [];
  const visiveis = zona
    ? ordenadas.filter((ocorrencia) => ocorrencia.vias.some((item) => zonaDaRegiao(item.via.regiao?.nome) === zona))
    : ordenadas;
  const comAlerta = ordenadas.filter((ocorrencia) => ocorrencia.risco.alerta).length;
  const alertasContados = useContagem(ativas.dados ? comAlerta : null, 420);

  // A ordem muda quando o modal muda: os itens deslizam até a nova posição.
  useReordenacao(refLista, visiveis.map((ocorrencia) => `${ocorrencia.id}:${ocorrencia.risco.valor}`).join("|"));

  return (
    <>
      <section className="tese" aria-labelledby="tese-titulo">
        <div className="tese__texto">
          <p className="sobretitulo entrada" style={{ "--atraso": 0 }}>
            Agora em São Paulo
          </p>
          <h1 id="tese-titulo" className="tese__titulo entrada" style={{ "--atraso": 1 }}>
            O mesmo alagamento não pesa igual para todo mundo.
          </h1>
          <p className="lead entrada" style={{ "--atraso": 2 }}>
            Para quem está de carro, água na pista é lentidão. Para quem está de moto ou a pé, é perigo. Escolha como
            você se move e veja a cidade recalculada para você.
          </p>
          <div className="entrada" style={{ "--atraso": 3 }}>
            <SeletorModal valor={modal} aoMudar={setModal} legenda="Calcular o risco para" />
          </div>
          {ativas.dados && (
            <p className="tese__conclusao entrada" style={{ "--atraso": 4 }} aria-live="polite">
              Para quem {MODAL_FRASE[modal]},{" "}
              <strong>
                {Math.round(alertasContados ?? comAlerta)} de {ordenadas.length}
              </strong>{" "}
              ocorrências ativas pedem atenção.
            </p>
          )}
        </div>

        <div className="tese__mapa entrada" style={{ "--atraso": 2 }}>
          {ativas.erro && <ErroCarga erro={ativas.erro} aoTentar={ativas.recarregar} />}
          {!ativas.erro && (
            <>
              <MapaCidade ocorrencias={ordenadas} modo="previa" zonaSelecionada={zona} aoSelecionarZona={setZona} />
              <a className="tese__abrir-mapa" href={`#/mapa?modal=${modal}`}>
                <Icone nome="TELA_CHEIA" tamanho={18} />
                Abrir o mapa 3D
              </a>
            </>
          )}
        </div>
      </section>

      <section className="situacao" aria-labelledby="situacao-titulo">
        <h2 id="situacao-titulo" className="situacao__titulo">
          Situação da cidade
        </h2>
        {resumo.erro && <ErroCarga erro={resumo.erro} aoTentar={resumo.recarregar} />}
        {!resumo.dados && !resumo.erro && <EsqueletoNumeros />}
        {resumo.dados && <NumerosSituacao resumo={resumo.dados} />}
      </section>

      <section className="secao" aria-labelledby="ativas-titulo">
        <div className="secao__cabeca">
          <h2 id="ativas-titulo">Ocorrências ativas, da mais arriscada para a menos</h2>
          <a className="link-seta" href="#/ocorrencias">
            Ver todas as ocorrências
          </a>
        </div>

        {zona && (
          <div className="filtro-zona" role="status">
            <span>
              Mostrando só <strong>{NOMES_ZONA[zona]}</strong>: {visiveis.length}{" "}
              {visiveis.length === 1 ? "ocorrência" : "ocorrências"}
            </span>
            <button type="button" className="botao botao--discreto botao--pequeno" onClick={() => setZona(null)}>
              Mostrar todas as zonas
            </button>
          </div>
        )}

        {!ativas.dados && !ativas.erro && <EsqueletoLista quantidade={4} />}
        {ativas.dados && visiveis.length === 0 && (
          <Vazio titulo={zona ? "Nenhuma ocorrência ativa nesta zona." : "Nenhuma ocorrência ativa agora."}>
            <p>Viu algum problema na via? Reporte e ajude quem passa por ela.</p>
          </Vazio>
        )}
        <div className="lista-ocorrencias" ref={refLista}>
          {visiveis.map((ocorrencia, indice) => (
            <ItemOcorrencia key={ocorrencia.id} ocorrencia={ocorrencia} risco={ocorrencia.risco} indice={indice} />
          ))}
        </div>
      </section>

      <section className="secao papeis" aria-label="O que cada pessoa faz no CitizenRoute">
        <div className="papel">
          <h2 className="papel__titulo">Quem circula se protege</h2>
          <p>Reporte o que viu, salve as vias do seu caminho e receba o risco calculado para o seu modal.</p>
          <div className="acoes">
            <a className="botao botao--primario" href="#/ocorrencias/nova">
              Reportar ocorrência
            </a>
            <a className="botao botao--secundario" href="#/meu-trajeto">
              Montar meu trajeto
            </a>
          </div>
        </div>
        <div className="papel">
          <h2 className="papel__titulo">A gestão pública age</h2>
          <p>CET, Defesa Civil, SPTrans, Subprefeitura e GCM confirmam relatos, registram ações e acompanham até resolver.</p>
          <div className="acoes">
            <a className="botao botao--secundario" href="#/gestao">
              Abrir a fila da gestão
            </a>
            <a className="botao botao--discreto" href="#/relatorios">
              Ver relatórios
            </a>
          </div>
        </div>
      </section>
    </>
  );
}

function Numero({ valor, formatar }) {
  const contado = useContagem(valor);
  return <>{valor === null || valor === undefined ? "—" : formatar(contado)}</>;
}

function NumerosSituacao({ resumo }) {
  const inteiro = (valor) => formatarNumero(Math.round(valor));
  const itens = [
    { valor: resumo.ocorrenciasAtivas, formatar: inteiro, rotulo: "ocorrências ativas" },
    { valor: resumo.aguardandoConfirmacao, formatar: inteiro, rotulo: "aguardando confirmação" },
    { valor: resumo.emAtendimento, formatar: inteiro, rotulo: "em atendimento" },
    { valor: resumo.gravesAtivas, formatar: inteiro, rotulo: "graves ou muito graves" },
    { valor: resumo.resolvidasUltimos30Dias, formatar: inteiro, rotulo: "resolvidas em 30 dias" },
    {
      valor: resumo.tempoMedioRespostaMinutos,
      formatar: formatarMinutos,
      rotulo: "até a primeira resposta, em média",
    },
  ];
  return (
    <dl className="situacao__lista">
      {itens.map((item) => (
        <div key={item.rotulo} className="situacao__item">
          <dt>{item.rotulo}</dt>
          <dd>
            <Numero valor={item.valor} formatar={item.formatar} />
          </dd>
        </div>
      ))}
    </dl>
  );
}
