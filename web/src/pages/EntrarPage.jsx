import { useState } from "react";
import Campo from "../components/Campo.jsx";
import Icone from "../components/Icone.jsx";
import { Aviso } from "../components/Estados.jsx";
import { useApp } from "../contexto.js";
import { useFormulario } from "../hooks/useFormulario.js";
import { navegar } from "../hooks/useRota.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api } from "../services/api.js";
import { avisarNaProximaPagina, retirarAvisoPendente } from "../services/aviso.js";
import { criarValidador } from "../utils/validacao.js";

/** Contas de teste criadas pelo sql/script.sql: um clique preenche o login. */
const CONTAS_TESTE = [
  { email: "ana.souza@exemplo.com", nome: "Ana", modal: "MOTO" },
  { email: "bruno.lima@exemplo.com", nome: "Bruno", modal: "CARRO" },
];
const SENHA_TESTE = "Citizen@2026";

export default function EntrarPage() {
  useTitulo("Entrar");
  const { sessao, sair } = useApp();
  const [aviso, setAviso] = useState(retirarAvisoPendente);

  return (
    <>
      <header className="cabeca-pagina">
        <div>
          <h1>Entrar</h1>
          <p className="lead">Salve rotas e receba alertas do seu caminho.</p>
        </div>
      </header>

      <Aviso aviso={aviso} aoFechar={() => setAviso(null)} />

      {sessao ? (
        <section className="cartao sessao-atual">
          <h2>Você já entrou</h2>
          <p>
            {sessao.nome} · {sessao.email}
          </p>
          <div className="acoes">
            <a className="botao botao--primario" href="#/perfil">
              Ir para o meu perfil
            </a>
            <button
              type="button"
              className="botao botao--secundario"
              onClick={() => {
                sair();
                setAviso({ tipo: "info", texto: "Você saiu da sua conta." });
              }}
            >
              Sair e trocar de conta
            </button>
          </div>
        </section>
      ) : (
        <div className="entrar">
          <Login />
          <Cadastro />
        </div>
      )}
    </>
  );
}

function Login() {
  const { entrar } = useApp();
  const form = useFormulario({ email: "", senha: "" });

  async function enviar(evento) {
    evento.preventDefault();
    let usuario;
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.email("email", valores.email);
        validador.exigir(Boolean(valores.senha), "senha", "Preencha o campo senha.");
        return validador.erros;
      },
      async (valores) => {
        usuario = await api.criar("/auth/login", { email: valores.email.trim(), senha: valores.senha });
      },
    );
    if (ok) {
      entrar(usuario);
      avisarNaProximaPagina("sucesso", `Olá, ${usuario.nome.split(" ")[0]}. Que bom ter você de volta.`);
      navegar("/perfil");
    }
  }

  return (
    <section className="cartao entrar__bloco" aria-labelledby="login-titulo">
      <h2 id="login-titulo">Já tenho conta</h2>
      <form className="formulario" onSubmit={enviar} noValidate>
        <Aviso aviso={form.aviso} />
        <Campo id="login-email" rotulo="E-mail" erro={form.erros.email}>
          <input type="email" autoComplete="username" value={form.valores.email} onChange={form.alterar("email")} />
        </Campo>
        <Campo id="login-senha" rotulo="Senha" erro={form.erros.senha}>
          <input type="password" autoComplete="current-password" value={form.valores.senha} onChange={form.alterar("senha")} />
        </Campo>
        <button type="submit" className="botao botao--primario" disabled={form.enviando}>
          {form.enviando ? "Entrando…" : "Entrar"}
        </button>
      </form>

      <div className="contas-teste">
        <span className="contas-teste__rotulo">Contas de teste</span>
        {CONTAS_TESTE.map((conta) => (
          <button
            key={conta.email}
            type="button"
            className="contas-teste__conta"
            onClick={() => form.setValores({ email: conta.email, senha: SENHA_TESTE })}
          >
            <Icone nome={conta.modal} tamanho={16} />
            {conta.nome}
          </button>
        ))}
      </div>

    </section>
  );
}

function Cadastro() {
  const { entrar } = useApp();
  const form = useFormulario({ nome: "", email: "", senha: "", confirmacao: "" });

  async function enviar(evento) {
    evento.preventDefault();
    let usuario;
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.texto("nome", valores.nome, "Nome", 3, 100);
        validador.email("email", valores.email);
        validador.senha("senha", valores.senha);
        validador.exigir(valores.senha === valores.confirmacao, "confirmacao", "As senhas não são iguais.");
        return validador.erros;
      },
      async (valores) => {
        await api.criar("/cidadaos", {
          nome: valores.nome.trim(),
          email: valores.email.trim(),
          senha: valores.senha,
        });
        usuario = await api.criar("/auth/login", { email: valores.email.trim(), senha: valores.senha });
      },
    );
    if (ok) {
      entrar(usuario);
      avisarNaProximaPagina("sucesso", `Conta criada. Boas-vindas, ${usuario.nome.split(" ")[0]}! Planeje uma rota e salve para receber alertas.`);
      navegar("/perfil");
    }
  }

  return (
    <section className="cartao entrar__bloco" aria-labelledby="cadastro-titulo">
      <h2 id="cadastro-titulo">Criar conta</h2>
      <form className="formulario" onSubmit={enviar} noValidate>
        <Aviso aviso={form.aviso} />
        <Campo id="cad-nome" rotulo="Nome" erro={form.erros.nome}>
          <input type="text" autoComplete="name" maxLength={100} value={form.valores.nome} onChange={form.alterar("nome")} />
        </Campo>
        <Campo id="cad-email" rotulo="E-mail" erro={form.erros.email}>
          <input type="email" autoComplete="email" maxLength={120} value={form.valores.email} onChange={form.alterar("email")} />
        </Campo>
        <Campo id="cad-senha" rotulo="Senha" erro={form.erros.senha} dica="Pelo menos 8 caracteres, com letras e números.">
          <input type="password" autoComplete="new-password" maxLength={64} value={form.valores.senha} onChange={form.alterar("senha")} />
        </Campo>
        <Campo id="cad-confirmacao" rotulo="Repita a senha" erro={form.erros.confirmacao}>
          <input
            type="password"
            autoComplete="new-password"
            maxLength={64}
            value={form.valores.confirmacao}
            onChange={form.alterar("confirmacao")}
          />
        </Campo>
        <button type="submit" className="botao botao--primario" disabled={form.enviando}>
          {form.enviando ? "Criando…" : "Criar conta"}
        </button>
      </form>
    </section>
  );
}
