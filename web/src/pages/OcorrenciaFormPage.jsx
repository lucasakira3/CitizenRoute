import { useEffect, useMemo } from "react";
import Campo from "../components/Campo.jsx";
import { Aviso, Carregando, ErroCarga, ExigirPerfil, Vazio } from "../components/Estados.jsx";
import { useApp } from "../contexto.js";
import { useCarregar } from "../hooks/useCarregar.js";
import { useFormulario } from "../hooks/useFormulario.js";
import { navegar } from "../hooks/useRota.js";
import { useTitulo } from "../hooks/useTitulo.js";
import { api } from "../services/api.js";
import { avisarNaProximaPagina } from "../services/aviso.js";
import { GRAVIDADE } from "../utils/rotulos.js";
import { criarValidador } from "../utils/validacao.js";

const MAXIMO_VIAS = 10;
const VIA_VAZIA = { idVia: "", nivelImpacto: "" };

export default function OcorrenciaFormPage({ parametros }) {
  const editando = Boolean(parametros.id);
  useTitulo(editando ? "Editar ocorrência" : "Reportar ocorrência");

  return (
    <>
      <a className="voltar" href={editando ? `#/ocorrencias/${parametros.id}` : "#/ocorrencias"}>
        {editando ? "Voltar para a ocorrência" : "Todas as ocorrências"}
      </a>
      <header className="cabeca-pagina">
        <div>
          <p className="sobretitulo">{editando ? "Corrigir relato" : "Viu algo na via?"}</p>
          <h1>{editando ? "Editar ocorrência" : "Reportar ocorrência"}</h1>
          <p className="lead">
            Conte o que está acontecendo e em quais vias. A gestão pública recebe o relato na hora, e quem monitora essas
            vias passa a ver o risco calculado para o próprio modal.
          </p>
        </div>
      </header>
      <ExigirPerfil
        tipo="CIDADAO"
        motivo="Só cidadãos registram ocorrências. Entre com a sua conta para reportar o que viu."
      >
        <FormularioOcorrencia id={parametros.id} />
      </ExigirPerfil>
    </>
  );
}

function validar(valores) {
  const validador = criarValidador();
  validador.texto("titulo", valores.titulo, "Título", 5, 100);
  validador.escolha("tipo", valores.tipo, "Escolha o tipo de ocorrência.");
  validador.escolha("gravidade", valores.gravidade, "Escolha a gravidade.");
  validador.texto("descricao", valores.descricao, "Descrição", 10, 500);

  const escolhidas = valores.vias.filter((linha) => linha.idVia);
  if (escolhidas.length === 0) {
    validador.registrar("vias", "Informe pelo menos uma via afetada.");
  }
  if (valores.vias.some((linha) => !linha.idVia || !linha.nivelImpacto)) {
    validador.registrar("vias", "Escolha a via e o nível de impacto em todas as linhas.");
  }
  if (new Set(escolhidas.map((linha) => linha.idVia)).size !== escolhidas.length) {
    validador.registrar("vias", "A mesma via foi escolhida mais de uma vez.");
  }
  return validador.erros;
}

