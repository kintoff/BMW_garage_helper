package pl.garage.bmwassistant.data

import pl.garage.bmwassistant.model.GarageTask
import pl.garage.bmwassistant.model.ConsumableItem
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea

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

fun sampleRepairsFor(vehicle: Vehicle): List<RepairProject> = listOf(
    sampleRepairFor(vehicle)
)

fun sampleRepairDocumentationFor(vehicle: Vehicle): List<RepairDocumentation> =
    sampleRepairsFor(vehicle).map { repair ->
        RepairDocumentation(
            title = "Dokumentacja: ${repair.title}",
            area = repair.area,
            repairTitle = repair.title,
            summary = "Notatki, zdjecia, linki, czesci i ustalenia dla tej naprawy. Dokumentacja zostaje w historii nawet po zamknieciu lub usunieciu naprawy z aktywnej listy."
        )
    }

fun sampleInventoryPartsFor(vehicle: Vehicle): List<PartInventoryItem> = listOf(
    PartInventoryItem(
        id = "1",
        oemPartNumber = "do ustalenia",
        manufacturerPartNumber = "do ustalenia",
        name = "Sruba mocowania tylnej zwrotnicy",
        manufacturer = "BMW / OEM",
        repairTitle = sampleRepairFor(vehicle).title,
        quantity = 1,
        purchasePrice = "do uzupelnienia",
        realOemUrl = null
    )
)

fun sampleShoppingListFor(vehicle: Vehicle): List<ShoppingListItem> =
    sampleRepairFor(vehicle).partsToIdentify.map { partName ->
        ShoppingListItem(
            partNumber = "do ustalenia",
            name = partName,
            repairTitle = sampleRepairFor(vehicle).title,
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
