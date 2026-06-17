# Aktualny stan UX

Ostatnia aktualizacja: 2026-06-09

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
- checkpointy mozna edytowac i usuwac z poziomu zakladki,
- checkpointy zapisuja sie w modelu `RepairProject`.

Model:

- aktualne pole: `checkpoints`,
- legacy: `checklist`,
- legacy: `goal`.

Do dopracowania pozniej:

- ewentualne przeciaganie kolejnosci,
- lepsze stany pustej listy.

## Naprawa - Czesci

Zrobione:

- sekcja `Czesci do ustalenia` zostala zastapiona przez `Lista zakupow`,
- `Lista zakupow` pokazuje pozycje z listy zakupow przypisane do konkretnej naprawy przez `repairId`,
- `Na stanie` pokazuje czesci z magazynu przypisane do konkretnej naprawy,
- wiersze czesci sa bardziej zwarte: nazwa, numer, ilosc i status po prawej,
- przycisk `Dodaj czesc` prowadzi do listy zakupow dla tej naprawy.
- klikniecie `Schematy czescidobmw.pl` od razu laduje schematy bez dodatkowego przycisku,
- widok wybranego schematu ma mniejszy tytul, wiekszy obraz i czytelniejsze rekordy czesci,
- wybor czesci ze schematu uzywa ikony zamiast tekstu `Wybierz`.
- obraz schematu obsluguje przyblizanie i przesuwanie gestem dwoch palcow,
- usunieto podpowiedz `Kliknij schemat, aby powiekszyc`, dzieki czemu lista czesci zaczyna sie wyzej.
- klikniecie czesci na schemacie otwiera wybor dostepnych produktow ze sklepu po numerze OEM,
- okno wyboru produktu automatycznie wyszukuje oferty, pozwala wybrac konkretna czesc i ustawic ilosc,
- produkt dodany ze sklepu trafia do listy zakupow z cena, linkiem i zdjeciem podgladowym, jesli sklep je zwroci.
- lista zakupow pokazuje miniatury zdjec czesci, jesli sa dostepne,
- przy pozycji zakupowej jest akcja `Do magazynu`, ktora pozwala dodac calosc albo wybrana ilosc,
- w oknie `Do magazynu` klikniecie `Dodaj calosc` od razu przenosi komplet, a `Dodaj ilosc` ma wlasny przycisk dla wpisanej liczby,
- czesciowe dodanie do magazynu aktualizuje pozostala ilosc na liscie zakupow,
- dodawanie do magazynu z poziomu naprawy ma opcje skanowania etykiety,
- przycisk `Dodaj czesc` pozwala wybrac dodanie do `Lista zakupow` albo bezposrednio do `Magazyn`.
- usuniecie lub edycja rekordu w magazynie aktualizuje wspolny stan auta, wiec powiazane czesci znikaja tez z `Naprawy -> Czesci`.
- ekran `Czesci` z dolnej nawigacji otwiera najpierw widok wyboru kafelkow, zamiast od razu przenosic do magazynu,
- lista zakupow i magazyn dostaly bardziej kartowy, mobilny uklad,
- edycja i dodawanie pozycji do listy zakupow oraz magazynu uzywa wyboru naprawy z aktywnych napraw zamiast wolnego tekstu,
- karta pozycji zakupowej pokazuje tylko najwazniejsze dane: nazwe, OEM, kod producenta gdy rozny od OEM, ilosc i cene za sztuke,
- w zakladce `Naprawa -> Czesci` dodano podsumowanie wartosci czesci dla naprawy,
- stan ekranow czesci, listy zakupow, naprawy i dokumentacji jest zapamietywany po obrocie telefonu.

Eksperyment:

- dodano prototyp akcji Allegro pod dlugim przytrzymaniem pozycji z listy zakupow,
- prototyp pozwala otworzyc wyszukiwanie po numerze OEM i wklei link oferty do testu,
- automatyczne pobieranie ceny i zdjecia z Allegro nie zostalo domkniete, bo strona oferty jest blokowana lub zwraca dane w sposob niestabilny dla tego podejscia,
- obecne podejscie traktujemy jako probe techniczna, a nie gotowa funkcje produkcyjna.

