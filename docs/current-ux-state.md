# Aktualny stan UX

Ostatnia aktualizacja: 2026-05-18

Ten dokument zapisuje, gdzie skonczylismy prace nad przebudowa UI i co jest juz podpiete funkcjonalnie.

## Kierunek UI

Trzymamy sie ciemnego, technicznego stylu z projektu:

- tlo w odcieniach granatu i czerni,
- karty z delikatnym kontrastem,
- akcent niebieski dla aktywnych elementow,
- kategorie napraw reprezentowane ikonami,
- dolna nawigacja spójna z ekranem garazu,
- bez zbednych opisow pomocniczych w UI.

## Ekran glowny garazu

Zrobione:

- widok `Moj garaz` jest zblizony do referencji,
- poprawione ikony szybkiego dostepu,
- poprawione dolne ikony nawigacji,
- przycisk `+` ma byc wzorcem dla pozostalych ekranow.

Do dopracowania pozniej:

- detale proporcji i odstepow,
- docelowe ikony/asset auta,
- ostatnie szlify wizualne po przejsciu przez wszystkie ekrany.

## Lista napraw

Zrobione:

- zostaly tylko zakladki `Aktywne` i `Zakonczone`,
- usunieto linie postepu z listy napraw,
- naprawy nadal maja kategorie,
- zamiast kolorowego kwadratu pokazywana jest ikona kategorii,
- przycisk `+` otwiera wybor kategorii i dodawanie naprawy,
- nowa naprawa od razu tworzy powiazana dokumentacje,
- dokumentacja, czesci i lista zakupow lacza sie z naprawa przez stabilne `repairId`.

Do dopracowania pozniej:

- formularz dodawania naprawy per kategoria,
- filtrowanie lub grupowanie po kategoriach,
- finalny wyglad statusow.

## Naprawa - Opis

Zrobione:

- `Opis problemu` jest edytowalny,
- `Cel naprawy` zostal usuniety z widoku,
- `Plan dzialania` jest checklista checkpointow,
- checkpointy mozna odhaczac,
- checkpointy mozna dodawac z poziomu zakladki,
- checkpointy zapisuja sie w modelu `RepairProject`.

Model:

- aktualne pole: `checkpoints`,
- legacy: `checklist`,
- legacy: `goal`.

Do dopracowania pozniej:

- usuwanie i edycja pojedynczego checkpointu,
- ewentualne przeciaganie kolejnosci,
- lepsze stany pustej listy.

## Naprawa - Dokumenty

Zrobione:

- zakladka jest podzielona na:
  - `Linki TIS`,
  - `Pliki i dokumenty`,
  - `Youtube`,
  - `Zdjecia i filmy`,
- YouTube ma miec kafel z miniatura,
- zdjecia i filmy maja podglad w 4 rownych kafelkach,
- ostatni kafelek moze pokazywac licznik pozostalych mediow,
- na dole jest przycisk `Dodaj`.

Do dopracowania pozniej:

- realne pobieranie miniatur YouTube,
- dodawanie elementow do konkretnej kategorii,
- otwieranie podgladu zdjec i filmow,
- rozdzielenie typow dodawania: TIS, plik, YouTube, media.

## Naprawa - Momenty

Zrobione:

- zakladki to `Lista` i `Szczegoly`,
- `Lista` pokazuje schemat z naniesionymi punktami momentow,
- pod schematem pokazuje krotkie rekordy z opisem i momentem,
- `Szczegoly` pokazuje pelna tabele dokrecen,
- model wspiera wiele tabel momentow przez `TorqueSpecTable`.

Do dopracowania pozniej:

- import tabeli ze screenshota,
- reczne dodawanie rekordow momentow,
- zmiana schematu,
- pelny podglad schematu,
- edycja punktow na schemacie.

## Zasada dokumentacji po zakonczeniu naprawy

Dokumentacja nie moze znikac po zakonczeniu naprawy.

Ustalenie:

- naprawa moze przejsc do `Zakonczone`,
- dokumentacja zostaje przypisana przez `repairId`,
- pozniej zbudujemy osobny widok historii lub archiwum dokumentacji,
- zakończenie naprawy nie usuwa linkow, plikow, YouTube, zdjec, momentow ani notatek.

## Ograniczenia techniczne na teraz

Pelny build Gradle nie zostal uruchomiony, bo lokalne srodowisko nie ma dostepnego Java Runtime.

Sprawdzone:

- `git diff --check` przechodzi bez bledow,
- usuniete zostaly duplikaty `* 2.dex`, ktore powodowaly bledy `defined multiple times`.

## Najblizszy powrot do pracy

Proponowana kolejnosc:

1. Odpalic projekt po zainstalowaniu/ustawieniu Java Runtime.
2. Sprawdzic ekran `Naprawy -> Opis` na telefonie/emulatorze.
3. Dopiac edycje/usuwanie checkpointow.
4. Wrocic do zakladki `Dokumenty` i podpiac dodawanie elementow do kategorii.
5. Potem przejsc do importu/edycji momentow w zakladce `Momenty`.
