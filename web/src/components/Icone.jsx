// Ícones desenhados para o CitizenRoute (SVG próprio, sem biblioteca).
// Grade de 24 × 24, traço de 2 px e pontas arredondadas, como sinalização de trânsito.

const DESENHOS = {
  // ---------------------------------------------------------------- modais
  CARRO: (
    <>
      <path d="M5 11l1.6-4.2A2 2 0 0 1 8.5 5.5h7a2 2 0 0 1 1.9 1.3L19 11" />
      <rect x="3" y="11" width="18" height="6" rx="2" />
      <path d="M6 17v2.5M18 17v2.5" />
      <circle cx="7.5" cy="14" r="1" />
      <circle cx="16.5" cy="14" r="1" />
    </>
  ),
  MOTO: (
    <>
      <circle cx="5.5" cy="16" r="3" />
      <circle cx="18.5" cy="16" r="3" />
      <path d="M5.5 16l3.5-6h5l4.5 6" />
      <path d="M14 10l1.5-3.5H18" />
      <path d="M8 10h3.5" />
    </>
  ),
  ONIBUS: (
    <>
      <rect x="4.5" y="3" width="15" height="15" rx="2.5" />
      <path d="M4.5 11h15" />
      <path d="M8 7h8" />
      <circle cx="8" cy="14.5" r="1" />
      <circle cx="16" cy="14.5" r="1" />
      <path d="M7.5 18v2.5M16.5 18v2.5" />
    </>
  ),
  APP: (
    <>
      <rect x="6" y="2" width="12" height="20" rx="2.5" />
      <path d="M12 15.5s-3-2.7-3-5a3 3 0 0 1 6 0c0 2.3-3 5-3 5z" />
      <path d="M11 19h2" />
    </>
  ),
  PEDESTRE: (
    <>
      <circle cx="13" cy="4" r="2" />
      <path d="M13 7.5l-1.5 6 3 3V21" />
      <path d="M11.5 13.5L9 21" />
      <path d="M8 11l3-3.5 3 2.5 2.5 1" />
    </>
  ),
  BIKE: (
    <>
      <circle cx="5.5" cy="16" r="3.5" />
      <circle cx="18.5" cy="16" r="3.5" />
      <path d="M5.5 16L9 9h6l3.5 7" />
      <path d="M9 9l3 7h-6.5" />
      <path d="M8 6.5h3M15 9l-1-3h2.5" />
    </>
  ),

  // ------------------------------------------------------ tipos de ocorrência
  ACIDENTE: (
    <>
      <path d="M2.5 13h5.5M16 13h5.5" />
      <path d="M8 13l-2-2.2M8 13l-2 2.2M16 13l2-2.2M16 13l2 2.2" />
      <path d="M12 5.5V8.5M12 17.5v3M8.6 7.2l1.4 1.5M15.4 7.2L14 8.7M8.6 18.8l1.4-1.5M15.4 18.8L14 17.3" />
    </>
  ),
  ALAGAMENTO: (
    <>
      <path d="M12 2.5s-3 3.4-3 5.6a3 3 0 0 0 6 0c0-2.2-3-5.6-3-5.6z" />
      <path d="M2 15c1.7 0 1.7-1.5 3.3-1.5S7 15 8.7 15s1.6-1.5 3.3-1.5 1.7 1.5 3.3 1.5 1.7-1.5 3.4-1.5S20.3 15 22 15" />
      <path d="M2 19.5c1.7 0 1.7-1.5 3.3-1.5S7 19.5 8.7 19.5s1.6-1.5 3.3-1.5 1.7 1.5 3.3 1.5 1.7-1.5 3.4-1.5 1.6 1.5 3.3 1.5" />
    </>
  ),
  QUEDA_ARVORE: (
    <>
      <path d="M3 21h18" />
      <path d="M6 21l5.5-7.5" />
      <circle cx="15" cy="9.5" r="5" />
      <path d="M13 11.5l2-2" />
    </>
  ),
  SEMAFORO_APAGADO: (
    <>
      <rect x="8" y="2" width="8" height="17" rx="2.5" />
      <circle cx="12" cy="6.5" r="1.4" />
      <circle cx="12" cy="10.5" r="1.4" />
      <circle cx="12" cy="14.5" r="1.4" />
      <path d="M12 19v3" />
      <path d="M4 3.5l16 16" />
    </>
  ),
  DESLIZAMENTO: (
    <>
      <path d="M2 20.5h20" />
      <path d="M2.5 20.5L10 7l4 5.5" />
      <path d="M13 15.5l3 1.5" />
      <circle cx="17.5" cy="18" r="1.4" />
      <circle cx="20.5" cy="16.5" r="1" />
      <circle cx="15.5" cy="13" r="1" />
    </>
  ),
  VIA_BLOQUEADA: (
    <>
      <rect x="2" y="7" width="20" height="6" rx="1" />
      <path d="M7 7l-3.5 6M12.5 7L9 13M18 7l-3.5 6" />
      <path d="M5 13v8M19 13v8" />
    </>
  ),
  INSEGURANCA: (
    <>
      <path d="M12 2.5l7.5 3v5.7c0 4.7-3.2 8.5-7.5 10.3-4.3-1.8-7.5-5.6-7.5-10.3V5.5z" />
      <path d="M12 8v4.5" />
      <path d="M12 16h.01" />
    </>
  ),

  // ------------------------------------------------------- controles do mapa
  MAIS: <path d="M12 5v14M5 12h14" />,
  MENOS: <path d="M5 12h14" />,
  ENQUADRAR: (
    <>
      <path d="M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5" />
      <path d="M12 8.5l3.5 3.5-3.5 3.5-3.5-3.5z" />
    </>
  ),
  TELA_CHEIA: (
    <>
      <path d="M4 9V4h5M4 4l6 6" />
      <path d="M20 15v5h-5M20 20l-6-6" />
    </>
  ),
  MAPA: (
    <>
      <path d="M3 6.5l6-2.5 6 2.5 6-2.5v13.5l-6 2.5-6-2.5-6 2.5z" />
      <path d="M9 4v13.5M15 6.5V20" />
    </>
  ),
  VOLTAR: <path d="M15 5l-7 7 7 7" />,

  // ------------------------------------------------------------------ rotas
  ALVO: (
    <>
      <circle cx="12" cy="12" r="7" />
      <circle cx="12" cy="12" r="2.5" />
      <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
    </>
  ),
  TROCAR: (
    <>
      <path d="M7 4v15M3.5 7.5L7 4l3.5 3.5" />
      <path d="M17 20V5M13.5 16.5L17 20l3.5-3.5" />
    </>
  ),
  VIA: (
    <>
      <path d="M8 3L5 21M16 3l3 18" />
      <path d="M12 4v3M12 10.5v3M12 17v3" />
    </>
  ),
  BAIRRO: (
    <>
      <path d="M4 20V10l5-4 5 4v10" />
      <path d="M14 20v-7l3-2.5 3 2.5v7" />
      <path d="M3 20h18M8 20v-4h2v4" />
    </>
  ),
  ESCUDO: (
    <>
      <path d="M12 3l7 3v5.5c0 4.4-3 7.9-7 9.5-4-1.6-7-5.1-7-9.5V6z" />
      <path d="M9 12l2.2 2.2L15.5 10" />
    </>
  ),
  RELOGIO: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 7.5V12l3 2" />
    </>
  ),
};

/** Ícone decorativo. O texto ao lado é quem dá o significado para leitores de tela. */
export default function Icone({ nome, tamanho = 20, className = "" }) {
  const desenho = DESENHOS[nome];
  if (!desenho) {
    return null;
  }
  return (
    <svg
      className={`icone ${className}`}
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {desenho}
    </svg>
  );
}
