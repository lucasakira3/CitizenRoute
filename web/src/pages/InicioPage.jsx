import { useApp } from "../contexto.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { PILARES } from "../utils/pilares.js";

export default function InicioPage() {
  useTitulo(null);
  const { sessao } = useApp();
  const primeiroNome = sessao?.nome.split(" ")[0];

  return (
    <>
      <section className="hub-apresentacao" aria-labelledby="hub-titulo">
        <div className="hub-apresentacao__texto">
          <p className="hub-apresentacao__eyebrow">Citizen Life</p>
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
          <p className="hub-apresentacao__lead">
            Mobilidade, alimentação, lazer, segurança, educação e moradia reunidos em um só lugar.
          </p>
          <div className="hub-apresentacao__acoes">
            <a className="botao botao--primario botao--seta" href="#/mobilidade/rota">
              Planejar rota
            </a>
            <a className="botao botao--secundario" href="#/alimentacao">
              Ver o que tem perto
            </a>
          </div>
        </div>

        <aside className="hub-resumo" aria-label="Resumo do dia">
          <div className="hub-resumo__topo">
            <span>Hoje perto de você</span>
          </div>
          <ul className="hub-resumo__lista">
            <li>
              <strong>Feira livre</strong>
              <span>Rua do Porto · às 18h</span>
            </li>
            <li>
              <strong>Rota mais segura</strong>
              <span>7 min · sem risco alto</span>
            </li>
            <li>
              <strong>Evento</strong>
              <span>Parque da Luz · hoje à noite</span>
            </li>
          </ul>
        </aside>
      </section>

      <section className="hub-apps" aria-labelledby="apps-titulo">
        <header className="hub-secao">
          <h2 id="apps-titulo">Apps do dia a dia</h2>
          <p>Escolha o que precisa resolver agora.</p>
        </header>

        <div className="hub-app-grid">
          {PILARES.map((pilar) => (
            <a
              key={pilar.id}
              href={`#/${pilar.id}`}
              className={`hub-app ${pilar.disponivel ? "hub-app--disponivel" : ""}`}
              aria-label={`${pilar.nome}${pilar.disponivel ? ", disponível" : ", em breve"}`}
            >
              <div className="hub-app__conteudo">
                <h3>{pilar.nome}</h3>
                <p>{pilar.resumo}</p>
              </div>

              <div className="hub-app__rodape">
                <span>{pilar.disponivel ? "Disponível" : "Em breve"}</span>
              </div>
            </a>
          ))}
        </div>
      </section>
    </>
  );
}
