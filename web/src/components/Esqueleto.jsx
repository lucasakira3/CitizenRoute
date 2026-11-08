/** Formas cinzentas no lugar do conteúdo enquanto ele carrega. */

export function EsqueletoLista({ quantidade = 4 }) {
  return (
    <div className="esqueleto-lista" role="status" aria-label="Carregando ocorrências">
      {Array.from({ length: quantidade }, (_, indice) => (
        <div key={indice} className="esqueleto-item" aria-hidden="true">
          <span className="esqueleto esqueleto--losango" />
          <span className="esqueleto-item__linhas">
            <span className="esqueleto esqueleto--linha esqueleto--curta" />
            <span className="esqueleto esqueleto--linha esqueleto--titulo" />
            <span className="esqueleto esqueleto--linha esqueleto--media" />
          </span>
        </div>
      ))}
    </div>
  );
}

export function EsqueletoNumeros({ quantidade = 6 }) {
  return (
    <div className="esqueleto-numeros" role="status" aria-label="Carregando números da cidade">
      {Array.from({ length: quantidade }, (_, indice) => (
        <span key={indice} className="esqueleto-numero" aria-hidden="true">
          <span className="esqueleto esqueleto--numero" />
          <span className="esqueleto esqueleto--linha esqueleto--curta" />
        </span>
      ))}
    </div>
  );
}
