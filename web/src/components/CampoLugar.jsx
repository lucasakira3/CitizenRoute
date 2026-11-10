import { useEffect, useId, useRef, useState } from "react";
import { buscarLugares } from "../utils/rotas.js";
import Icone from "./Icone.jsx";

/**
 * Campo de endereço com sugestões (combobox acessível): o cidadão digita o nome de uma rua,
 * avenida ou bairro de São Paulo e escolhe uma das opções. Também pode marcar o ponto no mapa.
 * As sugestões vêm do mapa do OpenStreetMap que já está no projeto: nada é buscado na internet.
 */
export default function CampoLugar({
  id,
  rotulo,
  letra,
  valor,
  dados,
  aoEscolher,
  aoMarcarNoMapa,
  marcandoNoMapa = false,
  erro,
  placeholder,
}) {
  const [texto, setTexto] = useState(valor?.descricao ?? "");
  const [aberto, setAberto] = useState(false);
  const [ativa, setAtiva] = useState(0);
  const idLista = useId();
  const idErro = useId();
  const refCampo = useRef(null);

  // Quando o ponto muda por fora (clique no mapa, inverter, rota aberta), o texto acompanha
  useEffect(() => {
    setTexto(valor?.descricao ?? "");
  }, [valor]);

  const sugestoes = aberto && dados ? buscarLugares(dados, texto) : [];

  function escolher(sugestao) {
    setAberto(false);
    setTexto(sugestao.nome);
    aoEscolher({ descricao: sugestao.nome, x: sugestao.x, y: sugestao.y });
  }

  function aoTeclar(evento) {
    if (!sugestoes.length) {
      if (evento.key === "ArrowDown" && texto.length >= 2) setAberto(true);
      return;
    }
    if (evento.key === "ArrowDown") {
      evento.preventDefault();
      setAtiva((atual) => (atual + 1) % sugestoes.length);
    } else if (evento.key === "ArrowUp") {
      evento.preventDefault();
      setAtiva((atual) => (atual - 1 + sugestoes.length) % sugestoes.length);
    } else if (evento.key === "Enter") {
      evento.preventDefault();
      escolher(sugestoes[ativa]);
    } else if (evento.key === "Escape") {
      setAberto(false);
    }
  }

  return (
    <div className={`campo-lugar ${erro ? "campo-lugar--erro" : ""}`}>
      <label className="campo-lugar__rotulo" htmlFor={id}>
        <span className={`campo-lugar__letra campo-lugar__letra--${letra}`} aria-hidden="true">
          {letra}
        </span>
        {rotulo}
      </label>
      <div className="campo-lugar__linha">
        <input
          ref={refCampo}
          id={id}
          type="text"
          role="combobox"
          autoComplete="off"
          maxLength={120}
          placeholder={placeholder}
          aria-expanded={sugestoes.length > 0}
          aria-controls={idLista}
          aria-autocomplete="list"
          aria-activedescendant={sugestoes.length ? `${idLista}-${ativa}` : undefined}
          aria-invalid={Boolean(erro)}
          aria-describedby={erro ? idErro : undefined}
          value={texto}
          onChange={(evento) => {
            setTexto(evento.target.value);
            setAtiva(0);
            setAberto(true);
          }}
          onFocus={() => texto.length >= 2 && !valor && setAberto(true)}
          onBlur={() => setTimeout(() => setAberto(false), 150)}
          onKeyDown={aoTeclar}
        />
        <button
          type="button"
          className={`campo-lugar__mapa ${marcandoNoMapa ? "campo-lugar__mapa--ativo" : ""}`}
          aria-pressed={marcandoNoMapa}
          onClick={aoMarcarNoMapa}
          title="Marcar no mapa"
        >
          <Icone nome="ALVO" tamanho={18} />
          <span className="visualmente-oculto">Marcar {rotulo.toLowerCase()} no mapa</span>
        </button>
      </div>
      <ul id={idLista} role="listbox" className="campo-lugar__lista" hidden={!sugestoes.length} aria-label={rotulo}>
        {sugestoes.map((sugestao, indice) => (
          <li
            key={`${sugestao.tipo}-${sugestao.nome}`}
            id={`${idLista}-${indice}`}
            role="option"
            aria-selected={indice === ativa}
            className="campo-lugar__opcao"
            onMouseDown={(evento) => {
              evento.preventDefault();
              escolher(sugestao);
            }}
            onMouseEnter={() => setAtiva(indice)}
          >
            <Icone nome={sugestao.tipo === "bairro" ? "BAIRRO" : "VIA"} tamanho={16} />
            <span className="campo-lugar__nome">{sugestao.nome}</span>
            <span className="campo-lugar__contexto">{sugestao.contexto}</span>
          </li>
        ))}
      </ul>
      {aberto && dados && texto.length >= 2 && !sugestoes.length && (
        <p className="campo-lugar__nada" role="status">
          Nenhuma rua ou bairro com esse nome. Tente outra grafia ou marque no mapa.
        </p>
      )}
      {erro && (
        <p id={idErro} className="campo__erro">
          {erro}
        </p>
      )}
    </div>
  );
}
