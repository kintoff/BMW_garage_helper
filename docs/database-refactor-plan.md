# Plan refaktoryzacji bazy danych

## Cel

Docelowo aplikacja ma:

- trzymac dane trwale na telefonie,
- miec jedna mala baze glowna aplikacji,
- miec osobna baze dla kazdego auta,
- umiec wykonac backup jednego auta,
- umiec zaimportowac gotowe auto do programu.

## Docelowa architektura

### 1. Baza glowna aplikacji

Plik:

- `garage_catalog.db`

Odpowiedzialnosc:

- lista aut,
- metadane aut,
- lokalizacja pliku bazy danego auta,
- informacje o imporcie i backupie,
- stan aktywnosci auta w aplikacji.

### 2. Osobna baza dla kazdego auta

Plik:

- `vehicles/<vehicleId>/vehicle.db`

Odpowiedzialnosc:

- naprawy,
- dokumentacja napraw,
- lista zakupow,
- magazyn czesci,
- media metadata,
- notatki i linki,
- archiwum danych tego auta.

### 3. Folder zasobow auta

Folder:

- `vehicles/<vehicleId>/files/`

Odpowiedzialnosc:

- zdjecia,
- filmy,
- dokumenty,
- zalaczniki importowane do dokumentacji,
- obrazy schematow i momentow.

Zasada:

- baza przechowuje tylko sciezki lub identyfikatory plikow,
- duze pliki nie wchodza do SQLite jako blob.

## Baza glowna aplikacji

### Tabela `vehicles`

Najwazniejsze pola:

- `vehicleId` - stale UUID auta, klucz glowny,
- `brand`,
- `model`,
- `generation`,
- `engine`,
- `year`,
- `vin`,
- `mileage`,
- `note`,
- `partsCatalogUrl`,
- `databasePath` - sciezka do `vehicle.db`,
- `filesDirectoryPath` - sciezka do folderu plikow auta,
- `createdAt`,
- `updatedAt`,
- `lastBackupAt`,
- `importSource`,
- `isArchived`.

### Dlaczego stale `vehicleId`

To rozwiazuje najwazniejszy problem obecnej implementacji:

- zmiana VIN,
- zmiana nazwy auta,
- zmiana generacji

nie moze juz odrywac napraw i dokumentacji od auta.

## Baza pojedynczego auta

## Tabela `repair_projects`

Pola:

- `repairId` - stale UUID naprawy,
- `title`,
- `area`,
- `status`,
- `priority`,
- `problemDescription`,
- `goal`,
- `createdAt`,
- `updatedAt`,
- `completedAt`,
- `sortOrder`,
- `isArchived`.

## Tabela `repair_checkpoints`

Pola:

- `checkpointId`,
- `repairId`,
- `text`,
- `isDone`,
- `sortOrder`.

Relacja:

- wiele checkpointow do jednej naprawy.

## Tabela `repair_parts_to_identify`

Pola:

- `itemId`,
- `repairId`,
- `text`,
- `sortOrder`.

## Tabela `repair_documents_to_collect`

Pola:

- `itemId`,
- `repairId`,
- `text`,
- `sortOrder`.

## Tabela `repair_documentation`

Pola:

- `documentationId`,
- `repairId`,
- `title`,
- `area`,
- `repairTitleSnapshot`,
- `summary`,
- `userNotes`,
- `createdAt`,
- `updatedAt`.

Zasada:

- dokumentacja nadal zyje po zakonczeniu naprawy,
- dokumentacja jest zawsze przypieta przez `repairId`.

## Tabela `shopping_list_items`

Pola:

- `shoppingItemId`,
- `repairId`,
- `partNumber`,
- `manufacturerPartNumber`,
- `name`,
- `manufacturer`,
- `quantity`,
- `source`,
- `price`,
- `imageUri`,
- `shopUrl`,
- `realOemUrl`,
- `status` - np. planned, ordered, received, archived,
- `archivedInDocumentation` - flaga pomocnicza,
- `createdAt`,
- `updatedAt`.

## Tabela `inventory_parts`

Pola:

- `inventoryPartId`,
- `originShoppingItemId`,
- `repairId`,
- `oemPartNumber`,
- `manufacturerPartNumber`,
- `name`,
- `manufacturer`,
- `quantity`,
- `purchasePrice`,
- `realOemUrl`,
- `photoUri`,
- `locationNote`,
- `createdAt`,
- `updatedAt`.

