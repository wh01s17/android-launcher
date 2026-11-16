#!/usr/bin/env bash
# Builds the README banner and screenshot showcases in docs/readme/ from the
# brand logo and the real captures in docs/screenshots/. Each image has a
# transparent background in a -dark and a -light variant; the README picks one
# with <picture> and prefers-color-scheme. Needs ImageMagick 7, rsvg-convert
# and optionally optipng. Run it again after recapturing.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

for tool in magick rsvg-convert; do
  command -v "$tool" >/dev/null || { echo "missing $tool" >&2; exit 1; }
done

readonly FONT=core/designsystem/src/main/res/font/departure_mono.otf
readonly SHOTS=docs/screenshots
readonly OUT=docs/readme
# Captures are 1220x2712 (Redmi); CROP keeps only the top rows of each one.
readonly SRC_W=1220 SRC_H=2712 SHOT_W=360 SHOT_R=28 GAP_X=40 GAP_Y=56
readonly CAPTION=22 WIDTH=1280
CROP=$SRC_H

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$OUT"

# use_theme dark|light: text colours for the page behind the image. Dark uses
# the wh01s17 tokens (plan.md §4.2); light darkens them to keep contrast on
# white. EDGE matches GitHub's own border colour in each theme.
use_theme() {
  THEME=$1
  if [[ $THEME == dark ]]; then
    STRONG=#f3fbee TEXT=#cbd5ce DIM=#748178 ACCENT=#84c959 EDGE=#30363d
  else
    STRONG=#11161a TEXT=#2a343a DIM=#6b7a72 ACCENT=#4e8f2a EDGE=#d0d7de
  fi
}

# text STRING COLOR SIZE OUT
text() {
  magick -background none -fill "$2" -font "$FONT" -pointsize "$3" \
    label:"$1" "$4"
}

# row OUT GAP IMAGE...: images side by side, centred vertically.
row() {
  local out=$1 gap=$2 args=()
  shift 2
  for img in "$@"; do args+=("$img" -size "${gap}x1" xc:none); done
  unset 'args[-1]' 'args[-1]' 'args[-1]'
  magick -background none -gravity center "${args[@]}" +append +repage "$out"
}

# column OUT GAP IMAGE...: images stacked, centred horizontally.
column() {
  local out=$1 gap=$2 args=()
  shift 2
  for img in "$@"; do args+=("$img" -size "1x${gap}" xc:none); done
  unset 'args[-1]' 'args[-1]' 'args[-1]'
  magick -background none -gravity center "${args[@]}" -append +repage "$out"
}

# shot SRC OUT: screenshot scaled down with rounded corners and a hairline.
shot() {
  local h=$((SHOT_W * CROP / SRC_W))
  magick "$1" -crop "${SRC_W}x${CROP}+0+0" +repage -resize "${SHOT_W}x${h}!" \
    \( -size "${SHOT_W}x${h}" xc:black -fill white \
       -draw "roundrectangle 0,0 $((SHOT_W - 1)),$((h - 1)) $SHOT_R,$SHOT_R" \) \
    -alpha off -compose CopyOpacity -composite -compose Over \
    -fill none -stroke "$EDGE" -strokewidth 2 \
    -draw "roundrectangle 1,1 $((SHOT_W - 2)),$((h - 2)) $((SHOT_R - 1)),$((SHOT_R - 1))" \
    "$2"
}

# captioned SRC "name|detail" OUT: shot with a centred two-tone caption below.
captioned() {
  local name=${2%%|*} detail=
  [[ $2 == *"|"* ]] && detail=${2#*|}
  shot "$1" "$tmp/s.png"
  text "$name" "$TEXT" "$CAPTION" "$tmp/n.png"
  if [[ -n $detail ]]; then
    text "$detail" "$DIM" "$CAPTION" "$tmp/d.png"
    row "$tmp/cap.png" 14 "$tmp/n.png" "$tmp/d.png"
  else
    cp "$tmp/n.png" "$tmp/cap.png"
  fi
  column "$3" 20 "$tmp/s.png" "$tmp/cap.png"
}

# showcase NAME COLS SRC CAPTION [SRC CAPTION...]: grid of captioned shots.
showcase() {
  local name=$1 cols=$2 i=0 cells=() rows=()
  shift 2
  while (($#)); do
    captioned "$SHOTS/$1" "$2" "$tmp/c$i.png"
    cells+=("$tmp/c$i.png")
    i=$((i + 1))
    shift 2
    if ((${#cells[@]} == cols || $# == 0)); then
      row "$tmp/r${#rows[@]}.png" "$GAP_X" "${cells[@]}"
      rows+=("$tmp/r${#rows[@]}.png")
      cells=()
    fi
  done
  column "$tmp/grid.png" "$GAP_Y" "${rows[@]}"
  magick "$tmp/grid.png" -bordercolor none -border 8 \
    "$OUT/$name-$THEME.png"
}

banner() {
  rsvg-convert -w 200 -h 200 theme/wh01s17/brand/logo.svg -o "$tmp/logo.png"
  text wh "$STRONG" 88 "$tmp/t0.png"
  text 01 "$ACCENT" 88 "$tmp/t1.png"
  text s "$STRONG" 88 "$tmp/t2.png"
  text 17 "$ACCENT" 88 "$tmp/t3.png"
  text launcher "$DIM" 88 "$tmp/t4.png"
  row "$tmp/word.png" 0 "$tmp/t0.png" "$tmp/t1.png" "$tmp/t2.png" "$tmp/t3.png"
  # label: trims spaces, so the word gap is an explicit spacer.
  row "$tmp/title.png" 48 "$tmp/word.png" "$tmp/t4.png"
  text "Hyprland en tu Android" "$TEXT" 33 "$tmp/tag.png"
  magick "$tmp/logo.png" -gravity center -background none \
    -extent "$WIDTH"x200 "$tmp/logo-wide.png"
  column "$tmp/stack.png" 28 "$tmp/logo-wide.png" "$tmp/title.png"
  column "$tmp/stack2.png" 18 "$tmp/stack.png" "$tmp/tag.png"
  magick "$tmp/stack2.png" -bordercolor none -border 0x24 \
    "$OUT/banner-$THEME.png"
}

render() {
  use_theme "$1"
  CROP=$SRC_H
  banner
  showcase overview 3 \
    home-redmi.png escritorio menu-current-redmi.png menú edit-redmi.png edición
  showcase style 3 \
    style-redmi.png estilo palettes-redmi.png paletas \
    widget-themes-redmi.png temáticas
  showcase widgets 3 \
    home-redmi.png "reloj|nova" workspace-4-redmi.png "timers|nova" \
    workspace-5-redmi.png "sistema|nova" \
    widgets-hack-clock-redmi.png "reloj|hack" \
    widgets-hack-timers-redmi.png "timers|hack" \
    widgets-hack-system-redmi.png "sistema|hack"
  # TIDAL captures share the lower half; keep the bar and the widget.
  CROP=1500
  showcase tidal 4 \
    tidal-nova-fine-redmi.png "fine|nova" tidal-nova-bars-redmi.png "bars|nova" \
    tidal-nova-mirror-redmi.png "mirror|nova" tidal-nova-curve-redmi.png "curve|nova" \
    tidal-hack-blocks-redmi.png "blocks|hack" tidal-hack-cava-redmi.png "cava|hack" \
    tidal-hack-scope-redmi.png "scope|hack" tidal-hack-hexdump-redmi.png "hexdump|hack"
}

rm -f "$OUT"/*.png
render dark
render light
if command -v optipng >/dev/null; then optipng -quiet -o2 "$OUT"/*.png; fi
ls -l "$OUT"
