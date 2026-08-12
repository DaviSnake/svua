/**
 * 🔥 Calcula qué botones de página mostrar en una paginación, para no
 * listar un botón por cada página cuando hay muchas (se ve mal y no
 * aporta nada). Siempre se muestran la primera página, la última, y un
 * rango alrededor de la página actual; el resto se resume con "...".
 *
 * `pageActual` y los valores del arreglo resultante son 0-based, igual
 * que el campo `page` que ya se usa en todos los mantenedores.
 *
 * El valor -1 en el resultado representa un "..." (no es una página
 * real: en el template no debe ser clickeable).
 *
 * Ejemplo (20 páginas, página actual = 10 → "página 11"):
 *   calcularPaginasVisibles(10, 20) → [0, -1, 8, 9, 10, 11, 12, -1, 19]
 *   se muestra como: 1 ... 9 10 11 12 13 ... 20
 */
export function calcularPaginasVisibles(
  pageActual: number,
  totalPages: number,
  delta: number = 2
): number[] {

  if (totalPages <= 0) {
    return [];
  }

  // Si entran todas sin necesidad de resumir, se muestran todas.
  const maxBotonesSinResumen = delta * 2 + 5;
  if (totalPages <= maxBotonesSinResumen) {
    return Array.from({ length: totalPages }, (_, i) => i);
  }

  const paginas: number[] = [];
  const rangoInicio = Math.max(1, pageActual - delta);
  const rangoFin = Math.min(totalPages - 2, pageActual + delta);

  paginas.push(0);

  if (rangoInicio > 1) {
    paginas.push(-1);
  }

  for (let i = rangoInicio; i <= rangoFin; i++) {
    paginas.push(i);
  }

  if (rangoFin < totalPages - 2) {
    paginas.push(-1);
  }

  paginas.push(totalPages - 1);

  return paginas;
}
