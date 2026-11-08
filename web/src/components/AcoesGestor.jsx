import { useId, useState } from "react";
import { useFormulario } from "../hooks/useFormulario.js";
import { api } from "../services/api.js";
import { ACAO_STATUS } from "../utils/rotulos.js";
import { criarValidador } from "../utils/validacao.js";
import Campo from "./Campo.jsx";
import { Aviso } from "./Estados.jsx";

function validarDescricao(valores) {
  const validador = criarValidador();
  validador.texto("descricao", valores.descricao, "Descrição", 5, 500);
  return validador.erros;
}

/**
 * Botões com os próximos status permitidos. Ao escolher um, o gestor descreve o que foi feito;
 * o Java muda o status e grava a ação no histórico na mesma transação.
 */
export function AlterarStatus({ idOcorrencia, proximos, aoConcluir }) {
  const [escolhido, setEscolhido] = useState(null);
  const form = useFormulario({ descricao: "" });
  const idCampo = useId();

  if (!proximos.length) {
    return null;
  }

  async function enviar(evento) {
    evento.preventDefault();
    const status = escolhido;
    const ok = await form.enviar(validarDescricao, (valores) =>
      api.alterar(`/ocorrencias/${idOcorrencia}/status`, { status, descricao: valores.descricao }),
    );
    if (ok) {
      setEscolhido(null);
      form.reiniciar();
      aoConcluir(ACAO_STATUS[status].feito);
    }
  }

  return (
    <div className="alterar-status">
      <div className="alterar-status__botoes" role="group" aria-label="Mudar o status">
        {proximos.map((status) => (
          <button
            key={status}
            type="button"
            className={`botao botao--pequeno ${status === "DESCARTADA" ? "botao--discreto" : "botao--secundario"}`}
            aria-pressed={escolhido === status}
            onClick={() => {
              setEscolhido(status);
              form.reiniciar();
            }}
          >
            {ACAO_STATUS[status].botao}
          </button>
        ))}
      </div>

      {escolhido && (
        <form className="formulario formulario--compacto" onSubmit={enviar} noValidate>
          <Aviso aviso={form.aviso} />
          <Campo
            id={idCampo}
            rotulo={ACAO_STATUS[escolhido].pergunta}
            erro={form.erros.descricao}
            dica={ACAO_STATUS[escolhido].exemplo}
          >
            <textarea rows={2} maxLength={500} value={form.valores.descricao} onChange={form.alterar("descricao")} />
          </Campo>
          <div className="acoes">
            <button type="submit" className="botao botao--primario botao--pequeno" disabled={form.enviando}>
              {form.enviando ? "Salvando…" : ACAO_STATUS[escolhido].botao}
            </button>
            <button type="button" className="botao botao--discreto botao--pequeno" onClick={() => setEscolhido(null)}>
              Cancelar
            </button>
          </div>
        </form>
      )}
    </div>
  );
}

/** Registro de andamento sem mudar o status, como "equipe a caminho". */
export function RegistrarAndamento({ idOcorrencia, aoConcluir }) {
  const form = useFormulario({ descricao: "" });
  const idCampo = useId();

  async function enviar(evento) {
    evento.preventDefault();
    const ok = await form.enviar(validarDescricao, (valores) =>
      api.criar(`/ocorrencias/${idOcorrencia}/acoes`, { descricao: valores.descricao }),
    );
    if (ok) {
      form.reiniciar();
      aoConcluir("Andamento registrado no histórico.");
    }
  }

  return (
    <form className="formulario formulario--compacto" onSubmit={enviar} noValidate>
      <Aviso aviso={form.aviso} />
      <Campo
        id={idCampo}
        rotulo="Registrar andamento"
        erro={form.erros.descricao}
        dica="Atualize quem acompanha a ocorrência sem mudar o status."
      >
        <textarea rows={2} maxLength={500} value={form.valores.descricao} onChange={form.alterar("descricao")} />
      </Campo>
      <button type="submit" className="botao botao--secundario botao--pequeno" disabled={form.enviando}>
        {form.enviando ? "Salvando…" : "Registrar andamento"}
      </button>
    </form>
  );
}
