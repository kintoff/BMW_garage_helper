# Architektura aplikacji BMW Garage Assistant

Ten dokument opisuje aktualny stan architektury aplikacji po rozpoczętym refactorze. Nie jest to jeszcze architektura docelowa, tylko praktyczny obraz tego, jak projekt jest obecnie zorganizowany i w którą stronę powinien dalej iść.

## Kierunek architektoniczny

Aplikacja stopniowo przechodzi z dużych, monolitycznych plików Compose w stronę prostego układu MVVM:

```text
Compose UI
  -> ViewModel / UI State / Actions
  -> Repository / Service
  -> Room / pliki / sieć
```

Refactor jest obecnie w fazie przejściowej. Część kodu ma już wydzielone warstwy, ale kilka dużych ekranów nadal zawiera UI, stan, dialogi i logikę biznesową w jednym miejscu.

## Główne pakiety

### `pl.garage.bmwassistant`

Pakiet startowy aplikacji.

Najważniejsze elementy:

- `MainActivity` uruchamia Compose UI.
- `GarageApplication` tworzy kontener zależności.
- `AppContainer` przechowuje podstawowe zależności aplikacji, obecnie głównie `GarageRepository` i migrator danych.
- `GarageApp` trzyma główny przepływ aplikacji: wybór auta, dodawanie auta i wejście do widoku pojazdu.

To jest obecnie prosty, ręczny odpowiednik dependency injection. Hilt nie jest jeszcze używany.

## Warstwa danych

### `database/catalog`

Przechowuje katalog pojazdów, czyli listę aut użytkownika.

Odpowiedzialność:

- rejestrowanie pojazdów,
- odczyt aktywnych pojazdów,
- usuwanie pojazdów,
- otwieranie osobnej bazy danych dla konkretnego auta.

### `database/vehicle`

To główna baza danych konkretnego pojazdu. Obecnie używa Room.

Najważniejsze obszary danych:

- naprawy,
- dokumentacja napraw,
- lista zakupów,
- części w magazynie,
- historia operacji magazynowych,
- linki TIS,
- filmy YouTube,
- tabele momentów dokręcania,
- osobiste materiały dokumentacyjne.

Aktualna wersja bazy pojazdu: `4`.

Ważna zmiana po refactorze:

- `InventoryHistoryEventEntity` przechowuje realne zdarzenia magazynowe,
- historia magazynu nie powinna być odtwarzana wyłącznie z aktualnego stanu części,
- `originShoppingItemId` jest informacją o pochodzeniu części z listy zakupów, ale nie jest twardym kluczem obcym do aktywnej listy zakupów.

### `database/repository`

`GarageRepository` jest główną bramą między UI a danymi.

Obecnie odpowiada za:

- ładowanie i zapisywanie pojazdów,
- ładowanie pełnego snapshotu danych pojazdu,
- zapis snapshotu pojazdu,
- operacje na magazynie części,
- operacje na historii magazynu,
- import i eksport archiwów napraw,
- mapowanie danych między Room a modelami aplikacji.

To jest dobre miejsce centralne dla danych, ale plik jest już dość duży. W kolejnych etapach warto wydzielić z niego mniejsze repozytoria lub serwisy, np.:

- `InventoryRepository`,
- `RepairRepository`,
- `DocumentationRepository`,
- `ShoppingListRepository`.

## Modele domenowe

### `model`

Zawiera główne modele używane przez UI i repository.

Przykłady:

- `Vehicle`,
- `RepairProject`,
- `RepairDocumentation`,
- `ShoppingListItem`,
- `PartInventoryItem`,
- `InventoryHistoryEvent`,
- `ConsumableItem`.

Modele są jeszcze współdzielone między UI, repository i logiką danych. To jest akceptowalne na obecnym etapie, ale docelowo warto pilnować, aby modele UI nie mieszały się z modelami bazy danych.

## Warstwa UI

### `ui/screens`

To nadal największa część aplikacji.

Najważniejsze ekrany:

