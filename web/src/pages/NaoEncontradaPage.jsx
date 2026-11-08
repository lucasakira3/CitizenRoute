import { useTitulo } from "../hooks/useTitulo.js";

export default function NaoEncontradaPage() {
  useTitulo("Página não encontrada");
  return (
    <section className="secao nao-encontrada">
      <p className="sobretitulo">Rota sem saída</p>
      <h1>Esta página não existe.</h1>
      <p className="lead">Confira o endereço ou volte para o painel.</p>
      <a className="botao botao--primario" href="#/">
        Ir para o painel
      </a>
    </section>
  );
}
