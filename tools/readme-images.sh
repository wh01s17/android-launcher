#!/usr/bin/env bash
# Builds the README banner and screenshot showcases in docs/readme/ from the
# brand logo and the real captures in docs/screenshots/. Needs ImageMagick 7,
# rsvg-convert and optionally optipng. Run it again after recapturing.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

for tool in magick rsvg-convert; do
  command -v "$tool" >/dev/null || { echo "missing $tool" >&2; exit 1; }
done

readonly FONT=core/designsystem/src/main/res/font/departure_mono.otf
readonly SHOTS=docs/screenshots
readonly OUT=docs/readme
# Theme tokens (plan.md §4.2).
readonly BG=#0a0e0f SURFACE=#11161a BORDER=#243038 MUTED=#33454f
readonly FG=#cbd5ce DIM=#748178 BRIGHT=#f3fbee ACCENT=#84c959
# Captures are 1220x2712 (Redmi); CROP keeps only the top rows of each one.
readonly SRC_W=1220 SRC_H=2712 SHOT_W=360 SHOT_R=26 GAP=36 PAD=48 LABEL=22
CROP=$SRC_H

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$OUT"

# panel W H OUT: dark rounded card with a thin border and transparent corners.
panel() {
  magick -size "${1}x${2}" xc:none \
    \( -size "${1}x${2}" gradient:"$SURFACE-$BG" \) \
    \( -size "${1}x${2}" xc:black -fill white \
       -draw "roundrectangle 0,0 $(($1 - 1)),$(($2 - 1)) 36,36" \) \
    -delete 0 -alpha off -compose CopyOpacity -composite -compose Over \
    -fill none -stroke "$BORDER" -strokewidth 2 \
    -draw "roundrectangle 1,1 $(($1 - 2)),$(($2 - 2)) 35,35" "$3"
}

# text STRING COLOR SIZE OUT
text() {
  magick -background none -fill "$2" -font "$FONT" -pointsize "$3" \
    label:"$1" "$4"
}

# shot SRC OUT: screenshot scaled down with rounded corners and a border.
shot() {
  local h=$((SHOT_W * CROP / SRC_W))
  magick "$1" -crop "${SRC_W}x${CROP}+0+0" +repage -resize "${SHOT_W}x${h}!" \
    \( -size "${SHOT_W}x${h}" xc:black -fill white \
       -draw "roundrectangle 0,0 $((SHOT_W - 1)),$((h - 1)) $SHOT_R,$SHOT_R" \) \
    -alpha off -compose CopyOpacity -composite -compose Over \
    -fill none -stroke "$MUTED" -strokewidth 2 \
    -draw "roundrectangle 1,1 $((SHOT_W - 2)),$((h - 2)) $((SHOT_R - 1)),$((SHOT_R - 1))" \
    "$2"
}

# labelled SRC LABEL OUT: "▍label" in the app's module style above a shot.
labelled() {
  shot "$1" "$tmp/s.png"
  text "$2" "$FG" "$LABEL" "$tmp/l.png"
  magick -size 6x$((LABEL + 4)) xc:"$ACCENT" "$tmp/bar.png"
  magick -background none "$tmp/bar.png" -size 12x1 xc:none "$tmp/l.png" \
    -gravity center +append "$tmp/head.png"
  magick -background none "$tmp/head.png" -size 1x18 xc:none "$tmp/s.png" \
    -gravity west -append "$3"
}

# showcase OUT COLS SRC LABEL [SRC LABEL...]: grid of labelled shots on a panel.
showcase() {
  local out=$1 cols=$2 i=0 row=0 rows=()
  shift 2
  local cells=()
  while (($#)); do
    labelled "$SHOTS/$1" "$2" "$tmp/c$i.png"
    cells+=("$tmp/c$i.png" -size "${GAP}x1" xc:none)
    i=$((i + 1))
    shift 2
    if ((i % cols == 0 || $# == 0)); then
      unset 'cells[-1]' 'cells[-1]' 'cells[-1]'
      magick -background none "${cells[@]}" +append "$tmp/r$row.png"
      rows+=("$tmp/r$row.png" -size "1x$GAP" xc:none)
      cells=()
      row=$((row + 1))
    fi
  done
  unset 'rows[-1]' 'rows[-1]' 'rows[-1]'
  magick -background none "${rows[@]}" -append "$tmp/grid.png"
  local w h
  read -r w h < <(magick identify -format '%w %h\n' "$tmp/grid.png")
  panel $((w + 2 * PAD)) $((h + 2 * PAD)) "$tmp/panel.png"
  magick "$tmp/panel.png" "$tmp/grid.png" -gravity center -compose Over \
    -composite "$out"
}

banner() {
  rsvg-convert -w 216 -h 216 theme/wh01s17/brand/logo.svg -o "$tmp/logo.png"
  local parts=() piece color i=0
  for piece in wh:"$BRIGHT" 01:"$ACCENT" s:"$BRIGHT" 17:"$ACCENT" launcher:"$DIM"; do
    color=${piece##*:}
    text "${piece%:*}" "$color" 88 "$tmp/t$i.png"
    # label: trims spaces, so the word gap is an explicit spacer.
    [[ ${piece%:*} == launcher ]] && parts+=(-size 44x1 xc:none)
    parts+=("$tmp/t$i.png")
    i=$((i + 1))
  done
  magick -background none "${parts[@]}" +append "$tmp/title.png"
  text "Hyprland en tu Android" "$FG" 33 "$tmp/tag.png"
  text "workspaces · tiling · barra · menú · atajos" "$DIM" 22 "$tmp/sub.png"
  magick -background none -gravity center "$tmp/logo.png" \
    -size 1x28 xc:none "$tmp/title.png" -size 1x10 xc:none "$tmp/tag.png" \
    -size 1x14 xc:none "$tmp/sub.png" -append "$tmp/stack.png"
  panel 1280 640 "$tmp/panel.png"
  magick "$tmp/panel.png" "$tmp/stack.png" -gravity center -compose Over \
    -composite "$OUT/banner.png"
}

banner
showcase "$OUT/overview.png" 3 \
  home-redmi.png escritorio menu-current-redmi.png menú edit-redmi.png edición
showcase "$OUT/style.png" 3 \
  style-redmi.png estilo palettes-redmi.png paletas \
  widget-themes-redmi.png temáticas
showcase "$OUT/widgets.png" 3 \
  home-redmi.png "reloj · nova" workspace-4-redmi.png "timers · nova" \
  workspace-5-redmi.png "sistema · nova" \
  widgets-hack-clock-redmi.png "reloj · hack" \
  widgets-hack-timers-redmi.png "timers · hack" \
  widgets-hack-system-redmi.png "sistema · hack"
# TIDAL captures share the lower half; keep the bar and the widget.
CROP=1500
showcase "$OUT/tidal.png" 4 \
  tidal-nova-fine-redmi.png "nova · fine" tidal-nova-bars-redmi.png "nova · bars" \
  tidal-nova-mirror-redmi.png "nova · mirror" tidal-nova-curve-redmi.png "nova · curve" \
  tidal-hack-blocks-redmi.png "hack · blocks" tidal-hack-cava-redmi.png "hack · cava" \
  tidal-hack-scope-redmi.png "hack · scope" tidal-hack-hexdump-redmi.png "hack · hexdump"

if command -v optipng >/dev/null; then optipng -quiet -o2 "$OUT"/*.png; fi
ls -l "$OUT"