Do dopracowania pozniej:

- dodawanie czesci do listy zakupow bez przechodzenia do osobnego ekranu magazynu,
- lepsze liczenie brakow, kiedy czesc ma rozne numery OEM i producenta.
- dalsze dopieszczenie ikon wyboru po podpieciu docelowego zestawu ikon,
- wymyslic inny, stabilniejszy sposob porownywania cen z Allegro niz bezposredni odczyt danych z publicznej strony oferty.

## Naprawa - Dokumenty

Zrobione:

- zakladka w szczegolach naprawy nazywa sie `Dokumentacja`,
- zakladka jest podzielona na:
  - `Linki TIS`,
  - `Pliki i dokumenty`,
  - `Youtube`,
  - `Zdjecia i filmy`,
- kazda glowna sekcja ma szybkie dodawanie elementu,
- dodawanie nie otwiera juz starego ekranu dokumentacji,
- linki TIS mozna dodawac, otwierac, edytowac i usuwac,
- pliki i dokumenty mozna dodawac z systemowego wybieraka, widac ich nazwe i rozmiar, mozna je otworzyc, edytowac i usuwac,
- YouTube ma kafel z miniatura, tytulem dopasowanym do kafla i akcjami edycji/usuwania po dlugim przytrzymaniu,
- zdjecia i filmy maja podglad w 4 rownych kafelkach,
- ostatni kafelek moze pokazywac licznik pozostalych mediow,
- zdjecia i filmy sa wybierane przez trwaly dostep do pliku, zeby miniatury nie znikaly po ponownym uruchomieniu aplikacji,
- dlugie przytrzymanie zdjecia lub filmu pokazuje akcje `Edytuj` i `Usun`,
- klikniecie zdjecia lub filmu otwiera pelnoekranowa galerie,
- galeria obsluguje przesuwanie miedzy mediami, powiekszanie dwoma palcami, podwojne tapniecie i przesuwanie powiekszonego zdjecia.

Do dopracowania pozniej:

- dalsze testy gestow galerii na fizycznym telefonie,
- ewentualne dopracowanie animacji i bezwladnosci galerii pod Android Gallery.

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
- zakonczona naprawa trafia do `Dokumenty -> Archiwum napraw`,
- archiwum napraw jest pogrupowane wedlug kategorii, np. silnik, nadwozie, hamulce,
- ekran archiwalnej naprawy uzywa tego samego widoku szczegolow co aktywna naprawa,
- `Dokumenty` maja tez sekcje `Dokumentacja ogolna`,
- zakonczenie naprawy nie usuwa linkow, plikow, YouTube, zdjec, momentow ani notatek,
- po zakonczeniu naprawy czesci wykorzystane z magazynu sa zdejmowane ze stanu,
- lista zakupow i uzyte czesci trafiaja do archiwalnej dokumentacji naprawy jako zapis tego, co zostalo wykorzystane.

## Sprawdzenie techniczne

Sprawdzone:

- `:app:compileDebugKotlin` przechodzi,
- `:app:assembleDebug` przechodzi,
- Gradle czasem pokazuje lokalne ostrzezenie o Kotlin daemon i `.android`, ale kompilacja konczy sie sukcesem po fallbacku.

## Najblizszy powrot do pracy

Proponowana kolejnosc:

1. Odpalic projekt po zainstalowaniu/ustawieniu Java Runtime.
2. Sprawdzic galerie dokumentacji na fizycznym telefonie po dluzszym uzyciu: zoom, przesuwanie, powrot i ponowne otwarcie.
3. Sprawdzic flow zakonczenia naprawy: magazyn, archiwalna lista zakupow i dokumentacja.
4. Wymyslic inne podejscie do porownywania cen z Allegro, bez opierania sie na niestabilnym parsowaniu publicznej strony oferty.
5. Dopracowac sekcje `Dokumentacja ogolna` w `Dokumenty`.
6. Potem przejsc do importu/edycji momentow w zakladce `Momenty`.
