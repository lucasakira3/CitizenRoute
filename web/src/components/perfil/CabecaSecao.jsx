/** Título de cada seção do perfil, com o caminho (pilar › seção) e uma ação opcional à direita. */
export default function CabecaSecao({ id, caminho, titulo, descricao, children }) {
  return (
    <header className="perfil-secao__cabeca">
      <div>
        {caminho && <p className="perfil-secao__caminho">{caminho}</p>}
        <h1 id={id} className="perfil-secao__titulo">
          {titulo}
        </h1>
        {descricao && <p className="perfil-secao__descricao">{descricao}</p>}
      </div>
      {children && <div className="perfil-secao__acao">{children}</div>}
    </header>
  );
}