function FormularioOcorrencia({ id }) {
  const { sessao, dominio, rotulo } = useApp();
  const vias = useCarregar(() => api.buscar("/vias"), []);
  const existente = useCarregar(() => (id ? api.buscar(`/ocorrencias/${id}`) : Promise.resolve(null)), [id]);
  const form = useFormulario({ titulo: "", tipo: "", gravidade: "", descricao: "", vias: [VIA_VAZIA] });
  const { setValores } = form;

  useEffect(() => {
    const dados = existente.dados;
    if (dados) {
      setValores({
        titulo: dados.titulo,
        tipo: dados.tipo,
        gravidade: String(dados.gravidade),
        descricao: dados.descricao,
        vias: dados.vias.map((item) => ({ idVia: String(item.via.id), nivelImpacto: item.nivelImpacto })),
      });
    }
  }, [existente.dados, setValores]);

  const viasPorRegiao = useMemo(() => {
    const grupos = new Map();
    (vias.dados ?? []).forEach((via) => {
      const lista = grupos.get(via.regiao.nome) ?? [];
      lista.push(via);
      grupos.set(via.regiao.nome, lista);
    });
    return [...grupos.entries()];
  }, [vias.dados]);

  if (vias.carregando || existente.carregando) {
    return <Carregando />;
  }
  if (vias.erro || existente.erro) {
    return <ErroCarga erro={vias.erro ?? existente.erro} aoTentar={vias.erro ? vias.recarregar : existente.recarregar} />;
  }
  if (existente.dados && (existente.dados.autor.id !== sessao.id || existente.dados.status !== "ABERTA")) {
    return (
      <Vazio titulo="Esta ocorrência não pode mais ser editada.">
        <p>Só quem registrou pode editar, e apenas enquanto a gestão ainda não analisou o relato.</p>
      </Vazio>
    );
  }

  const { valores, erros } = form;
  const tipoEscolhido = dominio.tiposOcorrencia.find((tipo) => tipo.codigo === valores.tipo);

  function alterarVia(indice, campo, valor) {
    form.definir(
      "vias",
      valores.vias.map((linha, i) => (i === indice ? { ...linha, [campo]: valor } : linha)),
    );
  }

  async function enviar(evento) {
    evento.preventDefault();
    let salva;
    const ok = await form.enviar(validar, async (dados) => {
      const corpo = {
        titulo: dados.titulo.trim(),
        tipo: dados.tipo,
        gravidade: Number(dados.gravidade),
        descricao: dados.descricao.trim(),
        vias: dados.vias.map((linha) => ({ idVia: Number(linha.idVia), nivelImpacto: linha.nivelImpacto })),
      };
      salva = id ? await api.atualizar(`/ocorrencias/${id}`, corpo) : await api.criar("/ocorrencias", corpo);
    });
    if (ok) {
      avisarNaProximaPagina(
        "sucesso",
        id ? "Ocorrência atualizada." : "Ocorrência registrada. A gestão pública já pode ver o seu relato.",
      );
      navegar(`/ocorrencias/${salva.id}`);
    }
  }

  return (
    <form className="formulario cartao" onSubmit={enviar} noValidate>
      <Aviso aviso={form.aviso} />

      <Campo id="oc-titulo" rotulo="Título" erro={erros.titulo} dica="Diga o que é e onde, em poucas palavras.">
        <input
          type="text"
          maxLength={100}
          placeholder="Ex.: Alagamento na Radial Leste, altura do Tatuapé"
          value={valores.titulo}
          onChange={form.alterar("titulo")}
        />
      </Campo>

      <Campo
        id="oc-tipo"
        rotulo="Tipo"
        erro={erros.tipo}
        dica={
          tipoEscolhido
            ? `O relato vai para a fila de: ${rotulo("orgaos", tipoEscolhido.orgaoResponsavel)}.`
            : "O tipo define qual órgão público recebe o relato."
        }
      >
        <select value={valores.tipo} onChange={form.alterar("tipo")}>
          <option value="">Escolha o tipo</option>
          {dominio.tiposOcorrencia.map((tipo) => (
            <option key={tipo.codigo} value={tipo.codigo}>
              {tipo.rotulo}
            </option>
          ))}
        </select>
      </Campo>

      <fieldset className={`campo campo--grupo ${erros.gravidade ? "campo--erro" : ""}`}>
        <legend className="campo__rotulo">Gravidade</legend>
        <div className="escala" role="radiogroup" aria-describedby={erros.gravidade ? "oc-gravidade-erro" : undefined}>
          {[1, 2, 3, 4, 5].map((nivel) => (
            <label key={nivel} className="escala__opcao">
              <input
                type="radio"
                name="gravidade"
                value={nivel}
                checked={valores.gravidade === String(nivel)}
                onChange={form.alterar("gravidade")}
                aria-invalid={erros.gravidade ? true : undefined}
              />
              <span className="escala__numero">{nivel}</span>
              <span className="escala__rotulo">{GRAVIDADE[nivel]}</span>
            </label>
          ))}
        </div>
        {erros.gravidade && (
          <p className="campo__erro" id="oc-gravidade-erro">
            {erros.gravidade}
          </p>
        )}
      </fieldset>

      <Campo
        id="oc-descricao"
        rotulo="Descrição"
        erro={erros.descricao}
        dica={`${valores.descricao.length} de 500 caracteres. Diga o que você viu e como isso afeta quem passa.`}
      >
        <textarea rows={4} maxLength={500} value={valores.descricao} onChange={form.alterar("descricao")} />
      </Campo>

      <fieldset className={`campo campo--grupo ${erros.vias ? "campo--erro" : ""}`}>
        <legend className="campo__rotulo">Vias afetadas</legend>
        <p className="campo__dica">Uma ocorrência pode atingir mais de uma via, cada uma de um jeito.</p>
        <ol className="linhas-vias">
          {valores.vias.map((linha, indice) => (
            // eslint-disable-next-line react/no-array-index-key
            <li key={indice} className="linhas-vias__linha">
              <label className="visualmente-oculto" htmlFor={`oc-via-${indice}`}>
                Via {indice + 1}
              </label>
              <select
                id={`oc-via-${indice}`}
                value={linha.idVia}
                onChange={(e) => alterarVia(indice, "idVia", e.target.value)}
                aria-invalid={erros.vias && !linha.idVia ? true : undefined}
              >
                <option value="">Escolha a via</option>
                {viasPorRegiao.map(([regiao, lista]) => (
                  <optgroup key={regiao} label={regiao}>
                    {lista.map((via) => (
                      <option key={via.id} value={via.id}>
                        {via.nome}
                      </option>
                    ))}
                  </optgroup>
                ))}
              </select>
              <label className="visualmente-oculto" htmlFor={`oc-impacto-${indice}`}>
                Impacto na via {indice + 1}
              </label>
              <select
                id={`oc-impacto-${indice}`}
                value={linha.nivelImpacto}
                onChange={(e) => alterarVia(indice, "nivelImpacto", e.target.value)}
                aria-invalid={erros.vias && !linha.nivelImpacto ? true : undefined}
              >
                <option value="">Impacto na via</option>
                {dominio.niveisImpacto.map((nivel) => (
                  <option key={nivel.codigo} value={nivel.codigo}>
                    {nivel.rotulo}
                  </option>
                ))}
              </select>
              {valores.vias.length > 1 && (
                <button
                  type="button"
                  className="botao botao--discreto botao--pequeno"
                  onClick={() =>
                    form.definir(
                      "vias",
                      valores.vias.filter((_, i) => i !== indice),
                    )
                  }
                >
                  Remover<span className="visualmente-oculto"> via {indice + 1}</span>
                </button>
              )}
            </li>
          ))}
        </ol>
        {erros.vias && <p className="campo__erro">{erros.vias}</p>}
        {valores.vias.length < MAXIMO_VIAS && (
          <button
            type="button"
            className="botao botao--secundario botao--pequeno"
            onClick={() => form.definir("vias", [...valores.vias, VIA_VAZIA])}
          >
            Adicionar outra via
          </button>
        )}
      </fieldset>

      <div className="acoes acoes--formulario">
        <button type="submit" className="botao botao--primario" disabled={form.enviando}>
          {form.enviando ? "Enviando…" : id ? "Salvar alterações" : "Reportar ocorrência"}
        </button>
        <a className="botao botao--discreto" href={id ? `#/ocorrencias/${id}` : "#/ocorrencias"}>
          Cancelar
        </a>
      </div>
    </form>
  );
}
