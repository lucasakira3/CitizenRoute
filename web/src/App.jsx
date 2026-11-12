import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Layout from "./components/Layout.jsx";
import { AppContext } from "./contexto.js";
import { combinar, useRota } from "./hooks/useRota.js";
import { DOMINIO } from "./utils/dominio.js";
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

  // A cada troca de página: volta ao topo e leva o foco ao conteúdo (ajuda quem usa teclado).
  useEffect(() => {
    if (primeiraRota.current) {
      primeiraRota.current = false;
      return;
    }
    window.scrollTo(0, 0);
    document.getElementById("conteudo")?.focus({ preventScroll: true });
  }, [rota]);

  // Rótulos de cada código (MOTO → "Moto"), montados uma vez a partir de utils/dominio.js
  const rotulos = useMemo(() => {
    const mapa = {};
    ["modais", "tiposOcorrencia", "niveisRisco", "tiposEndereco"].forEach((grupo) => {
      mapa[grupo] = Object.fromEntries(DOMINIO[grupo].map((item) => [item.codigo, item.rotulo]));
    });
    return mapa;
  }, []);

  const contexto = useMemo(
    () => ({
      sessao,
      entrar,
      sair,
      dominio: DOMINIO,
      rotulo: (grupo, codigo) => rotulos[grupo]?.[codigo] ?? codigo ?? "",
    }),
    [sessao, entrar, sair, rotulos],
  );

  const { Pagina, parametros } = resolverRota(rota);

  return (
    <AppContext.Provider value={contexto}>
      <Layout rota={rota}>
        <Pagina key={rota} parametros={parametros} />
      </Layout>
    </AppContext.Provider>
  );
}
