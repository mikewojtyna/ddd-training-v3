#!/usr/bin/env bash
# Validate a .cml file with the Context Mapper DSL that is already a dependency in pom.xml.
# Usage: validate_cml.sh <file.cml>   (run from the repository root)
# Exit code: 0 valid, 1 errors found, 2 usage/setup problem.
set -euo pipefail
if [[ $# -ne 1 ]]; then echo "usage: $0 <file.cml>" >&2; exit 2; fi
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
CML="$(realpath "$1")"
CP_FILE="$REPO_ROOT/target/discover-bounded-contexts.classpath"
if [[ ! -s "$CP_FILE" || "$REPO_ROOT/pom.xml" -nt "$CP_FILE" ]]; then
  mkdir -p "$REPO_ROOT/target"
  (cd "$REPO_ROOT" && ./mvnw -q dependency:build-classpath -Dmdep.outputFile="$CP_FILE" >&2) || {
    echo "could not resolve the Maven classpath" >&2; exit 2; }
fi
exec java --sun-misc-unsafe-memory-access=allow -cp "$(cat "$CP_FILE")" "$SCRIPT_DIR/ValidateCml.java" "$CML" 2> >(grep -v -e '^log4j' -e 'SLF4J' >&2)
