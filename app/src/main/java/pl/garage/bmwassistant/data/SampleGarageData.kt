package pl.garage.bmwassistant.data

import pl.garage.bmwassistant.model.GarageTask
import pl.garage.bmwassistant.model.ConsumableItem
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.YoutubeVideo

fun sampleTasksFor(vehicle: Vehicle): List<GarageTask> {
    val vehicleName = vehicle.model.ifBlank { vehicle.displayName.ifBlank { "BMW" } }
    return listOf(
        GarageTask(
            title = "Uzupelnic szczegoly profilu auta",
            vehicle = vehicleName,
            status = "Profil"
        ),
        GarageTask(
            title = "Dodac pierwszy projekt naprawy",
            vehicle = vehicleName,
            status = "Nastepny krok"
        ),
        GarageTask(
            title = "Przygotowac sekcje czesci i dokumentow",
            vehicle = vehicleName,
            status = "Planowanie"
        )
    )
}

fun sampleRepairFor(vehicle: Vehicle): RepairProject {
    val vehicleName = vehicle.displayName.ifBlank { "BMW E60 520d" }
    return RepairProject(
        title = "Tylna zwrotnica lewa",
        area = VehicleArea.Suspension,
        vehicleName = vehicleName,
        status = "Planowane",
        priority = "Wysoki priorytet",
        problemDescription = "Sruba w tylnej zwrotnicy lewego kola jest mocno skorodowana. Trzeba ustalic, czy element da sie bezpiecznie odkrecic, jakie czesci sa jednorazowe i co przygotowac przed praca.",
        goal = "Zebrac numer czesci, metode demontazu, ryzyka, narzedzia i liste zakupowa przed rozpoczeciem pracy przy zawieszeniu.",
        checklist = listOf(
            "Zrobic zdjecia zwrotnicy i ulozenia wahaczy",
            "Sprawdzic schemat tylnej osi w RealOEM",
            "Ustalic numer sruby, nakretki i elementow jednorazowych",
            "Przygotowac liste narzedzi i penetrant",
            "Sprawdzic momenty dokrecania i potrzebe geometrii"
        ),
        partsToIdentify = listOf(
            "Sruba mocowania tylnej zwrotnicy",
            "Nakretka jednorazowa",
            "Ewentualne tuleje lub elementy mocowania wahacza"
        ),
        documentsToCollect = listOf(
            "RealOEM - tylna os E60",
            "Instrukcja demontazu tylnej zwrotnicy",
            "Notatka z momentami dokrecania",
            "Zdjecia przed demontazem"
        )
    )
}

fun sampleRepairsFor(vehicle: Vehicle): List<RepairProject> {
    val vehicleName = vehicle.displayName.ifBlank { "BMW E61 520d" }
    return listOf(
        RepairProject(
            title = "Wymiana swiec zarowych",
            area = VehicleArea.Engine,
            vehicleName = vehicleName,
            status = "W trakcie",
            priority = "Wysoki priorytet",
            problemDescription = "Silnik ciezko odpala na zimno i nierowno pracuje na wolnych obrotach. Podejrzenie uszkodzonych swiec zarowych.",
            goal = "Wymienic swiece, sprawdzic wtyczki i skasowac bledy po naprawie.",
            checklist = listOf(
                "Demontaz oslony silnika",
                "Odlaczenie wtyczek swiec",
                "Wykrecenie swiec zarowych",
                "Montaz nowych swiec",
                "Kasowanie bledow",
                "Kontrola zimnego rozruchu"
            ),
            partsToIdentify = listOf(
                "Swieca zarowa Bosch",
                "Smar ceramiczny",
                "Uszczelka kolektora"
            ),
            documentsToCollect = listOf(
                "TIS - wymiana swiec zarowych",
                "Instrukcja producenta PDF",
                "Tabela momentow dokrecania"
            )
        ),
        RepairProject(
            title = "Wymiana maglownicy",
            area = VehicleArea.Suspension,
            vehicleName = vehicleName,
            status = "Planowane",
            priority = "Do ustalenia",
            problemDescription = "Luz i stuki w ukladzie kierowniczym. Trzeba przygotowac czesci, plyn i procedure odpowietrzenia.",
            goal = "Zebrac dokumentacje, czesci i momenty przed rozpoczeciem pracy.",
            checklist = emptyList(),
            partsToIdentify = listOf("Maglownica", "Plyn wspomagania", "Sruby mocujace"),
            documentsToCollect = listOf("Procedura demontazu maglownicy", "Geometria po montazu")
        ),
        RepairProject(
            title = "Wymiana oleju i filtrow",
            area = VehicleArea.Service,
            vehicleName = vehicleName,
            status = "Zakonczona",
            priority = "Historia",
            problemDescription = "Okresowy serwis olejowy wykonany zgodnie z planem.",
            goal = "Zapisac uzyte czesci i date kolejnego serwisu.",
            checklist = listOf("Spuszczenie oleju", "Wymiana filtra", "Zalanie oleju", "Reset inspekcji"),
            partsToIdentify = emptyList(),
            documentsToCollect = listOf("Paragon", "Specyfikacja oleju")
        )
    )
}

