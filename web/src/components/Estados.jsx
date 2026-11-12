import { useApp } from "../contexto.js";

/** Mensagem na página. Erros usam role="alert" para serem lidos na hora por leitores de tela. */
export function Aviso({ aviso, aoFechar }) {
  if (!aviso) {
    return null;
  }
  return (
    <div className={`aviso aviso--${aviso.tipo}`} role={aviso.tipo === "erro" ? "alert" : "status"}>
      <p>{aviso.texto}</p>
      {aoFechar && (
        <button type="button" className="aviso__fechar" onClick={aoFechar} aria-label="Fechar mensagem">
          ×
        </button>
      )}
    </div>
  );
}

export function Carregando({ texto = "Carregando…" }) {
  return (
    <p className="carregando" role="status">
      <span className="carregando__marca" aria-hidden="true" />
      {texto}
    </p>
  );
}

export function ErroCarga({ erro, aoTentar }) {
  return (
    <div className="aviso aviso--erro" role="alert">
      <p>{erro.message}</p>
      {aoTentar && (
        <button type="button" className="botao botao--secundario botao--pequeno" onClick={aoTentar}>
          Tentar de novo
        </button>
      )}
    </div>
  );
}

export function Vazio({ titulo, children }) {
  return (
    <div className="vazio">
      <p className="vazio__titulo">{titulo}</p>
      {children && <div className="vazio__texto">{children}</div>}
    </div>
  );
}

/**
 * Mostra o conteúdo só para quem tem o perfil certo.
 * Sem login, convida a entrar; com o perfil errado, explica de quem é a área.
 */
export function ExigirPerfil({ motivo, children }) {
  const { sessao } = useApp();
  if (!sessao) {
    return (
      <Vazio titulo="Entre para continuar">
        <p>{motivo}</p>
        <a className="botao botao--primario" href="#/entrar">
          Entrar
        </a>
      </Vazio>
    );
  }
  return children;
}
