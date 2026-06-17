package pl.garage.bmwassistant.ui.screens

import pl.garage.bmwassistant.data.ImportedRepairArchive
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem

internal fun RepairDocumentation.belongsToRepair(repair: RepairProject): Boolean =
    repairId == repair.id || (repairId.isBlank() && repairTitle == repair.title && area == repair.area)

internal fun RepairDocumentation.belongsToRepair(updatedDocumentation: RepairDocumentation): Boolean =
    repairId == updatedDocumentation.repairId ||
        (
            repairId.isBlank() &&
                repairTitle == updatedDocumentation.repairTitle &&
                area == updatedDocumentation.area
            )

internal fun ShoppingListItem.belongsToRepair(repair: RepairProject): Boolean =
    repairId == repair.id || (repairId.isBlank() && repairTitle == repair.title && area == repair.area)

internal fun PartInventoryItem.belongsToRepair(repair: RepairProject): Boolean =
    repairId == repair.id || (repairId.isNullOrBlank() && repairTitle == repair.title)

internal fun ImportedRepairArchive.withRepairTitle(newTitle: String): ImportedRepairArchive =
    copy(
        repair = repair.copy(title = newTitle),
        documentation = documentation.copy(
            title = "Dokumentacja: $newTitle",
            repairTitle = newTitle,
            summary = documentation.summary.replace(repair.title, newTitle),
            archivedShoppingList = documentation.archivedShoppingList.map { item ->
                item.copy(repairTitle = newTitle)
            }
        ),
        shoppingList = shoppingList.map { item ->
            item.copy(repairTitle = newTitle)
        }
    )

internal fun String.hasSameRepairTitleAs(other: String): Boolean =
    normalizedRepairTitleKey() == other.normalizedRepairTitleKey() &&
        normalizedRepairTitleKey().isNotBlank()

internal fun List<RepairProject>.nextAvailableRepairTitle(baseTitle: String): String {
    val cleanBaseTitle = baseTitle.trim().ifBlank { "Importowana naprawa" }
    var index = 2
    var candidate = "$cleanBaseTitle ($index)"
    while (any { it.title.hasSameRepairTitleAs(candidate) }) {
        index += 1
        candidate = "$cleanBaseTitle ($index)"
    }
    return candidate
}

internal fun String.normalizedRepairTitleKey(): String =
    lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun List<RepairDocumentation>.withArchivedShoppingList(
    repair: RepairProject,
    archivedShoppingList: List<ShoppingListItem>,
): List<RepairDocumentation> {
    var wasUpdated = false
    val updatedDocumentation = map { documentation ->
        if (documentation.belongsToRepair(repair)) {
            wasUpdated = true
            documentation.copy(
                archivedShoppingList = (documentation.archivedShoppingList + archivedShoppingList)
                    .mergeArchivedShoppingItems(repair)
            )
        } else {
            documentation
        }
    }
    return if (wasUpdated) {
        updatedDocumentation
    } else {
        updatedDocumentation + RepairDocumentation(
            title = "Dokumentacja: ${repair.title}",
            area = repair.area,
            repairTitle = repair.title,
            repairId = repair.id,
            summary = "Dokumentacja powiazana z naprawa: ${repair.title}.",
            archivedShoppingList = archivedShoppingList
        )
    }
}

internal fun PartInventoryItem.toArchivedShoppingListItem(repair: RepairProject): ShoppingListItem =
    ShoppingListItem(
        id = id.ifBlank { "archived_${repair.id}_${partNumber}_${name}" },
        partNumber = oemPartNumber,
        manufacturerPartNumber = manufacturerPartNumber,
        name = name,
        manufacturer = manufacturer,
        repairTitle = repair.title,
        repairId = repair.id,
        area = repair.area,
        quantity = quantity,
        source = "Magazyn",
        price = purchasePrice,
        imageUri = photoUri,
        realOemUrl = realOemUrl
    )

internal fun List<ShoppingListItem>.mergeArchivedShoppingItems(repair: RepairProject): List<ShoppingListItem> =
    mapIndexed { index, item -> item.archiveMergeKey(index) to item }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .values
        .map { items ->
            val primary = items.bestArchivedShoppingItem()
            primary.copy(
                id = primary.id.ifBlank {
                    "archived_${repair.id}_${primary.partNumber}_${primary.manufacturerPartNumber}_${primary.name}"
                },
                repairTitle = repair.title,
                repairId = repair.id,
                area = repair.area,
                quantity = items.sumOf { it.quantity },
                partNumber = primary.partNumber.ifBlank {
                    items.firstNotNullOfOrNull { it.partNumber.takeIf(String::isNotBlank) }.orEmpty()
                },
                manufacturerPartNumber = primary.manufacturerPartNumber.ifBlank {
                    items.firstNotNullOfOrNull { it.manufacturerPartNumber.takeIf(String::isNotBlank) }.orEmpty()
                },
                price = primary.price.ifBlank {
                    items.firstNotNullOfOrNull { it.price.takeIf(String::isNotBlank) }.orEmpty()
                },
                imageUri = primary.imageUri ?: items.firstNotNullOfOrNull { it.imageUri },
                shopUrl = primary.shopUrl ?: items.firstNotNullOfOrNull { it.shopUrl },
                realOemUrl = primary.realOemUrl ?: items.firstNotNullOfOrNull { it.realOemUrl }
            )
        }

internal fun ShoppingListItem.archiveMergeKey(index: Int): String {
    val explicitPartKey = listOf(
        manufacturerPartNumber.normalizedArchivePartKey(),
        partNumber.normalizedArchivePartKey()
    ).firstOrNull { it.isUsableArchivePartKey() }
    if (explicitPartKey != null) return "part_$explicitPartKey"
    if (id.isNotBlank()) return "id_$id"
    return "line_$index"
}

internal fun List<ShoppingListItem>.bestArchivedShoppingItem(): ShoppingListItem =
    maxBy { item ->
        listOf(
            item.source != "Magazyn",
            item.shopUrl != null,
            item.imageUri != null,
            item.price.isNotBlank(),
            item.manufacturerPartNumber.isNotBlank(),
            item.partNumber.isNotBlank()
        ).count { it }
    }

internal fun String.normalizedArchivePartKey(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

internal fun String.isUsableArchivePartKey(): Boolean =
    isNotBlank() &&
        this != "do_uzupelnienia" &&
        this != "do_ustalenia" &&
        this != "brak" &&
        this != "unknown"
