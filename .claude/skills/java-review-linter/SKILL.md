---
name: java-review-linter
description: 'Use this skill when the user wants to review Java code changes, fix Checkstyle/PMD linter warnings, or optimize Java source files.'
---

# Java Code Review & Linting Fixer

You are an expert Java staff engineer reviewing changes in this project. This repo has **no Checkstyle/PMD/SpotBugs Gradle plugin configured** — the only enforced check is the compiler itself, together with the Eclipse JDT null-analysis rules described in `CLAUDE.md`'s "Null-safety" section. Treat `CLAUDE.md` as the source of truth for project-specific conventions and defer to it over any generic Java style rule below.

## Activation Trigger
Run this when the user explicitly asks for a Java code review or a lint/warning fix — not on every routine edit.

## Review & Fixing Guidelines

1. **Null-safety (project-specific, see `CLAUDE.md`):**
   - Treat `@Nonnull`/`@Nullable` warnings as required fixes, not noise.
   - When overriding a method, only widen accepted parameter nullability and only narrow the return type to `@Nonnull` if actually guaranteed — verify the parent signature in `reference/server` (e.g. via `javap -v`) rather than guessing.
   - Fix "unchecked conversion to `@Nonnull`" warnings at their root (the actual unannotated source), not by patching every call site.
   - Never add `@SuppressWarnings` for a null-safety warning without asking the user first.

2. **General Java quality:**
   - Remove unused imports and local variables.
   - Enforce standard Java naming conventions (PascalCase for classes, camelCase for methods/variables) and explicit access modifiers.
   - Always use braces for `if`/`for`/`while` statements.
   - Suggest modern Java features (Records, Switch Expressions, Streams) where they improve readability, appropriate for this project's Java version (25).
   - Follow this project's comment convention: no comments on private methods/classes, and only comment public methods when something is genuinely non-obvious — don't restate what the code already says.

3. **Output Format Requirements:**
   - **Review Summary:** A bulleted list of issues found.
   - **Refactored Code:** The fully corrected code in a single markdown code block.
   - **Changelog:** A concise list of exact modifications made.

## Automated Workflow Steps

1. **Run Local Linter:**
   Before guessing potential errors, execute the automated verification script:
   - Command to run: `./scripts/run-java-linter.sh`
   - This runs `./gradlew compileJava compileTestJava` and surfaces javac warnings/errors — read the terminal output carefully, including warnings (not just failures), since a clean exit code doesn't mean warning-free.

2. **Fix Violations:**
   - If the script reports errors or warnings, analyze the output and fix all violations directly in the Java source files.
   - Never suppress or ignore warnings; address them, following the null-safety rules above for `@Nonnull`/`@Nullable` cases.

3. **Provide Output:**
   - State whether the script passed or failed.
   - Show the corrected code and outline your changes.