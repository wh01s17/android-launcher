#!/usr/bin/env bash
# R6 (plan.md §1): colores y dp sólo salen de los tokens del tema. Fuera de :core:designsystem
# no puede haber `Color(0x…)`, `#RRGGBB` ni `<número>.dp` en código ni en recursos.
# Una excepción puntual se marca con `r6:allow` en la misma línea, con su motivo. Los tests
# (`src/test`, `src/androidTest`) quedan fuera: sus colores son datos de prueba (config de usuario
# de ejemplo, valores esperados), no estilo.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

status=0
# Código y recursos de los módulos que consumen el tema (build-logic y designsystem quedan fuera).
mapfile -t files < <(git ls-files --cached --others --exclude-standard -- \
  'app/**/*.kt' 'app/**/*.xml' 'core/**/*.kt' 'core/**/*.xml' 'feature/**/*.kt' 'feature/**/*.xml' \
  | grep -v '^core/designsystem/' | grep -v '/build/' | grep -vE '/src/(test|androidTest)/' || true)

check() { # patrón, descripción
  local hits
  hits=$(grep -nE "$1" "${files[@]}" 2>/dev/null | grep -v 'r6:allow' || true)
  if [[ -n "$hits" ]]; then
    echo "ERROR R6: $2 fuera de :core:designsystem:"
    echo "$hits" | sed 's/^/  /'
    status=1
  fi
}

if ((${#files[@]})); then
  check 'Color\(0[xX]' 'Color(0x...)'
  check '#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?\b' 'color hexadecimal'
  check '\b[0-9]+(\.[0-9]+)?\.dp\b' 'dp literal'
fi
exit $status