- `GarageDashboard` - lista aut,
- `AddVehicleWizard` - dodawanie i edycja auta,
- `VehicleOverviewScreen` - główny widok auta i nawigacja między modułami,
- `VehicleRepairListScreen` - lista napraw, szczegóły napraw, części i dokumentacja napraw,
- `VehiclePartsStorageScreen` - magazyn części, lista zakupów, szczegóły części i AI assistant,
- `VehicleDocumentationScreen` - dokumentacja pojazdu,
- `VehicleStatusScreen` - status auta / sekcja więcej.

Największy dług techniczny nadal znajduje się w:

- `VehiclePartsStorageScreen`,
- `VehicleRepairListScreen`,
- `VehicleDocumentationScreen`,
- `VehicleOverviewScreen`.

Te pliki wciąż łączą kilka odpowiedzialności: layout Compose, stan ekranu, dialogi, transformacje danych, otwieranie zewnętrznych linków i część logiki biznesowej.

### `ui/components`

Zawiera komponenty współdzielone.

Przykłady:

- `BottomNavBar`,
- `GaragePanel`,
- `Header`,
- `SegmentTabs`,
- `StatusBadge`,
- wspólne kolory akcentów,
- obsługę bezpiecznego dolnego paddingu dla paska nawigacji Androida.

To jest dobry kierunek. Warto dalej wynosić tutaj komponenty używane na kilku ekranach, ale bez nadmiernego uogólniania.

## Moduł inventory

### `feature/inventory`

To pierwszy krok w stronę docelowej architektury feature-based.

Obecnie zawiera:

- `VehiclePartsStorageViewModel`,
- `VehiclePartsStorageUiState`,
- `VehiclePartsStorageAction`,
- `VehiclePartsStorageEvent`,
- `InventoryMappers`,
- `components/InventoryNavigationComponents`,
- `components/InventoryListComponents`,
- `dialogs/InventoryConfirmDialogs`.

Ten moduł jest nadal częściowo przygotowawczy. `VehiclePartsStorageScreen` korzysta już z pierwszych komponentów i dialogów przeniesionych do `feature/inventory`, ale nie jest jeszcze w pełni przepięty na ViewModel jako jedyne źródło stanu.

Dobry kierunek:

- stan i akcje zaczynają być nazwane jawnie,
- inventory ma osobny pakiet,
- powstaje miejsce na logikę widoku poza dużym plikiem Compose,
- proste komponenty nawigacyjne, kafle, badge, listowe elementy i dialogi potwierdzeń są już poza głównym ekranem.

Do dokończenia:

- przepiąć więcej stanu z `VehiclePartsStorageScreen` do `VehiclePartsStorageViewModel`,
- przenieść większe dialogi inventory do osobnych plików,
- przenieść karty i szczegóły części do osobnych komponentów feature,
- ograniczyć bezpośrednie operacje na danych w composable.

## AI Assistant

Logika AI znajduje się obecnie w:

```text
ui/screens/VehiclePartsShoppingAssistant.kt
```

Obecny układ:

- `ShoppingAssistantProvider` definiuje interfejs dostawcy AI,
- `BackendShoppingAssistantProvider` obsługuje backend `/parts/compare`,
- `FirebaseAiShoppingAssistantProvider` obsługuje test Firebase AI Logic,
- parser normalizuje odpowiedź do struktury Variant C.

Kierunek jest dobry, bo UI nie musi znać szczegółów backendu ani Firebase. Natomiast docelowo ten plik powinien zostać przeniesiony poza `ui/screens`, np. do:

```text
ai/
  ShoppingAssistantProvider.kt
  BackendShoppingAssistantProvider.kt
  FirebaseAiShoppingAssistantProvider.kt
  AiPromptBuilder.kt
  AiResponseParser.kt
  AiModels.kt
```

## Backend Gemini

W projekcie istnieje też katalog:

```text
backend/
```

Zawiera prosty backend testowy dla Gemini:

- `gemini_backend.py`,
- `.env.example`,
- `README.md`.

Ten backend jest obecnie fallbackiem / narzędziem diagnostycznym. Android nie powinien trzymać klucza Gemini API w APK.

## Przepływ danych

Typowy przepływ wygląda obecnie tak:

