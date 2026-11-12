import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Layout from "./components/Layout.jsx";
import { Carregando, ErroCarga } from "./components/Estados.jsx";
import { AppContext } from "./contexto.js";
import { combinar, useRota } from "./hooks/useRota.js";
import { api, definirAoSessaoInvalida, lerSessao, salvarSessao } from "./services/api.js";
import EmBrevePage from "./pages/EmBrevePage.jsx";
import EntrarPage from "./pages/EntrarPage.jsx";
import InicioPage from "./pages/InicioPage.jsx";
import MapaPage from "./pages/MapaPage.jsx";
import MobilidadePage from "./pages/MobilidadePage.jsx";
import NaoEncontradaPage from "./pages/NaoEncontradaPage.jsx";
import PerfilPage from "./pages/PerfilPage.jsx";
import PlanejarRotaPage from "./pages/PlanejarRotaPage.jsx";

// Os caminhos fixos vêm antes de "/:pilar", que atende os pilares ainda em construção.
const ROTAS = [
  ["/", InicioPage],
  ["/mobilidade", MobilidadePage],
  ["/mobilidade/rota", PlanejarRotaPage],
  ["/mobilidade/mapa", MapaPage],
  ["/perfil", PerfilPage],
  ["/perfil/:secao", PerfilPage],
  ["/perfil/:pilar/:secao", PerfilPage],
  ["/entrar", EntrarPage],
  ["/:pilar", EmBrevePage],
];

function resolverRota(rota) {
  for (const [padrao, Pagina] of ROTAS) {
    const parametros = combinar(padrao, rota);
    if (parametros) {
      return { Pagina, parametros };
    }
  }
  return { Pagina: NaoEncontradaPage, parametros: {} };
}

export default function App() {
  const rota = useRota();
  const [sessao, setSessao] = useState(lerSessao);
  const [dominio, setDominio] = useState(null);
  const [erroDominio, setErroDominio] = useState(null);
  const [tentativa, setTentativa] = useState(0);
  const primeiraRota = useRef(true);

  const entrar = useCallback((usuario) => {
    salvarSessao(usuario);
    setSessao(usuario);
  }, []);

  const sair = useCallback(() => {
    salvarSessao(null);
    setSessao(null);
  }, []);

  // Se a API responder 401 (usuário excluído, por exemplo), a sessão local é descartada.
  useEffect(() => {
    definirAoSessaoInvalida(sair);
  }, [sair]);

  // Confere no servidor se a sessão guardada no navegador ainda vale.
  useEffect(() => {
    if (lerSessao()) {
      api
        .buscar("/auth/sessao")
        .then(entrar)
        .catch(() => {});
    }
  }, [entrar]);

  // Opções e rótulos (modais, tipos, status...) vêm do Java: uma única fonte das regras.
  useEffect(() => {
    setErroDominio(null);
    api.buscar("/dominio").then(setDominio).catch(setErroDominio);
  }, [tentativa]);

  // A cada troca de página: volta ao topo e leva o foco ao conteúdo (ajuda quem usa teclado).
  useEffect(() => {
    if (primeiraRota.current) {
      primeiraRota.current = false;
      return;
    }
    window.scrollTo(0, 0);
    document.getElementById("conteudo")?.focus({ preventScroll: true });
  }, [rota]);

  const rotulos = useMemo(() => {
    if (!dominio) {
      return {};
    }
    const mapa = {};
    ["modais", "tiposOcorrencia", "status", "niveisImpacto", "niveisRisco", "criteriosRota", "tiposEndereco"].forEach((grupo) => {
      mapa[grupo] = Object.fromEntries(dominio[grupo].map((item) => [item.codigo, item.rotulo]));
    });
    return mapa;
  }, [dominio]);

  const contexto = useMemo(
    () => ({
      sessao,
      entrar,
      sair,
      dominio,
      rotulo: (grupo, codigo) => rotulos[grupo]?.[codigo] ?? codigo ?? "",
    }),
    [sessao, entrar, sair, dominio, rotulos],
  );

  const { Pagina, parametros } = resolverRota(rota);

  let conteudo;
  if (dominio) {
    conteudo = <Pagina key={rota} parametros={parametros} />;
  } else if (erroDominio) {
    conteudo = (
      <section className="secao">
        <h1>Não foi possível abrir o Citizen Life</h1>
        <ErroCarga erro={erroDominio} aoTentar={() => setTentativa((n) => n + 1)} />
      </section>
    );
  } else {
    conteudo = <Carregando texto="Abrindo o Citizen Life…" />;
  }

  return (
    <AppContext.Provider value={contexto}>
      <Layout rota={rota}>{conteudo}</Layout>
    </AppContext.Provider>
  );
}
