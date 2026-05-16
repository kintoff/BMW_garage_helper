# Struktura danych aplikacji

Ten dokument opisuje strukture danych dla BMW Garage Assistant. Ma pomoc zrozumiec, jakie informacje aplikacja przechowuje teraz i jak beda ze soba powiazane po migracji do Room/SQLite.

Na obecnym etapie aplikacja zapisuje profile aut, naprawy, czesci i dokumentacje napraw w prostym lokalnym magazynie. Docelowo te dane przeniesiemy do Room/SQLite.

## Glowna idea

Aplikacja jest prywatnym systemem garazowym. Najwazniejszym obiektem jest auto, a do auta podpinamy naprawy, zdjecia, czesci, dokumenty, logi i notatki.

```text
Garage
  |
  +-- Vehicle
        |
        +-- RepairProject
        +-- Part
        +-- RepairDocumentation
        +-- GaragePhoto
        +-- DocumentReference
        +-- DiagnosticSession
        +-- Note
        +-- Task
```

## Vehicle

Profil auta.

Przyklad: BMW E60 520d M47N2.

Pola:

```text
id
brand
model
generation
bodyCode
engineCode
engineDescription
year
vin
mileage
fuelType
transmission
color
registrationNumber
status
notes
createdAt
updatedAt
```

Przyklad:

```json
{
  "id": "vehicle-e60",
  "brand": "BMW",
  "model": "520d",
  "generation": "E60",
  "bodyCode": "E60",
  "engineCode": "M47N2",
  "engineDescription": "2.0d",
  "year": 2006,
  "vin": "WBAXXXXXXXXXXXXXX",
  "mileage": 285000,
  "fuelType": "diesel",
  "transmission": "manual",
  "status": "active_repair",
  "notes": "Pierwsze auto testowe aplikacji."
}
```

Relacje:

- jedno auto ma wiele napraw,
- jedno auto ma wiele czesci,
- jedno auto ma wiele zdjec,
- jedno auto ma wiele dokumentow,
- jedno auto ma wiele sesji diagnostycznych.

## RepairProject

Projekt naprawy lub diagnostyki.

Przyklad: tylna zwrotnica lewa, zardzewiala sruba.

Pola:

```text
id
vehicleId
title
problemDescription
status
priority
mileage
startedAt
finishedAt
diagnosisSummary
repairSummary
result
createdAt
updatedAt
```

Statusy:

```text
planned
in_progress
waiting_for_parts
blocked
done
cancelled
```

Przyklad:

```json
{
  "id": "repair-e60-rear-knuckle-left",
  "vehicleId": "vehicle-e60",
  "title": "Tylna zwrotnica lewa - zardzewiala sruba",
  "problemDescription": "Sruba w tylnej zwrotnicy jest mocno skorodowana. Trzeba ustalic numer czesci i metode demontazu.",
  "status": "planned",
  "priority": "high",
  "mileage": 285000
}
```

Relacje:

- naprawa nalezy do jednego auta,
- naprawa moze miec wiele zdjec,
- naprawa moze miec wiele czesci,
- naprawa moze miec wiele zadan,
- naprawa moze miec jedna glowna dokumentacje naprawy z linkami, schematami, momentami dokrecen, filmami i notatkami.

## RepairDocumentation

Dokumentacja przypisana do konkretnej naprawy. To obecnie najwazniejszy kontener wiedzy w aplikacji.

Pola:

```text
title
area
repairTitle
summary
tisLinks
tisDocuments
torqueSpecs
torqueDiagramImageUri
torqueDiagramAssignments
torqueTables
youtubeLinks
youtubeVideos
personalNotes
```

Uwagi:

- `tisLinks`, `torqueSpecs`, `torqueDiagramImageUri`, `torqueDiagramAssignments` i `youtubeLinks` sa polami legacy utrzymywanymi dla zgodnosci ze starszym zapisem.
- Nowy zapis uzywa `tisDocuments`, `torqueTables`, `youtubeVideos` i `personalNotes`.
- Jedna dokumentacja naprawy moze zawierac wiele tabel momentow dokrecen.
- Kazda tabela momentow moze miec wlasny schemat i wlasne przypisania punktow.
- Dokumentacja moze byc eksportowana i importowana jako jeden pakiet `.bmwdoc.zip`.