## Tabela `tis_links`

Pola:

- `tisLinkId`,
- `documentationId`,
- `title`,
- `url`,
- `sortOrder`.

## Tabela `youtube_videos`

Pola:

- `youtubeVideoId`,
- `documentationId`,
- `title`,
- `url`,
- `note`,
- `sortOrder`.

## Tabela `personal_documentation_items`

Pola:

- `itemId`,
- `documentationId`,
- `type`,
- `title`,
- `text`,
- `uri`,
- `url`,
- `sortOrder`,
- `createdAt`.

## Tabela `torque_spec_tables`

Pola:

- `tableId`,
- `documentationId`,
- `title`,
- `diagramImageUri`,
- `sortOrder`.

## Tabela `torque_specs`

Pola:

- `torqueSpecId`,
- `tableId`,
- `component`,
- `type`,
- `thread`,
- `tighteningSpecifications`,
- `torque`,
- `source`,
- `notes`,
- `sortOrder`.

## Tabela `torque_diagram_assignments`

Pola:

- `assignmentId`,
- `tableId`,
- `torqueSpecId`,
- `xRatio`,
- `yRatio`.

## Tabela `repair_media_assets`

Ta tabela bedzie wspolnym rejestrem plikow w bazie auta.

Pola:

- `assetId`,
- `ownerType` - np. documentationItem, torqueTable, inventoryPart,
- `ownerId`,
- `fileName`,
- `relativePath`,
- `mimeType`,
- `sizeBytes`,
- `checksum`,
- `createdAt`.

Cel:

- latwiejszy backup,
- latwiejszy import,
- mozliwosc sprzatania osieroconych plikow.

## Backup jednego auta

Format paczki:

- `manifest.json`
- `vehicle.db`
- `files/...`

Manifest powinien zawierac:

- `formatVersion`,
- `vehicleId`,
- `vin`,
- `displayName`,
- `exportedAt`,
- `appVersion`,
- `databaseFileName`,
- `filesDirectoryName`.

## Import auta

Kroki:

1. Wybranie paczki ZIP.
2. Walidacja manifestu.
3. Nadanie nowego lokalnego `vehicleId` albo pozostawienie starego przy imporcie 1:1.
4. Skopiowanie `vehicle.db` do `vehicles/<vehicleId>/`.
5. Skopiowanie `files/` do `vehicles/<vehicleId>/files/`.
6. Rejestracja auta w `garage_catalog.db`.

## Migracja ze starego modelu

Obecne storage:

- `VehicleStorage`
- `RepairProjectStorage`
- `PartInventoryStorage`

Plan migracji:

1. Odczyt listy aut ze starego `VehicleStorage`.
2. Dla kazdego auta utworzenie nowego `vehicleId`.
3. Utworzenie nowego pliku `vehicle.db`.
4. Przeniesienie:
   - napraw,
   - dokumentacji,
   - checkpointow,
   - list zakupow,
   - magazynu czesci,
   - linkow,
   - tabel momentow,
   - notatek.
5. Rejestracja auta w `garage_catalog.db`.
6. Oznaczenie migracji jako wykonanej, aby nie uruchamiala sie ponownie.

## Kolejnosc implementacji

### Etap 1

- dodanie Room do projektu,
- utworzenie `GarageCatalogDatabase`,
- utworzenie encji `VehicleCatalogEntity`,
- utworzenie `VehicleDatabaseDescriptor`,
- utworzenie `VehicleDatabaseManager`.

### Etap 2

- utworzenie `VehicleDatabase`,
- encje napraw,
- encje dokumentacji,
- encje listy zakupow,
- encje magazynu czesci.

### Etap 3

- migrator ze starych `SharedPreferences`,
- pierwszy start po migracji.

### Etap 4

- przepiecie listy aut na nowa baze,
- przepiecie ekranu auta,
- przepiecie napraw i dokumentacji.

### Etap 5

- backup jednego auta,
- import jednego auta,
- sprzatanie osieroconych plikow.

## Pierwszy krok implementacyjny

Najblizszy krok w kodzie:

- dodac zaleznosci Room,
- utworzyc pakiet `database/catalog`,
- zbudowac baze glowna aplikacji,
- dodac `VehicleDatabaseManager`, ktory umie:
  - stworzyc baze nowego auta,
  - zwrocic sciezke do bazy auta,
  - przygotowac katalog plikow auta.
