import Icone from "../components/Icone.jsx";
import { Cidadao } from "../components/Logo.jsx";
import { useApp } from "../contexto.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { PILARES } from "../utils/pilares.js";

export default function InicioPage() {
  useTitulo(null);
  const { sessao } = useApp();
  const primeiroNome = sessao?.nome.split(" ")[0];

  return (
    <>
      <section className="hub-abertura" aria-labelledby="hub-titulo">
        <div className="hub-abertura__texto">
          <h1 id="hub-titulo">
            {primeiroNome ? (
              <>
                {primeiroNome}, o que você <em>precisa hoje?</em>
              </>
            ) : (
              <>
                O seu dia a dia na cidade, <em>num lugar só.</em>
              </>
            )}
          </h1>
          <p className="hub-abertura__lead">Seis pilares da vida na cidade.</p>
          <div className="hub-abertura__acoes">
            <a className="botao botao--primario botao--seta" href="#/mobilidade/rota">
              Planejar rota
              <Icone nome="SETA" tamanho={18} />
            </a>
            <a className="botao botao--secundario" href="#/mobilidade/mapa">
              Mapa de riscos
            </a>
          </div>
        </div>

        <div className="orbita" aria-hidden="true">
          <span className="orbita__anel" />
          <span className="orbita__centro">
            <Cidadao tamanho={92} />
          </span>
          {PILARES.map((pilar, indice) => (
            <span
              key={pilar.id}
              className={`orbita__pilar ${pilar.disponivel ? "orbita__pilar--ativo" : ""}`}
              style={{
                "--angulo": `${indice * 60 - 90}deg`,
                "--cor-pilar": pilar.cor,
                "--cor-icone": pilar.corIcone,
                "--i": indice,
              }}
            >
              <span className="orbita__bolha">
                <Icone nome={pilar.icone} tamanho={24} />
              </span>
            </span>
          ))}
        </div>
      </section>

      <section aria-labelledby="pilares-titulo">
        <header className="hub-secao">
          <h2 id="pilares-titulo">Pilares</h2>
        </header>
        <ul className="pilares">
          {PILARES.map((pilar, indice) => (
            <li key={pilar.id} style={{ "--i": indice, "--cor-pilar": pilar.cor, "--cor-icone": pilar.corIcone }}>
              <a
                className={`pilar ${pilar.disponivel ? "pilar--disponivel" : ""}`}
                href={`#/${pilar.id}`}
                aria-describedby={`pilar-${pilar.id}-resumo`}
              >
                <span className="pilar__topo">
                  <span className="pilar__icone">
                    <Icone nome={pilar.icone} tamanho={26} />
                  </span>
                  <span className={`pilar__selo ${pilar.disponivel ? "" : "pilar__selo--breve"}`}>
                    {pilar.disponivel ? "Disponível" : "Em breve"}
                  </span>
                </span>
                <span className="pilar__nome">{pilar.nome}</span>
                <span id={`pilar-${pilar.id}-resumo`} className="pilar__consulta-nome">
                  {pilar.consulta}
                  <Icone nome="SETA" tamanho={18} />
                </span>
              </a>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