fun sampleRepairDocumentationFor(vehicle: Vehicle): List<RepairDocumentation> =
    sampleRepairsFor(vehicle).map { repair ->
        RepairDocumentation(
            title = "Dokumentacja: ${repair.title}",
            area = repair.area,
            repairTitle = repair.title,
            repairId = repair.id,
            summary = "Notatki, zdjecia, linki, czesci i ustalenia dla tej naprawy. Dokumentacja zostaje w historii nawet po zamknieciu lub usunieciu naprawy z aktywnej listy.",
            tisDocuments = if (repair.title.contains("swiec", ignoreCase = true)) {
                listOf(
                    TisDocumentationLink(
                        title = "Wymiana swiec zarowych - TIS",
                        url = "https://newtis.info"
                    )
                )
            } else {
                emptyList()
            },
            torqueSpecs = if (repair.title.contains("swiec", ignoreCase = true)) {
                List(6) { index ->
                    TorqueSpec(
                        component = "Swieca zarowa ${index + 1}",
                        torque = "25 Nm",
                        source = "TIS",
                        notes = "Dokrecac na zimnym silniku."
                    )
                }
            } else {
                emptyList()
            },
            youtubeVideos = if (repair.title.contains("swiec", ignoreCase = true)) {
                listOf(
                    YoutubeVideo(
                        title = "How To - Replace Glow Plugs BMW E61 520d",
                        url = "https://youtube.com",
                        note = "YouTube - 12:45"
                    )
                )
            } else {
                emptyList()
            },
            personalNotes = if (repair.title.contains("swiec", ignoreCase = true)) {
                listOf(
                    PersonalDocumentationItem(
                        id = "glow-plugs-pdf-1",
                        type = PersonalDocumentationItemType.Document,
                        title = "Instrukcja_wymiana_swiec.pdf",
                        text = "1.2 MB"
                    ),
                    PersonalDocumentationItem(
                        id = "glow-plugs-pdf-2",
                        type = PersonalDocumentationItemType.Document,
                        title = "Schemat_podlaczenia.pdf",
                        text = "856 KB"
                    ),
                    PersonalDocumentationItem(
                        id = "glow-plugs-photo-1",
                        type = PersonalDocumentationItemType.Photo,
                        title = "Demontaz kolektora"
                    ),
                    PersonalDocumentationItem(
                        id = "glow-plugs-photo-2",
                        type = PersonalDocumentationItemType.Photo,
                        title = "Stara swieca"
                    ),
                    PersonalDocumentationItem(
                        id = "glow-plugs-photo-3",
                        type = PersonalDocumentationItemType.Photo,
                        title = "Gniazdo swiecy"
                    ),
                    PersonalDocumentationItem(
                        id = "glow-plugs-photo-4",
                        type = PersonalDocumentationItemType.Photo,
                        title = "Kolektor"
                    ),
                    PersonalDocumentationItem(
                        id = "glow-plugs-photo-5",
                        type = PersonalDocumentationItemType.Photo,
                        title = "Wtyczki"
                    ),
                    PersonalDocumentationItem(
                        id = "glow-plugs-video-1",
                        type = PersonalDocumentationItemType.Video,
                        title = "Film z rozruchu"
                    )
                )
            } else {
                emptyList()
            }
        )
    }

fun sampleInventoryPartsFor(vehicle: Vehicle): List<PartInventoryItem> = listOf(
    PartInventoryItem(
        id = "1",
        oemPartNumber = "0 250 203 009",
        manufacturerPartNumber = "0 250 203 009",
        name = "Swieca zarowa Bosch",
        manufacturer = "Bosch",
        repairTitle = sampleRepairsFor(vehicle).first().title,
        repairId = sampleRepairsFor(vehicle).first().id,
        quantity = 2,
        purchasePrice = "do uzupelnienia",
        realOemUrl = null
    )
)

fun sampleShoppingListFor(vehicle: Vehicle): List<ShoppingListItem> =
    sampleRepairsFor(vehicle).first().partsToIdentify.map { partName ->
        val repair = sampleRepairsFor(vehicle).first()
        ShoppingListItem(
            partNumber = "do ustalenia",
            name = partName,
            repairTitle = repair.title,
            repairId = repair.id,
            area = repair.area,
            quantity = 1,
            source = "RealOEM"
        )
    }

fun sampleConsumablesFor(): List<ConsumableItem> = listOf(
    ConsumableItem(
        id = "consumable-penetrant",
        name = "Penetrant do srub",
        producer = "do uzupelnienia",
        quantity = "1 szt.",
        purchasePrice = "do uzupelnienia",
        notes = "Przydatny przy skorodowanych srubach zawieszenia."
    ),
    ConsumableItem(
        id = "consumable-brake-cleaner",
        name = "Zmywacz / cleaner",
        producer = "do uzupelnienia",
        quantity = "1 szt.",
        purchasePrice = "do uzupelnienia",
        notes = "Material eksploatacyjny do czyszczenia elementow przed montazem."
    )
)
