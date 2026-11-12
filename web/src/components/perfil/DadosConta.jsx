import { useApp } from "../../contexto.js";
import { useFormulario } from "../../hooks/useFormulario.js";
import { navegar } from "../../hooks/useRota.js";
import { api } from "../../services/api.js";
import { avisarNaProximaPagina } from "../../services/aviso.js";
import { criarValidador } from "../../utils/validacao.js";
import Campo from "../Campo.jsx";
import { Aviso } from "../Estados.jsx";
import CabecaSecao from "./CabecaSecao.jsx";

/** Minha conta › Meus dados: nome, e-mail, senha e exclusão da conta. */
export default function DadosConta({ setAviso }) {
  const { sessao, entrar, sair } = useApp();
  const form = useFormulario({ nome: sessao.nome, email: sessao.email, senha: "" });

  async function salvar(evento) {
    evento.preventDefault();
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.texto("nome", valores.nome, "Nome", 3, 100);
        validador.email("email", valores.email);
        validador.senha("senha", valores.senha, false);
        return validador.erros;
      },
      async (valores) => {
        const atualizado = await api.atualizar(`/cidadaos/${sessao.id}`, {
          nome: valores.nome.trim(),
          email: valores.email.trim(),
          // O PUT grava o modal junto: reenvia o atual para não apagá-lo
          modal: sessao.modal ?? undefined,
          senha: valores.senha || undefined,
        });
        entrar(atualizado);
      },
    );
    if (ok) {
      form.definir("senha", "");
      setAviso({ tipo: "sucesso", texto: "Seus dados foram atualizados." });
    }
  }

  async function excluirConta() {
    if (!window.confirm("Excluir a sua conta? Suas rotas e endereços salvos também serão apagados.")) {
      return;
    }
    try {
      await api.excluir(`/cidadaos/${sessao.id}`);
      sair();
      avisarNaProximaPagina("sucesso", "Sua conta foi excluída.");
      navegar("/entrar");
    } catch (erro) {
      setAviso({ tipo: "erro", texto: erro.message });
    }
  }

  return (
    <section className="perfil-secao" aria-labelledby="secao-dados">
      <CabecaSecao id="secao-dados" titulo="Meus dados" />

      <form className="cartao-perfil formulario perfil-form" onSubmit={salvar} noValidate>
        <Aviso aviso={form.aviso} />
        <Campo id="dados-nome" rotulo="Nome" erro={form.erros.nome}>
          <input type="text" autoComplete="name" maxLength={100} value={form.valores.nome} onChange={form.alterar("nome")} />
        </Campo>
        <Campo id="dados-email" rotulo="E-mail" erro={form.erros.email}>
          <input type="email" autoComplete="email" maxLength={120} value={form.valores.email} onChange={form.alterar("email")} />
        </Campo>
        <Campo id="dados-senha" rotulo="Nova senha" opcional erro={form.erros.senha} dica="Em branco, mantém a atual.">
          <input type="password" autoComplete="new-password" maxLength={64} value={form.valores.senha} onChange={form.alterar("senha")} />
        </Campo>
        <div className="acoes">
          <button type="submit" className="botao botao--primario" disabled={form.enviando}>
            {form.enviando ? "Salvando…" : "Salvar dados"}
          </button>
        </div>
      </form>

      <div className="cartao-perfil perfil-perigo">
        <div>
          <h2 className="perfil-subtitulo">Excluir conta</h2>
          <p className="texto-apoio">Apaga também rotas e endereços.</p>
        </div>
        <button type="button" className="botao botao--perigo" onClick={excluirConta}>
          Excluir minha conta
        </button>
      </div>
    </section>
  );
}
