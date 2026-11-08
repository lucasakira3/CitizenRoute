// Validação no front-end. Repete as regras da camada service do Java,
// para o usuário ver o erro antes mesmo de enviar. O Java valida de novo.

const EMAIL = /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/;

export function criarValidador() {
  const erros = {};
  const registrar = (campo, mensagem) => {
    if (!erros[campo]) {
      erros[campo] = mensagem;
    }
  };

  return {
    erros,
    registrar,

    texto(campo, valor, rotulo, minimo, maximo) {
      const limpo = (valor ?? "").trim();
      if (!limpo) {
        registrar(campo, `Preencha o campo ${rotulo.toLowerCase()}.`);
      } else if (limpo.length < minimo) {
        registrar(campo, `${rotulo} precisa ter pelo menos ${minimo} caracteres.`);
      } else if (limpo.length > maximo) {
        registrar(campo, `${rotulo} pode ter no máximo ${maximo} caracteres.`);
      }
    },

    email(campo, valor) {
      const limpo = (valor ?? "").trim();
      if (!limpo) {
        registrar(campo, "Preencha o campo e-mail.");
      } else if (!EMAIL.test(limpo) || limpo.length > 120) {
        registrar(campo, "Informe um e-mail válido, como nome@exemplo.com.");
      }
    },

    senha(campo, valor, obrigatoria = true) {
      if (!valor) {
        if (obrigatoria) {
          registrar(campo, "Preencha o campo senha.");
        }
        return;
      }
      if (valor.length < 8 || valor.length > 64) {
        registrar(campo, "A senha precisa ter entre 8 e 64 caracteres.");
      } else if (!/[A-Za-z]/.test(valor) || !/\d/.test(valor)) {
        registrar(campo, "A senha precisa ter letras e números.");
      }
    },

    escolha(campo, valor, mensagem) {
      if (valor === undefined || valor === null || valor === "") {
        registrar(campo, mensagem);
      }
    },

    exigir(condicao, campo, mensagem) {
      if (!condicao) {
        registrar(campo, mensagem);
      }
    },
  };
}
