#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

if ! command -v adb >/dev/null 2>&1; then
  echo "Nie znaleziono adb w PATH. Otworz Android Studio albo dodaj Android SDK platform-tools do PATH."
  exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "Nie wykryto podlaczonego telefonu lub gotowego urzadzenia testowego."
  echo "Podlacz telefon przez USB, zaakceptuj debugowanie i sprobuj ponownie."
  exit 1
fi

echo "Uruchamiam testy instrumentacyjne na podlaczonym urzadzeniu..."
./gradlew :app:connectedDebugAndroidTest
