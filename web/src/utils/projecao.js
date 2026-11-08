// Contas da câmera do mapa, iguais às que o CSS faz no plano 3D:
//   perspective(1500px) rotateX(inclinação) scale3d(zoom) rotateZ(giro) translate(-x, -y)
// O canvas do chão e as placas (elementos HTML em CSS 3D) usam a mesma fórmula,
// por isso ficam sempre no mesmo lugar.

export const PERSPECTIVA = 1500;
const RAD = Math.PI / 180;

/**
 * Posição na tela (px, a partir do centro da câmera) de um ponto do plano a uma altura h.
 * k é o fator da perspectiva: maior que 1 perto da câmera, menor que 1 longe.
 */
export function projetar(camera, u, v, h = 0) {
  const dx = u - camera.x;
  const dy = v - camera.y;
  const g = camera.giro * RAD;
  const t = camera.incl * RAD;
  const X = (dx * Math.cos(g) - dy * Math.sin(g)) * camera.zoom;
  const Y = (dx * Math.sin(g) + dy * Math.cos(g)) * camera.zoom;
  const Z = h * camera.zoom;
  const z = Y * Math.sin(t) + Z * Math.cos(t);
  const k = PERSPECTIVA / (PERSPECTIVA - z);
  return { x: X * k, y: (Y * Math.cos(t) - Z * Math.sin(t)) * k, k, atras: PERSPECTIVA - z < 40 };
}

/** Ponto do chão que aparece numa posição da tela (a conta de projetar ao contrário). */
export function desprojetar(camera, sx, sy) {
  const t = camera.incl * RAD;
  const g = camera.giro * RAD;
  // Acima do horizonte não existe chão: limita o raio para não dividir por zero
  const minimo = -PERSPECTIVA * Math.cos(t) * 0.85;
  const syLimitado = Math.sin(t) > 0 ? Math.max(sy, minimo / Math.sin(t)) : sy;
  const Y = (syLimitado * PERSPECTIVA) / (PERSPECTIVA * Math.cos(t) + syLimitado * Math.sin(t));
  const X = (sx * (PERSPECTIVA - Y * Math.sin(t))) / PERSPECTIVA;
  const x1 = X / camera.zoom;
  const y1 = Y / camera.zoom;
  return {
    x: camera.x + x1 * Math.cos(g) + y1 * Math.sin(g),
    y: camera.y - x1 * Math.sin(g) + y1 * Math.cos(g),
  };
}

/**
 * Versão rápida de projetar para desenhar milhares de pontos por quadro: calcula
 * senos e cossenos uma vez e devolve uma função que escreve o resultado em "saida".
 */
export function criarProjecao(camera, ox, oy) {
  const g = camera.giro * RAD;
  const t = camera.incl * RAD;
  const cg = Math.cos(g);
  const sg = Math.sin(g);
  const ct = Math.cos(t);
  const st = Math.sin(t);
  const { x: cx, y: cy, zoom } = camera;
  const saida = { x: 0, y: 0, k: 1, ok: true };
  const projetarRapido = (u, v, h = 0) => {
    const dx = u - cx;
    const dy = v - cy;
    const X = (dx * cg - dy * sg) * zoom;
    const Y = (dx * sg + dy * cg) * zoom;
    const Z = h * zoom;
    const z = Y * st + Z * ct;
    const k = PERSPECTIVA / (PERSPECTIVA - z);
    saida.ok = PERSPECTIVA - z > 40;
    saida.k = k;
    saida.x = ox + X * k;
    saida.y = oy + (Y * ct - Z * st) * k;
    return saida;
  };
  /** Profundidade (quanto maior, mais perto da câmera), para ordenar os prédios. */
  const profundidade = (u, v) => (u - cx) * sg + (v - cy) * cg;
  /** Direção (no chão) que aponta para a câmera: paredes viradas para ela são as visíveis. */
  const paraCamera = { x: sg, y: cg };
  return { projetar: projetarRapido, profundidade, paraCamera, saida };
}