Przyklad:

```json
{
  "title": "Dokumentacja - swiece zarowe",
  "area": "Engine",
  "repairTitle": "Wymiana swiec zarowych",
  "summary": "Materialy potrzebne do naprawy ukladu swiec zarowych.",
  "tisDocuments": [
    {
      "title": "TIS - demontaz kolektora",
      "url": "https://www.newtis.info/..."
    }
  ],
  "torqueTables": [],
  "youtubeVideos": [],
  "personalNotes": []
}
```

## TisDocumentationLink

Link do procedury, schematu albo innego materialu TIS z czytelna nazwa.

Pola:

```text
title
url
```

Przyklad:

```json
{
  "title": "TIS - momenty dokrecen kolektora",
  "url": "https://www.newtis.info/..."
}
```

## TorqueSpecTable

Tabela momentow dokrecen. Jedna naprawa moze miec kilka takich tabel, bo moze dotyczyc kilku schematow lub podzespolow.

Pola:

```text
id
title
torqueSpecs
diagramImageUri
diagramAssignments
```

Relacje:

- tabela nalezy do jednej dokumentacji naprawy,
- tabela ma wiele rekordow `TorqueSpec`,
- tabela moze miec jeden schemat,
- tabela moze miec wiele przypisan punktow `TorqueDiagramAssignment`.

## TorqueSpec

Pojedynczy rekord momentu dokrecenia.

Pola:

```text
component
type
thread
tighteningSpecifications
torque
source
notes
```

Przyklad:

```json
{
  "component": "3AZ Connection bolt",
  "type": "M8",
  "thread": "",
  "tighteningSpecifications": "",
  "torque": "25 Nm",
  "source": "TIS screenshot",
  "notes": ""
}
```

## TorqueDiagramAssignment

Przypisanie rekordu momentu dokrecenia do punktu na schemacie.

Pola:

```text
torqueSpecIndex
xRatio
yRatio
```

`xRatio` i `yRatio` sa zapisane jako wartosci wzgledne obrazu, dzieki czemu punkt zachowuje pozycje po zmianie rozmiaru podgladu.

## YoutubeVideo

Film YouTube przypisany do dokumentacji naprawy.

Pola:

```text
title
url
note
```

Tytul moze zostac pobrany automatycznie z YouTube przy dodawaniu linku. Uzytkownik moze go pozniej edytowac.

## PersonalDocumentationItem

Wpis w sekcji notatek wlasnych. Obsluguje tekst, zdjecia, filmy, dokumenty, linki i dowolne pliki.

Pola:

```text
id
type
title
text
uri
url
```

Typy:

```text
Text
Photo
Video
Document
Link
File
```

Uwagi:

- zdjecia i filmy sa pokazywane jako miniaturki,
- zdjecia i filmy maja podglad po kliknieciu,
- dokumenty i pliki sa otwierane przez systemowa liste aplikacji,
- linki nie pokazuja surowego adresu na liscie, zeby nie zaburzac widoku.

## RepairDocumentationPackage

Eksportowany pakiet dokumentacji jednej naprawy.

Format:

```text
*.bmwdoc.zip
```

Zawartosc:

```text
manifest.json
assets/
```

`manifest.json` zawiera dane dokumentacji, a `assets/` zawiera zalaczniki skopiowane z lokalnych `Uri`, np. schematy, zdjecia, filmy, PDF-y i inne pliki.

Przy imporcie zalaczniki sa kopiowane do prywatnego folderu aplikacji, a dokumentacja zostaje przypisana do aktualnie otwartej naprawy.

## Task

Pojedyncze zadanie do wykonania.

Przyklad: sprawdzic numer sruby w RealOEM.

Pola:

```text
id
vehicleId
repairProjectId
title
description
status
priority
dueDate
createdAt
completedAt
```

Statusy:

```text
todo
in_progress
done
cancelled
```

Przyklad:

```json
{
  "id": "task-check-bolt-realoem",
  "vehicleId": "vehicle-e60",
  "repairProjectId": "repair-e60-rear-knuckle-left",
  "title": "Ustalic numer sruby tylnej zwrotnicy",
  "status": "todo",
  "priority": "high"
}
```

