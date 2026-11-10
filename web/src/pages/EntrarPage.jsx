import { useState } from "react";
import Campo from "../components/Campo.jsx";
import { Aviso } from "../components/Estados.jsx";
import { useApp } from "../contexto.js";
import { useFormulario } from "../hooks/useFormulario.js";
import { navegar } from "../hooks/useRota.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api } from "../services/api.js";
import { avisarNaProximaPagina, retirarAvisoPendente } from "../services/aviso.js";
import { criarValidador } from "../utils/validacao.js";

const SENHA_DEMO = "Citizen@2026";

/** Contas criadas pelo sql/script.sql para a demonstração. */
const CONTAS_DEMO = [
  { email: "ana.souza@exemplo.com", nome: "Ana Souza", papel: "Anda de moto" },
  { email: "bruno.lima@exemplo.com", nome: "Bruno Lima", papel: "Anda de carro" },
  { email: "elisa.ferreira@exemplo.com", nome: "Elisa Ferreira", papel: "Anda a pé" },
  { email: "gabriela.prado@exemplo.com", nome: "Gabriela Prado", papel: "Gestão · CET" },
  { email: "henrique.alves@exemplo.com", nome: "Henrique Alves", papel: "Gestão · Defesa Civil" },
];

function destinoApos(usuario) {
  return usuario.tipo === "GESTOR" ? "/gestao" : "/minhas-rotas";
}

export default function EntrarPage() {
  useTitulo("Entrar");
  const { sessao, sair } = useApp();
  const [aviso, setAviso] = useState(retirarAvisoPendente);

  return (
    <>
      <header className="cabeca-pagina">
        <div>
          <p className="sobretitulo">Sua conta</p>
          <h1>Entrar no CitizenRoute</h1>
          <p className="lead">
            Cidadãos reportam ocorrências e recebem alertas nas vias que monitoram. Gestores públicos confirmam,
            registram ações e acompanham até resolver.
          </p>
        </div>
      </header>

      <Aviso aviso={aviso} aoFechar={() => setAviso(null)} />

      {sessao ? (
        <section className="cartao sessao-atual">
          <h2>Você já entrou</h2>
          <p>
            {sessao.nome} · {sessao.perfil}
          </p>
          <div className="acoes">
            <a className="botao botao--primario" href={`#${destinoApos(sessao)}`}>
              {sessao.tipo === "GESTOR" ? "Abrir a fila da gestão" : "Ver minhas rotas"}
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
      avisarNaProximaPagina("sucesso", `Olá, ${usuario.nome.split(" ")[0]}. Você entrou como ${usuario.perfil.toLowerCase()}.`);
      navegar(destinoApos(usuario));
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

      <div className="demo">
        <h3 className="subtitulo">Contas de demonstração</h3>
        <p className="campo__dica">
          Senha de todas: <code>{SENHA_DEMO}</code>. Compare a Ana e o Bruno: os dois passam pela Radial Leste.
        </p>
        <ul className="demo__lista">
          {CONTAS_DEMO.map((conta) => (
            <li key={conta.email}>
              <button
                type="button"
                className="demo__conta"
                onClick={() => form.setValores({ email: conta.email, senha: SENHA_DEMO })}
              >
                <span className="demo__nome">{conta.nome}</span>
                <span className="demo__papel">{conta.papel}</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

function Cadastro() {
  const { dominio, entrar } = useApp();
  const form = useFormulario({ nome: "", email: "", modal: "", senha: "", confirmacao: "" });

  async function enviar(evento) {
    evento.preventDefault();
    let usuario;
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.texto("nome", valores.nome, "Nome", 3, 100);
        validador.email("email", valores.email);
        validador.escolha("modal", valores.modal, "Escolha como você se locomove.");
        validador.senha("senha", valores.senha);
        validador.exigir(valores.senha === valores.confirmacao, "confirmacao", "As senhas não são iguais.");
        return validador.erros;
      },
      async (valores) => {
        await api.criar("/cidadaos", {
          nome: valores.nome.trim(),
          email: valores.email.trim(),
          modal: valores.modal,
          senha: valores.senha,
        });
        usuario = await api.criar("/auth/login", { email: valores.email.trim(), senha: valores.senha });
      },
    );
    if (ok) {
      entrar(usuario);
      avisarNaProximaPagina("sucesso", "Conta criada. Agora salve as vias do seu caminho para receber alertas.");
      navegar("/minhas-rotas");
    }
  }

  return (
    <section className="cartao entrar__bloco" aria-labelledby="cadastro-titulo">
      <h2 id="cadastro-titulo">Criar conta de cidadão</h2>
      <form className="formulario" onSubmit={enviar} noValidate>
        <Aviso aviso={form.aviso} />
        <Campo id="cad-nome" rotulo="Nome" erro={form.erros.nome}>
          <input type="text" autoComplete="name" maxLength={100} value={form.valores.nome} onChange={form.alterar("nome")} />
        </Campo>
        <Campo id="cad-email" rotulo="E-mail" erro={form.erros.email}>
          <input type="email" autoComplete="email" maxLength={120} value={form.valores.email} onChange={form.alterar("email")} />
        </Campo>
        <Campo
          id="cad-modal"
          rotulo="Como você se locomove no dia a dia"
          erro={form.erros.modal}
          dica="Os alertas calculam o risco para este modal. Dá para trocar depois."
        >
          <select value={form.valores.modal} onChange={form.alterar("modal")}>
            <option value="">Escolha uma opção</option>
            {dominio.modais.map((modal) => (
              <option key={modal.codigo} value={modal.codigo}>
                {modal.rotulo}
              </option>
            ))}
          </select>
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
