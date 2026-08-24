#!/usr/bin/env bash
# Bytecode lookup helper for the decompiled-free Hytale server mirror in
# reference/server (37k .class files, no sources). Wraps javap so callers do
# not have to resolve the bundled JDK path, hunt for a class file, or page
# through a 900-line disassembly to reach one method.
#
#   hytale-class.sh find <symbol>              which classes mention <symbol>
#   hytale-class.sh sig  <Class>               member signatures (javap -p)
#   hytale-class.sh code <Class> [member]      bytecode (javap -p -c), one method if given
#
# <Class> accepts a bare name (BlockType), an inner class (BlockModule$BlockStateInfo),
# a fully qualified name, or a path relative to reference/server.
set -uo pipefail

repo_root="$(cd "$(dirname "$0")/../../../.." && pwd)"
server_dir="$repo_root/reference/server"

if [[ ! -d "$server_dir" ]]; then
    echo "reference/server not found. It is a gitignored local mirror - see CLAUDE.md." >&2
    exit 1
fi

if [[ -z "${JAVA_HOME:-}" && -d "$repo_root/jdk-25.0.2" ]]; then
    JAVA_HOME="$repo_root/jdk-25.0.2"
fi
javap="$JAVA_HOME/bin/javap"
[[ -x "$javap" ]] || javap="$JAVA_HOME/bin/javap.exe"
if [[ ! -x "$javap" ]]; then
    echo "javap not found under JAVA_HOME ($JAVA_HOME). javap is not on PATH in this environment." >&2
    exit 1
fi

usage() {
    sed -n '5,12p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
}

# Resolves a class argument to a path relative to reference/server.
resolve_class() {
    local wanted="$1"
    if [[ "$wanted" == *.class && -f "$server_dir/$wanted" ]]; then
        printf '%s\n' "$wanted"
        return 0
    fi
    if [[ "$wanted" == */* && -f "$server_dir/${wanted}.class" ]]; then
        printf '%s\n' "${wanted}.class"
        return 0
    fi
    # Fully qualified dotted name.
    local dotted="${wanted//./\/}"
    if [[ -f "$server_dir/${dotted}.class" ]]; then
        printf '%s\n' "${dotted}.class"
        return 0
    fi
    local base="${wanted##*.}"
    local matches
    matches="$(cd "$server_dir" && find . -name "${base}.class" -printf '%P\n' 2>/dev/null)"
    local count
    count="$(printf '%s' "$matches" | grep -c . || true)"
    if [[ "$count" -eq 0 ]]; then
        echo "No class file named '${base}.class' under reference/server." >&2
        echo "Try: $(basename "$0") find $base" >&2
        return 1
    fi
    if [[ "$count" -gt 1 ]]; then
        echo "'$base' is ambiguous ($count matches). Re-run with one of:" >&2
        printf '%s\n' "$matches" >&2
        return 1
    fi
    printf '%s\n' "$matches"
}

# Prints one method's disassembly out of a full javap -c dump.
extract_member() {
    awk -v member="$1" '
        # Method headers sit at indent 2 and end in ");".
        /^  [A-Za-z].*\);[[:space:]]*$/ {
            inside = index($0, member "(") > 0
        }
        inside { print }
    '
}

command="${1:-}"
[[ -n "$command" ]] || usage
shift || true

case "$command" in
find)
    symbol="${1:-}"
    [[ -n "$symbol" ]] || usage
    # Bash grep, not the Grep tool: ripgrep skips binaries and would silently
    # report nothing here. Constant-pool strings are greppable as plain text.
    ( cd "$server_dir" && grep -rl --include='*.class' -- "$symbol" . ) \
        | sed 's|^\./||' \
        || { echo "No class mentions '$symbol'." >&2; exit 1; }
    ;;
sig)
    target="${1:-}"
    [[ -n "$target" ]] || usage
    path="$(resolve_class "$target")" || exit 1
    ( cd "$server_dir" && "$javap" -p "$path" )
    ;;
code)
    target="${1:-}"
    [[ -n "$target" ]] || usage
    member="${2:-}"
    path="$(resolve_class "$target")" || exit 1
    dump="$(mktemp -t hytale-javap-XXXXXX.txt)"
    ( cd "$server_dir" && "$javap" -p -c "$path" ) > "$dump" 2>&1
    if [[ -n "$member" ]]; then
        extract_member "$member" < "$dump"
        exit 0
    fi
    lines="$(wc -l < "$dump")"
    if [[ "$lines" -le 200 ]]; then
        cat "$dump"
        rm -f "$dump"
    else
        echo "$lines lines - too long to inline. Full dump: $dump"
        echo "Members (re-run with one as the second argument to isolate it):"
        grep -E '^  [A-Za-z].*\);[[:space:]]*$' "$dump" | sed 's/^  /  /'
    fi
    ;;
*)
    usage
    ;;
esac
