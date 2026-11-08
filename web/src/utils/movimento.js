/** Verdadeiro quando a pessoa pediu ao sistema operacional para reduzir animações. */
export function prefereMenosMovimento() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}