## Part

Czesc, sruba, nakretka, uszczelka, plyn albo narzedzie eksploatacyjne potrzebne do auta lub naprawy.

Pola:

```text
id
vehicleId
repairProjectId
name
partNumber
manufacturer
quantity
status
source
url
price
notes
createdAt
updatedAt
```

Statusy:

```text
to_identify
to_buy
ordered
received
installed
not_needed
```

Przyklad:

```json
{
  "id": "part-rear-knuckle-bolt",
  "vehicleId": "vehicle-e60",
  "repairProjectId": "repair-e60-rear-knuckle-left",
  "name": "Sruba mocowania tylnej zwrotnicy",
  "partNumber": "do ustalenia",
  "quantity": 1,
  "status": "to_identify",
  "source": "RealOEM"
}
```

## GaragePhoto

Zdjecie auta, usterki, czesci, wtyczki, schematu albo etapu naprawy.

Pola:

```text
id
vehicleId
repairProjectId
filePath
title
description
photoType
tags
takenAt
createdAt
```

Typy zdjec:

```text
vehicle
problem
part
connector
wiring
before
after
reference
```

Przyklad:

```json
{
  "id": "photo-rusty-bolt-left-rear",
  "vehicleId": "vehicle-e60",
  "repairProjectId": "repair-e60-rear-knuckle-left",
  "filePath": "/photos/e60/rear-left-rusty-bolt.jpg",
  "title": "Zardzewiala sruba tylnej zwrotnicy",
  "photoType": "problem",
  "tags": ["E60", "tylna os", "zwrotnica", "sruba", "korozja"]
}
```

## DocumentReference

Dokument, link, PDF, strona RealOEM, film YouTube, notatka z TIS albo schemat.

Pola:

```text
id
vehicleId
repairProjectId
title
type
source
url
localFilePath
description
tags
createdAt
updatedAt
```

Typy:

```text
realoem
youtube
forum
pdf
tis_note
wiring_diagram
service_note
web_page
```

Przyklad:

```json
{
  "id": "doc-realoem-rear-axle",
  "vehicleId": "vehicle-e60",
  "repairProjectId": "repair-e60-rear-knuckle-left",
  "title": "RealOEM - tylna os E60",
  "type": "realoem",
  "source": "RealOEM",
  "url": "https://www.realoem.com/..."
}
```

## DiagnosticSession

Sesja diagnostyczna auta.

Przyklad: odczyt bledow DDE, import logow, obserwacja parametrow live.

Pola:

```text
id
vehicleId
repairProjectId
title
toolName
sessionType
mileage
startedAt
finishedAt
summary
rawFilePath
createdAt
```

Typy sesji:

```text
fault_read
live_data
log_import
obd_scan
manual_measurement
```

Przyklad:

```json
{
  "id": "diag-e60-dde-2026-05-05",
  "vehicleId": "vehicle-e60",
  "repairProjectId": null,
  "title": "Odczyt bledow DDE",
  "toolName": "ISTA",
  "sessionType": "fault_read",
  "mileage": 285000,
  "summary": "Do analizy bledy ukladu swiec zarowych."
}
```

## FaultCode

Kod bledu znaleziony w sesji diagnostycznej.

Pola:

```text
id
diagnosticSessionId
vehicleId
module
code
description
status
freezeFrame
notes
createdAt
```

Przyklad:

```json
{
  "id": "fault-dde-glow-plug",
  "diagnosticSessionId": "diag-e60-dde-2026-05-05",
  "vehicleId": "vehicle-e60",
  "module": "DDE",
  "code": "do uzupelnienia",
  "description": "Blad ukladu swiec zarowych",
  "status": "active"
}
```

## LiveDataSample

Pojedynczy pomiar parametru z logu albo live data.

Na poczatku mozemy tego nie robic recznie. Bedzie potrzebne przy imporcie logow.

Pola:

```text
id
diagnosticSessionId
timestampMs
parameterName
value
unit
```

Przyklady parametrow dla M47N2:

