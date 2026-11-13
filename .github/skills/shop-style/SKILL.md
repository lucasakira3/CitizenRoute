# Citizen Life — Hub de Apps Design Skill

Use this skill whenever creating a new site, landing page, dashboard, or app section for a product that behaves like a hub of services, not a generic storefront.

This skill should be used as the design source of truth for any interface that aggregates multiple domains like mobility, food, leisure, security, education, and housing. It is optimized for a product that feels useful first, expressive second, and premium without being noisy.

## 1. Fonte de verdade e protocolo de decisão

### Regras obrigatórias

1. Se houver `design.md` no projeto, ele é a fonte de verdade visual.
2. Se não houver `design.md`, use o briefing funcional do produto e a arquitetura por pilares como fonte primária.
3. Nunca cair em padrões genéricos de SaaS, e-commerce, dashboard corporativo, ou template de landing page.
4. Preservar o nível de refinamento do produto original: a IA não pode produzir uma versão simplificada, vazia, ou genérica.
5. Toda decisão visual precisa ser justificada por uma regra do produto, da jornada, ou do sistema de informação.

### Quando o produto é um hub

Um hub de apps não é um catálogo de produtos. Ele é uma superfície de descoberta e ação com múltiplos módulos. O visual deve respeitar isso:

- cada módulo tem identidade funcional, mas o shell visual do hub é compartilhado;
- o usuário precisa perceber rapidamente onde está e o que pode fazer;
- o sistema deve combinar velocidade, clareza e confiança;
- os dados e os alertas precisam ser legíveis sem “poluição visual”.

## 2. Sistema visual base

### A. Filosofia visual

- O hub é utilitário, confiável e funcional.
- O visual deve ser limpo, urbano, moderno e produtivo.
- O design é orientado à tarefa: mapa, rota, alerta, oferta, curso, aluguel, evento.
- O produto deve parecer inteligente, não decorativo.
- O visual deve ser confortável para uso diário e em celular/desktop.

### B. Paleta

Use uma base moderna, clara e sofisticada:

- Fundo principal: `#F4F7F9` ou `#F3F5F7`
- Superfície branca: `#FFFFFF`
- Texto principal: `#101828`
- Texto secundário: `#4B5563`
- Bordas sutis: `#E5E7EB`
- Cinza neutro: `#9CA3AF`
- Verde de confiança: `#0EA5A4`
- Azul digital: `#2563EB`
- Roxo de destaque elegante: `#6D5EF5`
- Laranja de urgência / mobilidade: `#F59E0B`
- Vermelho de alerta: `#EF4444`
- Verde de saúde / bem-estar: `#10B981`
- Preto grafite: `#111827`

Regras:

- A interface deve manter uma base neutra e clara.
- O uso de cor deve seguir a função do módulo, não a decoração.
- Cada pilar pode ter uma cor de identidade, mas o hub precisa ter consistência visual.
- Não usar múltiplas cores saturadas ao mesmo tempo; preferir 1 cor principal + 1 cor de apoio por módulo.

### C. Tipografia

Use uma família sans moderna, com aparência técnica e amigável.

- body e labels: `Inter`, `Segoe UI`, `system-ui`, sans-serif
- headings: a mesma família, com peso médio/semibold
- números: destaque limpo, legível, sem excesso de ornamentação

Escala sugerida:

- caption: 11-12px
- body: 14-16px
- labels: 12-14px
- headings: 20-28px
- hero title: 32-48px

Regras:

- Hierarquia não deve depender de negrito excessivo.
- O texto precisa ser legível, composto e funcional.
- O uso de tracking negativo pode ser aplicado em títulos, mas sem exagero.
- Diferenciar texto por escala, peso e cor, não por “gritaria visual”.

### D. Espaçamento e densidade

- O produto é um hub de utilidade, então o layout precisa ser denso sem ser caótico.
- Use uma escala cuidadosa de espaçamento: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64.
- Espaços entre módulos devem ser claros e consistentes.
- O visual deve ter uma respiração adequada sem ficar “vazio”.

### E. Formas e raios

- cards: 18–24px
- inputs: 14–18px
- pills/chips: 9999px
- botões primários: 14–18px
- mapas e imagens: 16–24px

Regras:

- cantos arredondados são preferidos, mas sem exagero de “bubbles”.
- a forma precisa ajudar a leitura e o agrupamento, não apenas “decorar”.

### F. Sombras e elevação

Usar sombras leves e funcionais:

- sombra mínima: `0 2px 8px rgba(15, 23, 42, 0.06)`
- sombra média: `0 8px 18px rgba(15, 23, 42, 0.08)`
- sombra de destaque: `0 16px 30px rgba(37, 99, 235, 0.12)`

Regras:

- sombras ajudam a separar camadas, não a “ornamentar” a interface.
- evite brilho artificial, highlights excessivos ou bordas pesadas.

## 3. Estrutura do hub

### Layout base

- shell principal com header, navegação lateral ou superior, e área de conteúdo;
- cada módulo deve ter a sua própria identidade, mas o container geral precisa ser consistente;
- nav principal agrupando todos os pilares do produto;
- espaço central para conteúdo principal e micro-dados;
- cards funcionando como atalhos rápidos para ações e informações.

### Organização por pilares

A arquitetura da experiência precisa refletir a realidade do produto:

- Mobilidade: rota, risco, mapa, alertas
- Alimentação: ofertas, promoções, restaurantes/baratos, mapas
- Lazer: eventos, agenda, detalhes, localização
- Segurança: alertas, regiões, policiamento, medidas de cautela
- Educação: cursos, vestibulares, oportunidades, detalhes
- Habitação: aluguel, moradia social, ajuda, obras, mapa

Nenhum módulo pode ser tratado como um “card genérico” sem relação com a intenção do usuário.

