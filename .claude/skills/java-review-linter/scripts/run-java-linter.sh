#!/usr/bin/env bash
# Runs the project's Java compile step and surfaces warnings/errors so the
# java-review-linter skill can review and fix them. This project has no
# Checkstyle/PMD/SpotBugs Gradle plugin configured — the compiler (javac,
# with the Eclipse JDT null-analysis prefs picked up in the IDE) is the only
# enforced check, so `compileJava` is the real "linter" here.
set -uo pipefail

cd "$(dirname "$0")/../../../.." || exit 1

# Fall back to the JDK bundled in the repo (see CLAUDE.md) if the shell
# doesn't already have JAVA_HOME set.
if [[ -z "${JAVA_HOME:-}" && -d "jdk-25.0.2" ]]; then
    export JAVA_HOME="$(pwd)/jdk-25.0.2"
fi

# ./gradlew is the POSIX wrapper script; it runs fine under Git Bash on
# Windows too, so no need to branch to gradlew.bat here.
echo "Running: ./gradlew compileJava compileTestJava --console=plain"
./gradlew compileJava compileTestJava --console=plain
status=$?

if [[ $status -eq 0 ]]; then
    echo "✅ Compile succeeded with no errors (review output above for any warnings)."
else
    echo "❌ Compile failed (exit code $status). See errors above."
fi

exit $status
