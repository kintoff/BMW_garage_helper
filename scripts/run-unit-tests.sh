#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

if [[ -z "${JAVA_HOME:-}" ]] && [[ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT_DIR/.gradle-local}"

echo "Uruchamiam szybkie testy logiki..."
./gradlew :app:testDebugUnitTest

if [[ "${1:-}" == "--coverage" ]]; then
  echo "Generuje raport pokrycia dla testow JVM..."
  ./gradlew :app:jacocoDebugUnitTestReport
fi
