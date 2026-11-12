import { useEffect, useState } from "react";
import { useApp } from "../../contexto.js";
import { useCarregar } from "../../hooks/useCarregar.js";
import { useFormulario } from "../../hooks/useFormulario.js";
import { api } from "../../services/api.js";
import { carregarMapa } from "../../utils/mapaCidade.js";
import { ICONE_ENDERECO } from "../../utils/rotulos.js";
import { criarValidador } from "../../utils/validacao.js";
import Campo from "../Campo.jsx";
import CampoLugar from "../CampoLugar.jsx";
import { Aviso, Carregando, ErroCarga, Vazio } from "../Estados.jsx";
import Icone from "../Icone.jsx";
import IntroAba from "./IntroAba.jsx";

/** Mobilidade › Meus endereços: casa, trabalho e outros lugares, que viram atalho na tela de rotas. */
export default function Enderecos({ setAviso }) {
  const { sessao, dominio } = useApp();
  const enderecos = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/enderecos`), [sessao.id]);
  // null: só a lista; "novo": formulário vazio; um endereço: formulário de edição
  const [editando, setEditando] = useState(null);
  const lista = enderecos.dados ?? [];
  const cheio = lista.length >= dominio.maximoEnderecos;

  function aposSalvar(texto) {
    setEditando(null);
    setAviso({ tipo: "sucesso", texto });
    enderecos.recarregar();
  }

  async function excluir(endereco) {
    if (!window.confirm(`Excluir o endereço "${endereco.apelido}"?`)) return;
    try {
      await api.excluir(`/cidadaos/${sessao.id}/enderecos/${endereco.id}`);
      setAviso({ tipo: "sucesso", texto: "Endereço excluído." });
      enderecos.recarregar();
    } catch (erro) {
      setAviso({ tipo: "erro", texto: erro.message });
    }
  }

  return (
    <>
      <IntroAba>
        {editando === null && (
          <button type="button" className="botao botao--primario" onClick={() => setEditando("novo")} disabled={cheio}>
            <Icone nome="MAIS" tamanho={18} />
            Novo endereço
          </button>
        )}
      </IntroAba>

      {editando !== null && (
        <FormularioEndereco
          endereco={editando === "novo" ? null : editando}
          existentes={lista}
          aoSalvar={aposSalvar}
          aoCancelar={() => setEditando(null)}
        />
      )}

      {enderecos.carregando && !enderecos.dados && <Carregando />}
      {enderecos.erro && <ErroCarga erro={enderecos.erro} aoTentar={enderecos.recarregar} />}
      {enderecos.dados && lista.length === 0 && editando === null && (
        <Vazio titulo="Nenhum endereço salvo." />
      )}

      <ul className="enderecos cartoes-perfil">
        {lista.map((endereco) => (
          <li key={endereco.id} className="endereco cartao-perfil">
            <span className={`endereco__icone endereco__icone--${endereco.tipo}`} aria-hidden="true">
              <Icone nome={ICONE_ENDERECO[endereco.tipo]} tamanho={24} />
            </span>
            <div className="endereco__texto">
              <p className="endereco__apelido">{endereco.apelido}</p>
              <p className="endereco__local">{endereco.local.descricao}</p>
            </div>
            <div className="acoes acoes--linha endereco__acoes">
              <button type="button" className="botao-texto" onClick={() => setEditando(endereco)}>
                Editar<span className="visualmente-oculto"> {endereco.apelido}</span>
              </button>
              <button type="button" className="botao-texto botao-texto--perigo" onClick={() => excluir(endereco)}>
                Excluir<span className="visualmente-oculto"> {endereco.apelido}</span>
              </button>
            </div>
          </li>
        ))}
      </ul>
      {cheio && <p className="texto-apoio">Limite de {dominio.maximoEnderecos} endereços.</p>}
    </>
  );
}

function FormularioEndereco({ endereco, existentes, aoSalvar, aoCancelar }) {
  const { sessao, dominio } = useApp();
  const [dadosMapa, setDadosMapa] = useState(null);
  const [erroMapa, setErroMapa] = useState(null);
  const form = useFormulario({
    apelido: endereco?.apelido ?? "",
    tipo: endereco?.tipo ?? "",
    local: endereco?.local ?? null,
  });

  // A busca de ruas e bairros usa o mesmo mapa da tela de rotas (carregado uma vez só)
  useEffect(() => {
    carregarMapa().then(setDadosMapa).catch(setErroMapa);
  }, []);

  // Casa e trabalho só podem existir uma vez: esconde o tipo que já está em uso
  const tiposEmUso = new Set(
    existentes.filter((item) => item.id !== endereco?.id).map((item) => item.tipo),
  );
  const tipos = dominio.tiposEndereco.filter((tipo) => !tipo.unico || !tiposEmUso.has(tipo.codigo));

  function escolherTipo(tipo) {
    form.definir("tipo", tipo.codigo);
    // Casa e trabalho já vêm com o apelido preenchido (dá para trocar); os outros pedem um nome próprio
    const apelidosPadrao = dominio.tiposEndereco.map((item) => item.rotulo);
    if (!form.valores.apelido || apelidosPadrao.includes(form.valores.apelido)) {
      form.definir("apelido", tipo.unico ? tipo.rotulo : "");
    }
  }

  async function salvar(evento) {
    evento.preventDefault();
    const ok = await form.enviar(
      (valores) => {
        const validador = criarValidador();
        validador.escolha("tipo", valores.tipo, "Escolha o tipo do endereço.");
        validador.texto("apelido", valores.apelido, "Apelido", 2, 40);
        validador.exigir(Boolean(valores.local), "local", "Escolha a rua, a avenida ou o bairro.");
        return validador.erros;
      },
      (valores) => {
        const corpo = { apelido: valores.apelido.trim(), tipo: valores.tipo, local: valores.local };
        return endereco
          ? api.atualizar(`/cidadaos/${sessao.id}/enderecos/${endereco.id}`, corpo)
          : api.criar(`/cidadaos/${sessao.id}/enderecos`, corpo);
      },
    );
    if (ok) aoSalvar(endereco ? "Endereço atualizado." : "Endereço salvo.");
  }

  return (
    <form className="cartao-perfil endereco-form formulario" onSubmit={salvar} noValidate>
      <h2 className="perfil-subtitulo">{endereco ? `Editar "${endereco.apelido}"` : "Novo endereço"}</h2>
      <Aviso aviso={form.aviso} />

      <fieldset className={`campo tipos-endereco ${form.erros.tipo ? "campo--erro" : ""}`}>
        <legend className="campo__rotulo">Tipo</legend>
        <div className="tipos-endereco__opcoes">
          {tipos.map((tipo) => (
            <label key={tipo.codigo} className="tipo-endereco">
              <input
                type="radio"
                name="tipo-endereco"
                value={tipo.codigo}
                checked={form.valores.tipo === tipo.codigo}
                onChange={() => escolherTipo(tipo)}
              />
              <span>
                <Icone nome={ICONE_ENDERECO[tipo.codigo]} tamanho={18} />
                {tipo.rotulo}
              </span>
            </label>
          ))}
        </div>
        {form.erros.tipo && <p className="campo__erro">{form.erros.tipo}</p>}
      </fieldset>

      <Campo id="endereco-apelido" rotulo="Apelido" erro={form.erros.apelido}>
        <input type="text" maxLength={40} value={form.valores.apelido} onChange={form.alterar("apelido")} />
      </Campo>

      {erroMapa && <Aviso aviso={{ tipo: "erro", texto: "Não foi possível carregar o mapa para buscar o local." }} />}
      <CampoLugar
        id="endereco-local"
        rotulo="Local"
        valor={form.valores.local}
        dados={dadosMapa}
        placeholder={dadosMapa ? "Rua, avenida ou bairro" : "Carregando o mapa…"}
        aoEscolher={(local) => form.definir("local", local)}
        erro={form.erros.local}
      />

      <div className="acoes">
        <button type="submit" className="botao botao--primario" disabled={form.enviando}>
          {form.enviando ? "Salvando…" : "Salvar endereço"}
        </button>
        <button type="button" className="botao botao--discreto" onClick={aoCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
