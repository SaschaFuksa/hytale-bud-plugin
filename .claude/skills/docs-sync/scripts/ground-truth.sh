#!/usr/bin/env bash
# Dumps the actual, current shape of the plugin's config keys, commands and
# Buds straight from source - the ground truth the docs-sync skill diffs
# README.md / docs/*.html against. Exists because that diff was done by hand
# once (grepping each *Config.java / commands/*.java file individually) and
# is exactly the kind of mechanical step that should not be re-invented, or
# skipped, on the next pass.
#
#   ground-truth.sh configs     KeyedCodec key list per com.bud.core.config.*Config class
#   ground-truth.sh commands    subcommand name + every flag/arg per com.bud.app.commands class
#   ground-truth.sh buds        id/workRole/restPosition per buds/*.yml
#   ground-truth.sh all         all three, in order
set -uo pipefail

repo_root="$(cd "$(dirname "$0")/../../../.." && pwd)"
config_dir="$repo_root/src/main/java/com/bud/core/config"
commands_dir="$repo_root/src/main/java/com/bud/app/commands"
buds_dir="$repo_root/src/main/resources/buds"

dump_configs() {
    echo "## Config keys (KeyedCodec) per class"
    for f in "$config_dir"/*.java; do
        name="$(basename "$f" .java)"
        [[ "$name" == "WorkConfig" ]] && continue
        keys="$(grep -oE 'new KeyedCodec<>\("[A-Za-z]+"' "$f" | sed -E 's/new KeyedCodec<>\("//; s/"$//')"
        count="$(printf '%s\n' "$keys" | grep -c . || true)"
        echo
        echo "### $name ($count keys)"
        printf '%s\n' "$keys" | sed 's/^/  - /'
    done
    echo
    echo "### WorkConfig (documented separately on the Work Stations page, not Configuration)"
    grep -oE 'new KeyedCodec<>\("[A-Za-z]+"' "$config_dir/WorkConfig.java" | sed -E 's/new KeyedCodec<>\("//; s/"$//' | sed 's/^/  - /'
}

dump_commands() {
    echo "## Commands (subcommand name + every flag/arg) per class"
    for f in "$commands_dir"/*.java; do
        name="$(basename "$f" .java)"
        [[ "$name" == "BudIdArgumentType" ]] && continue
        cmd_name="$(grep -oE 'super\("[a-zA-Z]+"' "$f" | head -1 | sed -E 's/super\("//; s/"$//')"
        echo
        echo "### $name (/bud ... $cmd_name)"
        grep -oE '\.?withFlagArg\("[a-zA-Z]+"|\.?withRequiredArg\("[a-zA-Z]+"|\.?withOptionalArg\("[a-zA-Z]+"' "$f" \
            | sed -E 's/\.?withFlagArg\("/  --/; s/\.?withRequiredArg\("/  <required> /; s/\.?withOptionalArg\("/  [optional] /' \
            | sed 's/"$//' || true
        if grep -qE 'ADMIN_GROUP|requireAdmin' "$f"; then
            echo "  (has an admin-only gate - check which branch)"
        fi
    done
}

dump_buds() {
    echo "## Buds (buds/*.yml)"
    for f in "$buds_dir"/*.yml; do
        base="$(basename "$f")"
        [[ "$base" == "roster.yml" ]] && continue
        id="$(grep -E '^id:' "$f" | head -1 | sed -E 's/^id:[[:space:]]*//')"
        workRole="$(grep -E '^workRole:' "$f" | head -1 | sed -E 's/^workRole:[[:space:]]*//')"
        restPosition="$(grep -E '^restPosition:' "$f" | head -1 | sed -E 's/^restPosition:[[:space:]]*//')"
        echo "  - $base: id=$id workRole=${workRole:-none} restPosition=${restPosition:-NONE}"
    done
}

mode="${1:-all}"
case "$mode" in
configs) dump_configs ;;
commands) dump_commands ;;
buds) dump_buds ;;
all)
    dump_configs
    echo
    dump_commands
    echo
    dump_buds
    ;;
*)
    sed -n '5,10p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
    ;;
esac
