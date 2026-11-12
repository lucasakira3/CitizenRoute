/** Linha de abertura de uma aba do perfil: uma frase curta (opcional) e a ação principal à direita. */
export default function IntroAba({ descricao, children }) {
  return (
    <div className="intro-aba">
      {descricao && <p className="intro-aba__descricao">{descricao}</p>}
      {children && <div className="intro-aba__acao">{children}</div>}
    </div>
  );
}
