package pl.garage.bmwassistant.ui.screens

import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.VehicleArea
import java.util.Locale

internal fun PartInventoryItem.stableId(): String =
    id.ifBlank { "$repairId|$repairTitle|$oemPartNumber|$manufacturerPartNumber|$name" }

fun ShoppingListItem.stableId(): String =
    id.ifBlank { "$repairTitle|$partNumber|$manufacturerPartNumber|$name" }

fun ShoppingListItem.toInventoryPart(
    nextId: String,
    receivedQuantity: Int = quantity,
): PartInventoryItem {
    val now = System.currentTimeMillis()
    return PartInventoryItem(
        id = nextId,
        oemPartNumber = partNumber.ifBlank { "do uzupelnienia" },
        manufacturerPartNumber = manufacturerPartNumber.ifBlank { partNumber.ifBlank { "do uzupelnienia" } },
        name = name,
        manufacturer = manufacturer.ifBlank { "do uzupelnienia" },
        repairTitle = repairTitle.ifBlank { null },
        quantity = receivedQuantity.coerceAtLeast(1),
        purchasePrice = price.ifBlank { "do uzupelnienia" },
        realOemUrl = realOemUrl,
        photoUri = imageUri,
        repairId = repairId.ifBlank { null },
        originShoppingItemId = stableId(),
        createdAtEpochMillis = now,
        updatedAtEpochMillis = now
    )
}

internal fun List<ShoppingListItem>.primaryArea(): VehicleArea =
    groupBy { it.area }
        .maxByOrNull { (_, items) -> items.size }
        ?.key
        ?: VehicleArea.Service

internal fun List<ShoppingListItem>.areaSummaryLabel(): String? {
    val distinctAreas = map { it.area }.distinct()
    return when {
        distinctAreas.isEmpty() -> null
        distinctAreas.size == 1 -> distinctAreas.first().label
        else -> "${distinctAreas.size} obszary"
    }
}

internal fun shoppingTotalLabel(items: List<ShoppingListItem>): String {
    val total = items.sumOf { item ->
        item.totalPriceAmount()
    }
    return if (total > 0.0) {
        "Wartosc: ${"%.2f".format(Locale.US, total).replace('.', ',')} PLN"
    } else {
        "Wartosc do uzupelnienia"
    }
}

internal fun ShoppingListItem.totalPriceAmount(): Double {
    val unitPrice = parsePriceAmount(price) ?: return 0.0
    return unitPrice * quantity.coerceAtLeast(0)
}

internal fun ShoppingListItem.totalPriceLabel(): String =
    totalPriceAmount()
        .takeIf { it > 0.0 }
        ?.let { "${"%.2f".format(Locale.US, it).replace('.', ',')} PLN" }
        ?: price.ifBlank { "Cena do sprawdzenia" }

internal fun ShoppingListItem.unitPriceInfoLabel(): String? =
    price
        .takeIf { quantity > 1 && parsePriceAmount(it) != null }
        ?.let { "$it / szt." }

internal fun PartInventoryItem.totalPurchasePriceAmount(): Double {
    val unitPrice = parsePriceAmount(purchasePrice) ?: return 0.0
    return unitPrice * quantity.coerceAtLeast(0)
}

internal fun PartInventoryItem.totalPurchasePriceLabel(): String =
    totalPurchasePriceAmount()
        .takeIf { it > 0.0 }
        ?.let { "${"%.2f".format(Locale.US, it).replace('.', ',')} PLN" }
        ?: purchasePrice.ifBlank { "Do uzupełnienia" }

internal fun PartInventoryItem.unitPurchasePriceInfoLabel(): String? =
    purchasePrice
        .takeIf { quantity > 1 && parsePriceAmount(it) != null }
        ?.let { "$it / szt." }

internal fun List<ShoppingListItem>.afterReceiving(
    receivedItem: ShoppingListItem,
    receivedQuantity: Int,
): List<ShoppingListItem> =
    mapNotNull { item ->
        if (item.stableId() != receivedItem.stableId()) {
            item
        } else {
            val remainingQuantity = item.quantity - receivedQuantity
            if (remainingQuantity > 0) item.copy(quantity = remainingQuantity) else null
        }
    }
