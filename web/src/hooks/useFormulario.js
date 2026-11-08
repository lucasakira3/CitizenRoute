import { useState } from "react";

/**
 * Estado de um formulário: valores, erros por campo, aviso geral e envio.
 *
 * enviar(validar, acao):
 *  1. roda a validação do front-end; se houver erro, mostra e não chama a API;
 *  2. chama a API; se ela recusar, mostra os erros que vieram do Java (campo a campo).
 */
export function useFormulario(valoresIniciais) {
  const [valores, setValores] = useState(valoresIniciais);
  const [erros, setErros] = useState({});
  const [aviso, setAviso] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const alterar = (campo) => (evento) => {
    const valor = evento.target.type === "checkbox" ? evento.target.checked : evento.target.value;
    definir(campo, valor);
  };

  function definir(campo, valor) {
    setValores((anteriores) => ({ ...anteriores, [campo]: valor }));
    setErros((anteriores) => {
      if (!anteriores[campo]) {
        return anteriores;
      }
      const novos = { ...anteriores };
      delete novos[campo];
      return novos;
    });
  }

  function reiniciar(novosValores = valoresIniciais) {
    setValores(novosValores);
    setErros({});
    setAviso(null);
  }

  async function enviar(validar, acao) {
    const encontrados = validar ? validar(valores) : {};
    const quantidade = Object.keys(encontrados).length;
    if (quantidade > 0) {
      setErros(encontrados);
      // Com um único erro, a mensagem ao lado do campo basta (e o foco vai até ele).
      setAviso(quantidade > 1 ? { tipo: "erro", texto: `Revise os ${quantidade} campos destacados.` } : null);
      focarPrimeiroErro();
      return false;
    }

    setEnviando(true);
    setErros({});
    setAviso(null);
    try {
      await acao(valores);
      return true;
    } catch (erro) {
      const campos = erro.campos ?? {};
      setErros(campos);
      const repetida = Object.values(campos).includes(erro.message);
      setAviso(repetida ? null : { tipo: "erro", texto: erro.message });
      focarPrimeiroErro();
      return false;
    } finally {
      setEnviando(false);
    }
  }

  return { valores, setValores, definir, alterar, erros, setErros, aviso, setAviso, enviando, enviar, reiniciar };
}

function focarPrimeiroErro() {
  setTimeout(() => {
    const campo = document.querySelector('[aria-invalid="true"]');
    if (campo) {
      campo.focus();
    }
  }, 0);
}
