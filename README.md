# BMW Garage Assistant

Prywatna aplikacja Android do organizowania napraw, diagnostyki, notatek, zdjec, czesci i dokumentow dla aut BMW.

## Aktualny zakres

- profile aut i lista modulow auta,
- edycja profilu auta z VIN oraz opcjonalnym linkiem katalogu czesci,
- lista napraw pogrupowana wedlug obszarow auta,
- lista zakupow przypisana do konkretnej naprawy i obszaru auta,
- integracja schematow czescidobmw.pl po VIN: grupy, miniatury, obraz schematu i lista OEM,
- dodawanie wybranych czesci ze schematu bezposrednio do listy zakupow,
- przyjmowanie pozycji z listy zakupow do magazynu czesci,
- automatyczne zdejmowanie wykorzystanych czesci z magazynu po zakonczeniu naprawy,
- dokumentacja przypisana do konkretnej naprawy,
- archiwum zakonczonych napraw w zakladce Dokumenty, pogrupowane wedlug obszaru auta,
- linki TIS z nazwa i edycja,
- pliki, dokumenty, zdjecia, filmy i linki YouTube zarzadzane bezposrednio w szczegolach naprawy,
- pelnoekranowa galeria zdjec i filmow z przesuwaniem oraz powiekszaniem gestami,
- wiele tabel momentow dokrecen dla jednej naprawy,
- import tabel momentow ze screenshotu TIS przez OCR,
- schematy do tabel momentow z recznym przypisywaniem punktow na zdjeciu,
- linki YouTube z tytulem, miniaturka, notatka i edycja,
- notatki wlasne: tekst, zdjecia, filmy, dokumenty, linki i dowolne pliki,
- import i eksport dokumentacji naprawy jako jeden pakiet `.bmwdoc.zip`.

Pakiet eksportu dokumentacji zawiera dane dokumentacji oraz dolaczone pliki, np. schematy, zdjecia, filmy i dokumenty. Dzieki temu dokumentacje jednej naprawy mozna przeniesc na inne urzadzenie lub wyslac innemu uzytkownikowi.

## Schematy i lista zakupow

Profil auta przechowuje VIN. Na jego podstawie aplikacja pobiera katalog pojazdu z `czescidobmw.pl`, pokazuje grupy pasujace do obszaru naprawy, a nastepnie liste schematow z miniaturami. W widoku schematu obraz pozostaje widoczny nad przewijana lista czesci. Obraz mozna powiekszyc, a wybrane pozycje OEM dodac do listy zakupow danej naprawy.

Lista zakupow jest powiazana z naprawa i magazynem. Pozycje mozna edytowac, usuwac, uzupelniac o dane sklepu i oznaczac jako odebrane. Odebrana pozycja przechodzi do magazynu czesci.

Pierwszy profil testowy:

- BMW E60
- 520d
- silnik M47N2 2.0d
- glowny przypadek: tylna zwrotnica lewa, zardzewiala sruba, identyfikacja czesci i plan naprawy

## Jak otworzyc projekt

1. Otworz Android Studio.
2. Wybierz `Open`.
3. Wskaz katalog `BmwGarageAssistant`.
4. Poczekaj na `Gradle Sync`.
5. Uruchom aplikacje na emulatorze albo na Galaxy S23 Ultra przez USB.

Pierwsza synchronizacja Gradle moze pobrac zaleznosci z internetu.

## Aktualizacja APK poza Google Play

Aplikacja ma teraz wbudowany mechanizm sprawdzania nowej wersji z `GitHub Releases`.

Jak to dziala:

- ekran `Moj garaz` sprawdza najnowszy release w repozytorium,
- gdy znajdzie nowsza wersje, pokazuje przycisk pobrania,
- APK pobiera sie do cache aplikacji,
- po pobraniu aplikacja uruchamia systemowy instalator Androida,
- na Androidzie instalacja nadal wymaga potwierdzenia uzytkownika.

Konfiguracja:

1. Repo aktualizacji jest ustawione na:
   - `kintoff/BMW_garage_helper`
2. Publikuj nowe wersje jako `GitHub Release`.
3. Do release dodawaj plik `.apk` jako asset.
4. `tag_name` release powinien odpowiadac `versionName`, np. `v0.2.0`.
5. Kazde wydanie APK musi byc podpisane tym samym kluczem i miec rosnacy `versionCode`.

Uwagi:

- obecna implementacja zaklada publiczne `GitHub Releases`,
- dla prywatnego repo trzeba bedzie dodac osobna warstwe autoryzacji albo posredni endpoint,
- najlepszy format wydania to jeden release = jeden APK do instalacji.

## Workflow wydan APK

W repo jest przygotowany workflow:

- [.github/workflows/release-apk.yml](/Users/izabelakoziol/Documents/Codex/2026-05-05/chce-rozpocz-c-projekt-aplikacji-dla/BmwGarageAssistant/.github/workflows/release-apk.yml)

## Testy regresyjne i CI

W repo jest tez przygotowany osobny workflow testowy:

- [.github/workflows/android-tests.yml](/Users/izabelakoziol/Documents/Codex/2026-05-05/chce-rozpocz-c-projekt-aplikacji-dla/BmwGarageAssistant/.github/workflows/android-tests.yml)

Co uruchamia sie automatycznie:

- przy `push`,
- przy `pull_request`,
- recznie z zakladki `Actions`.

Zakres workflow w GitHub:

- `:app:testDebugUnitTest` dla szybkich testow logiki.

