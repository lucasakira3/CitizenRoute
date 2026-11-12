import { useApp } from "../../contexto.js";
import { useCarregar } from "../../hooks/useCarregar.js";
import { api } from "../../services/api.js";
import { PILARES } from "../../utils/pilares.js";
import { ICONE_ENDERECO } from "../../utils/rotulos.js";
import Icone from "../Icone.jsx";
import SinalRisco from "../SinalRisco.jsx";

/** Minha conta › Resumo: o cartão do cidadão e os números da Mobilidade. */
export default function ResumoConta() {
  const { sessao, rotulo } = useApp();
  const rotas = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/rotas`), [sessao.id]);
  const alertas = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/alertas`), [sessao.id, sessao.modal]);
  const enderecos = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/enderecos`), [sessao.id]);

  const importantes = (alertas.dados ?? []).filter((alerta) => alerta.risco.alerta);
  const outrosPilares = PILARES.filter((pilar) => !pilar.disponivel);

  return (
    <section className="perfil-secao" aria-labelledby="secao-resumo">
      <h1 id="secao-resumo" className="visualmente-oculto">
        Resumo da conta
      </h1>

      <div className="cartao-cidadao">
        <span className="cartao-cidadao__avatar" aria-hidden="true">
          {sessao.nome.charAt(0).toUpperCase()}
        </span>
        <div className="cartao-cidadao__texto">
          <p className="cartao-cidadao__nome">{sessao.nome}</p>
          <p className="cartao-cidadao__email">{sessao.email}</p>
        </div>
        <a className="cartao-cidadao__modal" href="#/perfil/mobilidade/locomocao">
          <Icone nome={sessao.modal ?? "PESSOA"} tamanho={18} />
          {sessao.modal ? rotulo("modais", sessao.modal) : "Como você se locomove?"}
        </a>
      </div>

      <div className="quadros">
        <a className="quadro" href="#/perfil/mobilidade/rotas">
          <span className="quadro__rotulo">Alertas</span>
          <span className="quadro__valor">
            {alertas.dados ? importantes.length : "…"}
            {importantes[0] && <SinalRisco risco={importantes[0].risco} tamanho="pequeno" />}
          </span>
        </a>
        <a className="quadro" href="#/perfil/mobilidade/rotas">
          <span className="quadro__rotulo">Rotas</span>
          <span className="quadro__valor">{rotas.dados ? rotas.dados.length : "…"}</span>
        </a>
        <a className="quadro" href="#/perfil/mobilidade/enderecos">
          <span className="quadro__rotulo">Endereços</span>
          <span className="quadro__valor">
            {enderecos.dados ? enderecos.dados.length : "…"}
            <span className="quadro__icones" aria-hidden="true">
              {(enderecos.dados ?? []).slice(0, 3).map((endereco) => (
                <Icone key={endereco.id} nome={ICONE_ENDERECO[endereco.tipo]} tamanho={16} />
              ))}
            </span>
          </span>
        </a>
      </div>

      <div className="em-breve-linha">
        <span className="em-breve-linha__rotulo">Em breve</span>
        <ul>
          {outrosPilares.map((pilar) => (
            <li key={pilar.id}>
              <a
                href={`#/perfil/${pilar.id}`}
                title={pilar.nome}
                style={{ "--cor-pilar": pilar.cor, "--cor-icone": pilar.corIcone }}
              >
                <Icone nome={pilar.icone} tamanho={18} />
                <span className="visualmente-oculto">{pilar.nome}</span>
              </a>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
