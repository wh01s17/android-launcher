#!/usr/bin/env bash
# Fails when a tracked text file exceeds MAX lines without a justified entry
# in tools/file-length.allow (R1, R1a); warns above WARN for code files (R2).
set -euo pipefail
MAX=${MAX:-1000}; WARN=${WARN:-600}; status=0
cd "$(git rev-parse --show-toplevel)"
trim() { sed 's/^[[:space:]]*//;s/[[:space:]]*$//' <<<"$1"; }

declare -A allow=()
while IFS='|' read -r path reason; do
  path=$(trim "$path"); reason=$(trim "${reason:-}")
  [[ -z "$path" || "$path" == \#* ]] && continue
  if (( ${#reason} < 20 )); then
    echo "ERROR file-length.allow: $path needs a justification (R1a)"; status=1
  fi
  allow[$path]=1
done < tools/file-length.allow

mapfile -t files < <(git ls-files --cached --others --exclude-standard \
  | grep -vEf tools/file-length.exclude || true)
for f in "${files[@]}"; do
  [[ -f "$f" ]] && grep -Iq . "$f" 2>/dev/null || continue  # skip binaries
  n=$(wc -l < "$f")
  if (( n > MAX )); then
    if [[ -n "${allow[$f]:-}" ]]; then echo "INFO  $f: $n lines (justified)"
    else echo "ERROR $f: $n > $MAX lines; split it or justify it (R1a)"; status=1; fi
  elif (( n > WARN )) && [[ "$f" =~ \.(kt|kts|py|sh)$ ]]; then
    echo "WARN  $f: $n > $WARN lines"
  fi
done

for p in "${!allow[@]}"; do
  if [[ ! -f "$p" ]] || (( $(wc -l < "$p") <= MAX )); then
    echo "WARN  file-length.allow: $p no longer needs an exception"
  fi
done
exit $status
