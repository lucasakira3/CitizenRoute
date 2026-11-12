import { useState } from "react";
import { useApp } from "../../contexto.js";
import { api } from "../../services/api.js";
import Icone from "../Icone.jsx";

/** Mobilidade › Como me locomovo: um clique escolhe e já salva o modal usado nos alertas. */
export default function Locomocao({ setAviso }) {
  const { sessao, dominio, entrar } = useApp();
  const [salvando, setSalvando] = useState(null);
  const atual = sessao.modal ?? "";

  async function escolher(codigo) {
    if (codigo === atual || salvando !== null) return;
    setSalvando(codigo);
    try {
      // O PUT do cidadão grava nome, e-mail e modal juntos; nome e e-mail seguem os atuais
      const atualizado = await api.atualizar(`/cidadaos/${sessao.id}`, {
        nome: sessao.nome,
        email: sessao.email,
        modal: codigo || undefined,
      });
      entrar(atualizado);
      setAviso({ tipo: "sucesso", texto: "Meio de transporte salvo." });
    } catch (erro) {
      setAviso({ tipo: "erro", texto: erro.message });
    } finally {
      setSalvando(null);
    }
  }

  const opcoes = [
    ...dominio.modais.map((modal) => ({ codigo: modal.codigo, rotulo: modal.rotulo, icone: modal.codigo })),
    { codigo: "", rotulo: "Não informar", icone: "PESSOA" },
  ];

  return (
    <div className="modais-perfil" role="radiogroup" aria-label="Como você se locomove">
      {opcoes.map((opcao) => (
        <button
          key={opcao.codigo || "nenhum"}
          type="button"
          role="radio"
          aria-checked={atual === opcao.codigo}
          className={`modal-perfil ${opcao.codigo ? "" : "modal-perfil--nenhum"}`}
          onClick={() => escolher(opcao.codigo)}
          disabled={salvando !== null}
        >
          <span className="modal-perfil__icone" aria-hidden="true">
            <Icone nome={opcao.icone} tamanho={30} />
          </span>
          <span className="modal-perfil__nome">{salvando === opcao.codigo ? "Salvando…" : opcao.rotulo}</span>
        </button>
      ))}
    </div>
  );
}