```text
GarageApp
  -> VehicleOverviewScreen
  -> GarageRepository
  -> VehicleDatabaseManager
  -> Room database per vehicle
```

Dla listy zakupów i magazynu:

```text
Naprawa / schemat części
  -> ShoppingListItem
  -> Lista zakupów
  -> Przyjmij do magazynu
  -> PartInventoryItem
  -> InventoryHistoryEvent
```

Ważna zasada biznesowa:

- lista zakupów pokazuje części do kupienia,
- magazyn pokazuje części dostępne fizycznie,
- przyjęcie części do magazynu powinno usuwać lub zmniejszać pozycję na liście zakupów,
- historia magazynu powinna zapisywać realne operacje użytkownika.

## Ocena obecnego refactoru

Kierunek jest dobry i zdrowy dla projektu.

Największe plusy:

- pojawił się ręczny kontener zależności zamiast tworzenia repository bezpośrednio w UI,
- dane są coraz mocniej oparte o Room,
- inventory dostało początek własnego modułu feature,
- inventory ma już wydzielone pierwsze komponenty i dialogi,
- repository ma dedykowaną transakcyjną operację przyjęcia pozycji zakupowej do magazynu,
- historia magazynu została potraktowana jako realne zdarzenia, a nie tylko widok aktualnego stanu,
- relacje zaczynają iść w stronę stabilniejszych identyfikatorów,
- UI jest wizualnie bardziej spójne między listą zakupów, magazynem i szczegółami części.

Największe ryzyka:

- największe pliki Compose nadal są bardzo duże,
- `VehiclePartsStorageViewModel` istnieje, ale nie jest jeszcze centralnym źródłem prawdy dla całego ekranu,
- `GarageRepository` zaczyna mieć zbyt wiele odpowiedzialności,
- AI nadal leży w `ui/screens`, mimo że logicznie powinno być osobnym modułem,
- część przepływów nadal zapisuje większy snapshot danych zamiast dedykowanych operacji repository,
- projekt jest w stanie przejściowym, więc nowe funkcje trzeba dodawać ostrożnie, aby nie wzmacniać starego monolitu.

## Rekomendowana kolejność dalszego refactoru

Najbezpieczniejsza kolejność:

1. Dokończyć moduł `feature/inventory`.
2. Przepiąć `VehiclePartsStorageScreen` na `VehiclePartsStorageViewModel`.
3. Wydzielić z `VehiclePartsStorageScreen` pozostałe większe dialogi, karty i szczegóły części.
4. Przenieść AI Assistant z `ui/screens` do osobnego pakietu `ai`.
5. Wydzielić z `GarageRepository` osobne operacje inventory/shopping/repairs.
6. Dopiero potem refactorować `VehicleRepairListScreen`.
7. Następnie refactorować `VehicleDocumentationScreen`.

Nie warto teraz przepisywać wszystkiego naraz. Lepsza strategia to: jeden moduł, jedna odpowiedzialność, pełna kompilacja, szybka walidacja na telefonie.

## Zasady dla nowych funkcji

Przy dodawaniu nowych funkcji warto trzymać się tych zasad:

- nie dodawać nowej logiki biznesowej bezpośrednio do dużych composable,
- jeśli ekran potrzebuje stanu, preferować `UiState` i `Action`,
- jeśli funkcja dotyczy danych, dodawać metodę w repository albo use case,
- jeśli komponent UI pojawia się w kilku miejscach, wynosić go do `ui/components`,
- jeśli logika dotyczy AI, wynosić ją do pakietu `ai`,
- jeśli logika dotyczy plików, importu, OCR lub sieci, nie trzymać jej w Compose.

## Stan docelowy

Docelowo projekt powinien wyglądać mniej więcej tak:

```text
pl.garage.bmwassistant
  core/
  data/
    local/
    repository/
    mapper/
  domain/
    model/
    usecase/
  feature/
    inventory/
    repairs/
    documentation/
    shopping/
    vehicle/
  ai/
  ui/
    components/
    theme/
    navigation/
  update/
```

Nie trzeba osiągać tego od razu. Aktualny refactor jest dobrym pierwszym krokiem, ale wymaga konsekwentnego dokończenia, szczególnie w module inventory.
