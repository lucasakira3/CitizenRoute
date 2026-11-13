import { useApp } from "../../contexto.js";
import { useCarregar } from "../../hooks/useCarregar.js";
import { api } from "../../services/api.js";
import { Carregando, ErroCarga, Vazio } from "../Estados.jsx";
import Icone from "../Icone.jsx";
import IntroAba from "./IntroAba.jsx";

/** Alimentação › Minhas feiras: feiras favoritadas pelo cidadão. */
export default function MinhasFeiras({ setAviso }) {
  const { sessao } = useApp();
  const favoritas = useCarregar(() => api.buscar(`/cidadaos/${sessao.id}/feiras`), [sessao.id]);
  const todas = useCarregar(() => api.buscar("/feiras"), []);

  async function favoritar(feira) {
    try {
      await api.criar(`/cidadaos/${sessao.id}/feiras`, { idFeira: feira.id });
      favoritas.recarregar();
      setAviso?.({ tipo: "sucesso", texto: `Feira salva em sua lista.` });
    } catch (erro) {
      setAviso?.({ tipo: "erro", texto: erro.message });
    }
  }

  async function desfavoritar(feira) {
    try {
      await api.excluir(`/cidadaos/${sessao.id}/feiras/${feira.id}`);
      favoritas.recarregar();
      setAviso?.({ tipo: "sucesso", texto: `Feira removida da sua lista.` });
    } catch (erro) {
      setAviso?.({ tipo: "erro", texto: erro.message });
    }
  }

  const favoritasMap = new Map((favoritas.dados ?? []).map((feira) => [feira.id, feira]));
  const disponiveis = (todas.dados ?? []).filter((feira) => !favoritasMap.has(feira.id));

  return (
    <>
      <IntroAba>
        <span className="perfil-meta">{favoritas.dados ? favoritas.dados.length : 0} favoritas</span>
      </IntroAba>

      {favoritas.carregando && !favoritas.dados && <Carregando texto="Carregando suas feiras…" />}
      {favoritas.erro && <ErroCarga erro={favoritas.erro} aoTentar={favoritas.recarregar} />}

      {favoritas.dados && favoritas.dados.length === 0 && (
        <Vazio titulo="Você ainda não favoritou uma feira.">
          Escolha abaixo as feiras que você costuma frequentar.
        </Vazio>
      )}

      <ul className="cartoes-perfil">
        {(favoritas.dados ?? []).map((feira) => (
          <li key={feira.id} className="cartao-perfil">
            <div className="rotas-salvas__cabeca">
              <span className="rotas-salvas__nome">{feira.rua}</span>
              <span className="rotas-salvas__livre">{feira.dia}</span>
            </div>
            <p className="rotas-salvas__trajeto">{feira.nome}</p>
            <p className="rotas-salvas__meta">{feira.horario} · {feira.zona}</p>
            <div className="acoes acoes--linha">
              <button type="button" className="botao-texto" onClick={() => desfavoritar(feira)}>
                Remover
              </button>
            </div>
          </li>
        ))}
      </ul>

      <div className="perfil-bloco">
        <h2 className="perfil-subtitulo">Adicionar feiras</h2>
        {todas.carregando && !todas.dados && <Carregando />}
        {todas.erro && <ErroCarga erro={todas.erro} aoTentar={todas.recarregar} />}
        {todas.dados && disponiveis.length === 0 && <Vazio titulo="Todas as feiras já estão na sua lista." />}

        <ul className="rotas-salvas cartoes-perfil">
          {disponiveis.map((feira) => (
            <li key={feira.id} className="cartao-perfil">
              <div className="rotas-salvas__cabeca">
                <span className="rotas-salvas__nome">{feira.rua}</span>
                <span className="rotas-salvas__livre">{feira.dia}</span>
              </div>
              <p className="rotas-salvas__trajeto">{feira.nome}</p>
              <p className="rotas-salvas__meta">{feira.horario} · {feira.zona}</p>
              <div className="acoes acoes--linha">
                <button type="button" className="botao botao--secundario botao--pequeno" onClick={() => favoritar(feira)}>
                  <Icone nome="MAIS" tamanho={16} />
                  Salvar
                </button>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </>
  );
}
