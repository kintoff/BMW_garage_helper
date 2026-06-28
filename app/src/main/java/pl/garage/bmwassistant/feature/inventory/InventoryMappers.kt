package pl.garage.bmwassistant.feature.inventory

import androidx.compose.ui.graphics.Color
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.VehicleArea
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class InventoryHistoryEntry(
    val title: String,
    val timestamp: String,
    val quantityLabel: String,
)

internal data class InventoryCategoryUi(
    val area: VehicleArea,
    val label: String,
    val accentColor: Color,
)

internal enum class InventorySearchColumn(val label: String) {
    Id("ID"),
    OemPartNumber("Nr czesci OEM"),
    ManufacturerPartNumber("Nr czesci producenta"),
    Name("Nazwa czesci"),
    Manufacturer("Producent"),
    Repair("Do jakiej naprawy"),
    Quantity("Ilosc"),
    Price("Cena zakupu"),
}

internal fun nextPartId(parts: List<PartInventoryItem>): String =
    ((parts.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()

internal fun nextShoppingItemId(items: List<ShoppingListItem>): String =
    "shopping-${(items.mapNotNull { it.id.removePrefix("shopping-").toIntOrNull() }.maxOrNull() ?: 0) + 1}"

internal fun shoppingRepairCountLabel(count: Int): String = when {
    count == 1 -> "naprawa"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "naprawy"
    else -> "napraw"
}

internal fun shoppingPartCountLabel(count: Int): String = when {
    count == 1 -> "czesc"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "czesci"
    else -> "czesci"
}

internal fun shoppingRepairTotalLabel(items: List<ShoppingListItem>): String {
    val total = items.sumOf { item ->
        (parseShoppingPriceAmount(item.price) ?: 0.0) * item.quantity.coerceAtLeast(0)
    }
    return if (total <= 0.0) {
        "Cena do sprawdzenia"
    } else {
        String.format(Locale.US, "%.2f PLN", total).replace(".", ",")
    }
}

private fun parseShoppingPriceAmount(value: String): Double? {
    val normalized = value
        .replace("PLN", "", ignoreCase = true)
        .replace("zl", "", ignoreCase = true)
        .replace("zł", "", ignoreCase = true)
        .replace(" ", "")
        .replace(",", ".")
    return Regex("""\d+(\.\d+)?""").find(normalized)?.value?.toDoubleOrNull()
}

internal fun List<ShoppingListItem>.shoppingPrimaryArea(): VehicleArea =
    groupBy { it.area }
        .maxByOrNull { (_, values) -> values.size }
        ?.key
        ?: VehicleArea.Service

internal fun PartInventoryItem.storageCategory(availableRepairs: List<RepairProject>): InventoryCategoryUi {
    val repairArea = availableRepairs.firstOrNull { repair ->
        repair.id == repairId || repair.title == repairTitle
    }?.area
    val area = repairArea ?: inferredStorageArea()
    return InventoryCategoryUi(
        area = area,
        label = area.label,
        accentColor = when (area) {
            VehicleArea.Engine -> Color(0xFF63C8FF)
            VehicleArea.Suspension -> Color(0xFFB47CFF)
            VehicleArea.Body -> Color(0xFFFFB86B)
            VehicleArea.Electronics -> Color(0xFF8EA2FF)
            VehicleArea.Service -> Color(0xFF73E48A)
        }
    )
}

internal fun PartInventoryItem.inferredStorageArea(): VehicleArea {
    val text = listOf(name, manufacturer, repairTitle.orEmpty())
        .joinToString(" ")
        .lowercase(Locale.ROOT)
    return when {
        listOf("silnik", "olej", "paliw", "wąż", "waz", "uszczel", "filtr").any { it in text } -> VehicleArea.Engine
        listOf("hamul", "tarcza", "klock", "zacisk").any { it in text } -> VehicleArea.Service
        listOf("wahacz", "zawies", "spręż", "sprez", "amort").any { it in text } -> VehicleArea.Suspension
        listOf("nadwo", "drzwi", "zderzak", "błot", "blot").any { it in text } -> VehicleArea.Body
        listOf("elektr", "czujnik", "moduł", "modul", "przekaź", "przekaz").any { it in text } -> VehicleArea.Electronics
        else -> VehicleArea.Service
    }
}

internal fun InventoryCategoryUi.categoryOrder(): Int = when (area) {
    VehicleArea.Engine -> 0
    VehicleArea.Service -> 1
    VehicleArea.Suspension -> 2
    VehicleArea.Body -> 3
    VehicleArea.Electronics -> 4
}

internal fun PartInventoryItem.withCreatedInventoryTimestamps(): PartInventoryItem {
    val now = System.currentTimeMillis()
    return copy(
        createdAtEpochMillis = createdAtEpochMillis.takeIf { it > 0L } ?: now,
        updatedAtEpochMillis = updatedAtEpochMillis.takeIf { it > 0L } ?: now
    )
}

internal fun Long.formatInventoryTimestamp(): String =
    if (this > 0L) {
        SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.forLanguageTag("pl-PL")).format(Date(this))
    } else {
        "Brak daty"
    }

internal fun PartInventoryItem.defaultHistoryEntries(): List<InventoryHistoryEntry> {
    val title = if (originShoppingItemId.isNullOrBlank()) {
        "Dodano do magazynu"
    } else {
        "Przyjęto do magazynu"
    }
    return listOf(
        InventoryHistoryEntry(
            title = title,
            timestamp = createdAtEpochMillis.formatInventoryTimestamp(),
            quantityLabel = "$quantity szt."
        )
    )
}

internal fun inventoryPurchaseSourceLabel(part: PartInventoryItem): String =
    when {
        !part.originShoppingItemId.isNullOrBlank() -> "Lista zakupów"
        part.realOemUrl?.contains("czescidobmw.pl", ignoreCase = true) == true -> "Sklep partnerski (OEM)"
        part.purchasePrice.isNotBlank() && part.purchasePrice != "do uzupelnienia" -> "Partner OEM"
        else -> "Do uzupełnienia"
    }

internal fun PartInventoryItem.searchValue(column: InventorySearchColumn): String =
    when (column) {
        InventorySearchColumn.Id -> id
        InventorySearchColumn.OemPartNumber -> oemPartNumber
        InventorySearchColumn.ManufacturerPartNumber -> manufacturerPartNumber
        InventorySearchColumn.Name -> name
        InventorySearchColumn.Manufacturer -> manufacturer
        InventorySearchColumn.Repair -> repairTitle.orEmpty()
        InventorySearchColumn.Quantity -> quantity.toString()
        InventorySearchColumn.Price -> purchasePrice
    }

internal fun PartInventoryItem.imageSearchUrl(): String {
    val partNumber = manufacturerPartNumber.ifBlank { oemPartNumber }
    val query = java.net.URLEncoder.encode("$partNumber BMW $manufacturer czesc zdjecie", "UTF-8")
    return "https://www.google.com/search?tbm=isch&q=$query"
}

internal fun InventoryItemInput.toInventoryPart(
    id: String,
    createdAt: Long,
    updatedAt: Long,
    originShoppingItemId: String? = null,
): PartInventoryItem = PartInventoryItem(
    id = id,
    oemPartNumber = oemPartNumber.ifBlank { "do uzupelnienia" },
    manufacturerPartNumber = manufacturerPartNumber.ifBlank { oemPartNumber.ifBlank { "do uzupelnienia" } },
    name = name,
    manufacturer = manufacturer.ifBlank { "do uzupelnienia" },
    repairTitle = repairTitle,
    quantity = quantity.coerceAtLeast(1),
    purchasePrice = purchasePrice.ifBlank { "do uzupelnienia" },
    realOemUrl = realOemUrl,
    photoUri = photoUri,
    repairId = repairId,
    originShoppingItemId = originShoppingItemId,
    locationNote = locationNote,
    createdAtEpochMillis = createdAt,
    updatedAtEpochMillis = updatedAt
)

internal fun ShoppingListItem.toInventoryPart(
    id: String,
    quantity: Int,
    createdAt: Long,
    updatedAt: Long,
): PartInventoryItem = PartInventoryItem(
    id = id,
    oemPartNumber = partNumber.ifBlank { "do uzupelnienia" },
    manufacturerPartNumber = manufacturerPartNumber.ifBlank { partNumber.ifBlank { "do uzupelnienia" } },
    name = name,
    manufacturer = manufacturer.ifBlank { "do uzupelnienia" },
    repairTitle = repairTitle.ifBlank { null },
    quantity = quantity.coerceAtLeast(1),
    purchasePrice = price.ifBlank { "do uzupelnienia" },
    realOemUrl = realOemUrl,
    photoUri = imageUri,
    repairId = repairId.ifBlank { null },
    originShoppingItemId = this.id,
    createdAtEpochMillis = createdAt,
    updatedAtEpochMillis = updatedAt
)
