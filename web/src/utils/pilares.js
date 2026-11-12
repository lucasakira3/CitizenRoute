// Os seis pilares da vida na cidade que o Citizen Life reúne, na ordem do menu.
// Mobilidade já funciona; os outros mostram o que vem na próxima versão.

export const PILARES = [
  {
    id: "mobilidade",
    cor: "var(--pilar-mobilidade)",
    nome: "Mobilidade",
    icone: "MOBILIDADE",
    disponivel: true,
    resumo: "Planeje a rota da saída ao destino e veja os riscos do caminho para o seu meio de transporte.",
    consulta: "Rota segura e mapa de riscos",
  },
  {
    id: "educacao",
    perfil: ["Escolas e cursos de interesse", "Idade e série dos filhos"],
    cor: "var(--pilar-educacao)",
    nome: "Educação",
    icone: "EDUCACAO",
    disponivel: false,
    resumo: "Escolas, creches, cursos gratuitos e bibliotecas públicas.",
    consulta: "Vagas e cursos gratuitos",
    planos: [
      "Escolas e creches públicas por bairro",
      "Cursos gratuitos de qualificação com inscrições abertas",
      "Bibliotecas e salas de estudo públicas",
    ],
  },
  {
    id: "alimentacao",
    perfil: ["Preferências e restrições alimentares", "Feiras e mercados favoritos"],
    cor: "var(--pilar-alimentacao)",
    corIcone: "var(--asfalto)",
    nome: "Alimentação",
    icone: "ALIMENTACAO",
    disponivel: false,
    resumo: "Feiras livres, restaurantes populares e bancos de alimentos da sua região.",
    consulta: "Comida acessível perto de você",
    planos: [
      "Feiras livres por dia da semana e endereço",
      "Restaurantes populares com preço e horário",
      "Bancos de alimentos e pontos de doação",
    ],
  },
  {
    id: "seguranca",
    perfil: ["Contatos de emergência", "Bairros que você acompanha"],
    cor: "var(--pilar-seguranca)",
    nome: "Segurança",
    icone: "SEGURANCA",
    disponivel: false,
    resumo: "Delegacias, bases da guarda e pontos de apoio abertos agora.",
    consulta: "Onde pedir ajuda perto de você",
    planos: [
      "Delegacias e bases da guarda municipal mais próximas",
      "Pontos de apoio abertos 24 horas",
      "Telefones de emergência de cada serviço",
    ],
  },
  {
    id: "habitacao",
    perfil: ["Situação de moradia", "Programas em que você se inscreveu"],
    cor: "var(--pilar-habitacao)",
    nome: "Habitação",
    icone: "HABITACAO",
    disponivel: false,
    resumo: "Programas de moradia, aluguel social e regularização de imóveis.",
    consulta: "Programas de moradia para você",
    planos: [
      "Programas habitacionais com regras de inscrição",
      "Aluguel social: quem tem direito e como pedir",
      "Onde regularizar o imóvel e quais documentos levar",
    ],
  },
  {
    id: "lazer",
    perfil: ["Interesses (esporte, cultura, parques)", "Bairros favoritos"],
    cor: "var(--pilar-lazer)",
    nome: "Lazer",
    icone: "LAZER",
    disponivel: false,
    resumo: "Parques, centros culturais e eventos gratuitos perto de você.",
    consulta: "Programação gratuita por região",
    planos: [
      "Parques e praças com horário de funcionamento",
      "Centros culturais, CEUs e bibliotecas com atividades da semana",
      "Eventos gratuitos filtrados pela sua região",
    ],
  },
];

export function pilarPorId(id) {
  return PILARES.find((pilar) => pilar.id === id) ?? null;
}
