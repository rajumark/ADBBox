#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

rm -f "$SCRIPT_DIR"/project_files_map_*.txt

DATE=$(date '+%d_%B_%Y' | tr 'A-Z' 'a-z')
OUTPUT_FILE="$SCRIPT_DIR/project_files_map_${DATE}.txt"

print_tree() {
    local dir="$1"
    local prefix="$2"
    local entries=()
    while IFS= read -r -d '' e; do
        entries+=("$e")
    done < <(find "$dir" -maxdepth 1 -mindepth 1 ! -name '.git' ! -name '.*' -print0 | sort -z)
    local count=${#entries[@]}
    local i=0
    for entry in "${entries[@]}"; do
        ((i++))
        local name=$(basename "$entry")
        if [ "$i" -eq "$count" ]; then
            echo "${prefix}└── ${name}"
            ext="    "
        else
            echo "${prefix}├── ${name}"
            ext="│   "
        fi
        if [ -d "$entry" ]; then
            print_tree "$entry" "${prefix}${ext}"
        fi
    done
}

{
    echo "Project File Structure"
    echo "Generated: $(date '+%d %B %Y')"
    echo "========================================"
    echo ""
    echo "."
    print_tree "$PROJECT_DIR" ""
} > "$OUTPUT_FILE"

echo "Created: $OUTPUT_FILE"
