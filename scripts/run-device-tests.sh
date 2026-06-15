#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

if [[ -z "${JAVA_HOME:-}" ]] && [[ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT_DIR/.gradle-local}"

if ! command -v adb >/dev/null 2>&1; then
  echo "Nie znaleziono adb w PATH. Otworz Android Studio albo dodaj Android SDK platform-tools do PATH."
  exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "Nie wykryto podlaczonego telefonu lub gotowego urzadzenia testowego."
  echo "Podlacz telefon przez USB, zaakceptuj debugowanie i sprobuj ponownie."
  exit 1
fi

ANDROID_LOCAL_HOME="${ANDROID_USER_HOME:-$ROOT_DIR/.android-local}"
mkdir -p "$ANDROID_LOCAL_HOME"
export ANDROID_USER_HOME="$ANDROID_LOCAL_HOME"
unset ANDROID_SDK_HOME

echo "Uruchamiam testy instrumentacyjne na podlaczonym urzadzeniu..."
echo "Czyszcze projekt przed testami na urzadzeniu..."
./gradlew clean

echo "Buduje i uruchamiam testy instrumentacyjne..."
./gradlew :app:connectedDebugAndroidTest

echo "Generuje wspolny raport pokrycia JVM + androidTest..."
./gradlew :app:jacocoDebugCombinedCoverageReport