```text
MAF actual
boost pressure requested
boost pressure actual
rail pressure requested
rail pressure actual
coolant temperature
EGR duty cycle
injector correction cylinder 1
injector correction cylinder 2
injector correction cylinder 3
injector correction cylinder 4
```

## Note

Dowolna notatka uzytkownika.

Pola:

```text
id
vehicleId
repairProjectId
title
content
tags
createdAt
updatedAt
```

Przyklad:

```json
{
  "id": "note-e60-rear-suspension",
  "vehicleId": "vehicle-e60",
  "repairProjectId": "repair-e60-rear-knuckle-left",
  "title": "Uwagi do tylnego zawieszenia",
  "content": "Przed demontazem spryskac sruby penetrantem i zrobic zdjecia ulozenia wahaczy.",
  "tags": ["E60", "tylna os", "zawieszenie"]
}
```

## Tool

Narzedzie potrzebne do naprawy albo diagnostyki.

Pola:

```text
id
name
category
size
notes
```

Przyklad:

```json
{
  "id": "tool-torque-wrench",
  "name": "Klucz dynamometryczny",
  "category": "mechaniczne",
  "size": "20-200 Nm",
  "notes": "Potrzebny do prac przy zawieszeniu."
}
```

## RepairTool

Powiazanie narzedzia z konkretna naprawa.

Pola:

```text
repairProjectId
toolId
required
notes
```

## Tag

Tagi ulatwiaja wyszukiwanie.

Przyklady:

```text
E60
M47N2
DDE
tylna os
zwrotnica
sruba
korozja
EGR
MAF
turbo
DPF
schemat elektryczny
RealOEM
```

Na poczatku tagi mozemy trzymac jako liste tekstowa w obiektach. Pozniej mozna zrobic osobna tabele tagow.

## Minimalny model na najblizszy etap

Nie musimy wdrazac wszystkiego od razu. Najblizszy sensowny krok to:

```text
Vehicle
RepairProject
RepairDocumentation
Task
Part
GaragePhoto
DocumentReference
Note
```

Dopiero potem:

```text
DiagnosticSession
FaultCode
LiveDataSample
Tool
RepairTool
```

## Docelowe tabele Room/SQLite

Kiedy przeniesiemy aplikacje do Room, struktura moze wygladac tak:

```text
vehicles
repair_projects
repair_documentation
tis_documentation_links
torque_spec_tables
torque_specs
torque_diagram_assignments
youtube_videos
personal_documentation_items
tasks
parts
garage_photos
document_references
diagnostic_sessions
fault_codes
live_data_samples
notes
tools
repair_tools
```

Relacje:

```text
vehicles.id -> repair_projects.vehicleId
repair_projects.id -> repair_documentation.repairProjectId
repair_documentation.id -> tis_documentation_links.repairDocumentationId
repair_documentation.id -> torque_spec_tables.repairDocumentationId
repair_documentation.id -> youtube_videos.repairDocumentationId
repair_documentation.id -> personal_documentation_items.repairDocumentationId
torque_spec_tables.id -> torque_specs.torqueSpecTableId
torque_spec_tables.id -> torque_diagram_assignments.torqueSpecTableId
vehicles.id -> parts.vehicleId
vehicles.id -> garage_photos.vehicleId
vehicles.id -> document_references.vehicleId
vehicles.id -> diagnostic_sessions.vehicleId
vehicles.id -> notes.vehicleId

repair_projects.id -> tasks.repairProjectId
repair_projects.id -> parts.repairProjectId
repair_projects.id -> garage_photos.repairProjectId
repair_projects.id -> document_references.repairProjectId
repair_projects.id -> notes.repairProjectId
repair_projects.id -> diagnostic_sessions.repairProjectId

diagnostic_sessions.id -> fault_codes.diagnosticSessionId
diagnostic_sessions.id -> live_data_samples.diagnosticSessionId
```

## Zasada projektowa

Najpierw wszystko przypinamy do auta. Dopiero potem do konkretnej naprawy.

Dzieki temu mozna miec:

- zdjecie przypisane tylko do auta,
- czesc przypisana tylko do auta,
- notatke przypisana tylko do auta,
- albo te same rzeczy przypisane dokladniej do konkretnego projektu naprawy.