## 4. Regras por categoria funcional

### Mobilidade

- priorizar mapa + rota como narrativa principal;
- o risco precisa aparecer com forte sinalização visual, sem depender apenas de texto;
- usar cor de alerta para perigo, mas sem exagerar o impacto visual;
- cards com origem/destino, tempo e risco aparecem antes de detalhes longos.

### Alimentação

- priorizar ofertas, preços e proximidade;
- promoções e “bom prato” devem ter destaque visual, mas em ordem de utilidade;
- usar imagens e badges de preço/tempo sem sujar a tela;
- a informação geográfica e de custo deve ser sempre legível.

### Lazer

- uso de calendário/agenda em primeiro plano;
- eventos e detalhes precisam ter hierarquia clara: nome, data, local, valor, categoria;
- mapa deve ser apoio, não centro da experiência, salvo em telas de detalhe.

### Segurança

- alertas e riscos precisam ser claros, confiáveis e acionáveis;
- usa sinais visuais imediatos: vermelho/amarelo/verde para status;
- a sensação é de controle, prevenção e orientação.

### Educação

- priorizar listas e cards de cursos/ofertas;
- detalhes precisam ser claros: carga horária, público, nível, preço, data;
- a experiência pode ter visual mais “escolar” sem perder uma aparência digital moderna.

### Habitação

- mapa e localização são essenciais;
- texto de valor, custo e disponibilidade precisa ser legível em primeira vista;
- a interface deve ser prática, com filtros e comparações imediatas.

## 5. Princípios reutilizáveis

### Princípio 1: utilidade sobre estética
O design não existe para impressionar; ele existe para orientar decisões e reduzir fricção.

### Princípio 2: consistência do shell, diferenciação dos módulos
O hub deve ter uma identidade visual comum, mas cada pilar pode ter uma cor e uma linguagem funcional próprias.

### Princípio 3: clareza antes de riqueza visual
A interface precisa ser inteligível, rápida de ler e funcional em torno do dado.

### Princípio 4: mapa, lista e card em combinação
Para um hub de serviços, a combinação de mapa + lista + cartão de detalhe é mais útil do que um único padrão visual rígido.

### Princípio 5: cor como sinal, não como decoração
Cor será usada para comunicar status, prioridade e contexto, nunca para “enfeitar” a UI.

### Princípio 6: hierarquia funcional
A informação mais útil deve estar acima do fold e sempre em destaque imediato.

### Princípio 7: refinamento mínimo, mas sem excessos
A interface precisa parecer premium e bem pensada, mas sem peso visual, bordas pesadas, ou excesso de elementos.

## 6. Filtro anti-genérico

Antes de aceitar qualquer decisão visual, verificar:

- ela serve ao usuário real do hub ou é só “bonita”?
- ela respeita a arquitetura por pilares ou está transformando tudo em uma landing page genérica?
- ela usa cor para sinalizar contexto funcional ou para decorar?
- ela sobrecarrega a tela com elementos que não ajudam a decisão?
- ela cria um visual de loja, dashboard corporativo, ou template genérico em vez de um hub de serviços?
- ela ignora a importância do mapa, do alerta, do preço, do local ou do detalhe funcional?

Se a resposta for sim para qualquer uma dessas perguntas, a decisão deve ser rejeitada.

## 7. Checklist de validação

### Estrutura
- [ ] O produto foi pensado como hub e não como loja ou landing page.
- [ ] Os pilares e funções estão claramente organizados.
- [ ] A navegação é clara e disponível em todos os módulos principais.

### Visual
- [ ] O shell visual é consistente entre módulos.
- [ ] Cada módulo tem identidade funcional clara sem quebrar a linguagem geral.
- [ ] O uso de cor está relacionado à ação, ao status ou ao contexto.
- [ ] A interface não usa excesso de saturação ou efeito decorativo.

### Legibilidade
- [ ] Texto principal e secundário têm contraste suficiente.
- [ ] Cards, listas, alertas e mapas são legíveis sem esforço.
- [ ] Informações críticas aparecem em destaque.

### Funcionalidade
- [ ] As ações do usuário estão alinhadas com o problema do módulo.
- [ ] Onde há mapa, ele ajuda a decisão; onde há lista, ela é útil; onde há detalhe, ele traz contexto.
- [ ] O fluxo de navegação é intuitivo e sem fricção.

### Refinamento
- [ ] O resultado parece pensado e útil, não genérico.
- [ ] A interface cobre a complexidade real do produto sem ficar bagunçada.
- [ ] O design admite expansão para novos módulos sem perder consistência.

## 8. Prompt padrão para IA

> Crie uma interface para um hub de apps e serviços, com um shell visual consistente e múltiplos pilares funcionais. A tela deve priorizar clareza, utilidade e ação do usuário. Use uma base neutra, moderna e com espaço para indicadores, mapas, listas e cards de detalhe. Respeite a identidade de cada módulo (mobilidade, alimentação, lazer, segurança, educação, habitação), mas mantenha uma linguagem visual compartilhada no hub. Evite padrões genéricos de e-commerce, dashboard corporate ou landing page. O design deve parecer útil, confiável e prático, com mapas, alertas, ofertas, listas, filtros e resumos guiados por jornada real.

## 9. Execução prática

Antes de escrever o código ou o layout:

1. identificar o módulo principal e o objetivo do usuário;
2. mapear quais dados são essenciais e prioritários;
3. decidir se a tela precisa de listagem, card de detalhe, filtro, mapa ou alerta;
4. construir o visual em torno da ação real, não da estética vazia;
5. validar contra a checklist antes de finalizar.

This skill is designed for hub-shaped products and experiences like Citizen Life: multi-domain, information-rich, action-oriented, and useful by default.
