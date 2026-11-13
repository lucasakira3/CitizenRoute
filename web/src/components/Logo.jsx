// Marca do Citizen Life: um símbolo simples em branco dentro de um quadrado verde.
// O desenho é o mesmo do favicon (public/favicon.svg).

/** Só o símbolo. Decorativo: quem usa coloca o nome ao lado ou um aria-label no link. */
export function MarcaCidadao({ tamanho = 36, className = "" }) {
  return (
    <svg
      className={`marca-cidadao ${className}`}
      width={tamanho}
      height={tamanho}
      viewBox="0 0 48 48"
      aria-hidden="true"
      focusable="false"
    >
      <rect width="48" height="48" rx="8" fill="var(--placa)" />
      <circle cx="24" cy="13" r="5" fill="#ffffff" />
      <path
        className="marca-cidadao__corpo"
        d="M11 20.5c4.5 4 8.5 5.5 13 5.5s8.5-1.5 13-5.5M24 26v6.5M24 32.5l-5.5 6.5M24 32.5l5.5 6.5"
        fill="none"
        stroke="#ffffff"
        strokeWidth="3.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

/** O bonequinho sem o quadrado, para ilustrações (cor herdada de currentColor). */
export function Cidadao({ tamanho = 96, className = "" }) {
  return (
    <svg
      className={className}
      width={tamanho}
      height={tamanho}
      viewBox="8 6 32 36"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx="24" cy="13" r="5" fill="currentColor" />
      <path
        d="M11 20.5c4.5 4 8.5 5.5 13 5.5s8.5-1.5 13-5.5M24 26v6.5M24 32.5l-5.5 6.5M24 32.5l5.5 6.5"
        fill="none"
        stroke="currentColor"
        strokeWidth="3.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

/** Símbolo + nome, usado no cabeçalho e no rodapé. */
export default function Logo({ tamanho = 36 }) {
  return (
    <span className="logo">
      <MarcaCidadao tamanho={tamanho} />
      <span className="logo__nome">
        Citizen<span className="logo__destaque">Life</span>
      </span>
    </span>
  );
}