Aktualna strategia testow:

- nowy feature powinien dostawac nowy test,
- logika biznesowa i parsery trafiaja najpierw do szybkiej warstwy `JVM`,
- `androidTest` zostaje dla realnych przeplywow UI, Room oraz import/export zaleznych od Androida,
- procent pokrycia jest wskaznikiem pomocniczym, ale priorytetem jest ochrona krytycznych danych i scenariuszy uzytkownika.

Najwazniejsze obszary pokryte testami:

- reguly napraw, checkpointow, notatek i archiwizacji,
- dokumentacja napraw, linki TIS, YouTube i `personalNotes`,
- `torqueTables` oraz import/export dokumentacji,
- shopping list, magazyn i przyjmowanie czesci,
- parsery `Allegro` i `czescidobmw.pl`,
- zgodnosc wsteczna danych i migracje legacy -> Room,
- logika sprawdzania aktualizacji APK,
- podstawowe przeplywy UI Compose na urzadzeniu.

Pelne testy regresyjne odpalamy lokalnie na podlaczonym telefonie. To jest celowy wybor:

- mniej problemow z emulatorami w CI,
- bardziej realne srodowisko testowe,
- prostszy workflow do nauki i codziennej pracy.

Gotowe skrypty lokalne:

- [scripts/run-unit-tests.sh](/Users/izabelakoziol/Documents/Codex/2026-05-05/chce-rozpocz-c-projekt-aplikacji-dla/BmwGarageAssistant/scripts/run-unit-tests.sh) uruchamia szybkie testy logiki,
- [scripts/run-device-tests.sh](/Users/izabelakoziol/Documents/Codex/2026-05-05/chce-rozpocz-c-projekt-aplikacji-dla/BmwGarageAssistant/scripts/run-device-tests.sh) uruchamia testy Room i UI Compose na podlaczonym telefonie,
- [scripts/run-local-regression.sh](/Users/izabelakoziol/Documents/Codex/2026-05-05/chce-rozpocz-c-projekt-aplikacji-dla/BmwGarageAssistant/scripts/run-local-regression.sh) odpala caly lokalny pakiet regresji.

Najwygodniejszy wariant przed pushem:

```bash
./scripts/run-local-regression.sh
```

Albo osobno:

```bash
./scripts/run-unit-tests.sh
./scripts/run-unit-tests.sh --coverage
./scripts/run-device-tests.sh
```

Raport pokrycia po `--coverage` znajdziesz w:

```bash
app/build/reports/jacoco/jacocoDebugUnitTestReport/html/index.html
```

## Firebase AI Logic test

W projekcie jest przygotowany proof of concept dla:

Android App
-> Firebase AI Logic
-> Gemini
-> Android UI

Obecny backend AI zostaje w repo jako fallback.

Domyslnie proof of concept Firebase jest uspiony:

- bez `USE_FIREBASE_AI_LOGIC=true` aplikacja zostaje na backend fallback,
- nawet przy wlaczonej fladze Firebase nie zostanie uzyte bez lokalnego `app/google-services.json`.

Jak przelaczac providera AI:

- `USE_FIREBASE_AI_LOGIC=true`:
  aplikacja korzysta z Firebase AI Logic
- `USE_FIREBASE_AI_LOGIC=false`:
  aplikacja korzysta z istniejacego backendu `AI_ASSISTANT_BASE_URL`

Do testu Firebase potrzebujesz prawdziwego pliku:

- `app/google-services.json`

W repo jest tylko szablon:

- [app/google-services.json.example](/Users/izabelakoziol/Documents/Codex/2026-05-05/chce-rozpocz-c-projekt-aplikacji-dla/BmwGarageAssistant/app/google-services.json.example)

Przyklady builda:

```bash
./gradlew assembleDebug -PUSE_FIREBASE_AI_LOGIC=true
./gradlew assembleDebug -PUSE_FIREBASE_AI_LOGIC=false -PAI_ASSISTANT_BASE_URL=http://10.0.2.2:8000
```

Logi Androida dla obu wariantow filtruj po tagu:

```text
AI_ASSISTANT
```

Skrypt testow na telefonie wymaga:

- podlaczonego telefonu przez USB,
- wlaczonego debugowania USB,
- zaakceptowanego polaczenia ADB na telefonie.

Przed pierwszym wydaniem ustaw w `GitHub -> Settings -> Secrets and variables -> Actions` sekrety:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_KEY_PASSWORD`

Jak przygotowac `ANDROID_KEYSTORE_BASE64` na Macu:

```bash
base64 -i twoj-release-key.jks | pbcopy
```

To skopiuje zawartosc do schowka. Wklej ja jako wartosc sekretu `ANDROID_KEYSTORE_BASE64`.

Jak uruchomic wydanie:

1. Wejdz w `Actions`.
2. Wybierz workflow `Release APK`.
3. Kliknij `Run workflow`.
4. Podaj:
   - `version_name`, np. `0.2.0`
   - `version_code`, np. `2`
   - opcjonalnie `release_notes`
5. Workflow zbuduje podpisany `release APK`, wrzuci go do `GitHub Release` i doda plik `.sha256`.

Wazne zasady:

- `version_code` zawsze musi rosnac,
- `version_name` powinien zgadzac sie z tagiem release, np. `v0.2.0`,
- wszystkie wydania musza byc podpisane tym samym kluczem,
- aplikacja sprawdza nowe wersje na podstawie `GitHub Releases`, wiec release musi zawierac plik `.apk`.
