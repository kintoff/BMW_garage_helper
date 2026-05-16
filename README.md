# BMW Garage Assistant

Prywatna aplikacja Android do organizowania napraw, diagnostyki, notatek, zdjec, czesci i dokumentow dla aut BMW.

## Aktualny zakres

- profile aut i lista modulow auta,
- lista napraw pogrupowana wedlug obszarow auta,
- dokumentacja przypisana do konkretnej naprawy,
- linki TIS z nazwa i edycja,
- wiele tabel momentow dokrecen dla jednej naprawy,
- import tabel momentow ze screenshotu TIS przez OCR,
- schematy do tabel momentow z recznym przypisywaniem punktow na zdjeciu,
- linki YouTube z tytulem, miniaturka, notatka i edycja,
- notatki wlasne: tekst, zdjecia, filmy, dokumenty, linki i dowolne pliki,
- import i eksport dokumentacji naprawy jako jeden pakiet `.bmwdoc.zip`.

Pakiet eksportu dokumentacji zawiera dane dokumentacji oraz dolaczone pliki, np. schematy, zdjecia, filmy i dokumenty. Dzieki temu dokumentacje jednej naprawy mozna przeniesc na inne urzadzenie lub wyslac innemu uzytkownikowi.

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
