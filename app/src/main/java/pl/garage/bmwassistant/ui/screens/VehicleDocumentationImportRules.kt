package pl.garage.bmwassistant.ui.screens

import org.json.JSONArray
import org.json.JSONObject
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.TorqueDiagramAssignment
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.YoutubeVideo

internal fun RepairDocumentation.effectiveTorqueTables(): List<TorqueSpecTable> =
    torqueTables.ifEmpty {
        if (
            torqueSpecs.isEmpty() &&
            torqueDiagramImageUri == null &&
            torqueDiagramAssignments.isEmpty()
        ) {
            emptyList()
        } else {
            listOf(
                TorqueSpecTable(
                    id = "table-1",
                    title = "Tabela momentow 1",
                    torqueSpecs = torqueSpecs,
                    diagramImageUri = torqueDiagramImageUri,
                    diagramAssignments = torqueDiagramAssignments
                )
            )
        }
    }

internal fun JSONObject.toImportedDocumentation(
    currentDocumentation: RepairDocumentation,
    resolveAsset: (String?) -> String?,
    personalIdFactory: (Int) -> String = { index -> "personal-import-$index" },
): RepairDocumentation {
    val importedTorqueTables = optJSONArray("torqueTables").toImportedTorqueTables(resolveAsset)
    return currentDocumentation.copy(
        title = optString("title").ifBlank { currentDocumentation.title },
        summary = optString("summary").ifBlank { currentDocumentation.summary },
        archivedShoppingList = optJSONArray("archivedShoppingList").toImportedShoppingList(),
        tisLinks = emptyList(),
        tisDocuments = optJSONArray("tisDocuments").toImportedTisDocuments(),
        torqueSpecs = importedTorqueTables.firstOrNull()?.torqueSpecs.orEmpty(),
        torqueDiagramImageUri = importedTorqueTables.firstOrNull()?.diagramImageUri,
        torqueDiagramAssignments = importedTorqueTables.firstOrNull()?.diagramAssignments.orEmpty(),
        torqueTables = importedTorqueTables,
        youtubeLinks = emptyList(),
        youtubeVideos = optJSONArray("youtubeVideos").toImportedYoutubeVideos(),
        personalNotes = optJSONArray("personalNotes").toImportedPersonalNotes(resolveAsset, personalIdFactory)
    )
}

internal fun JSONArray?.toImportedShoppingList(): List<ShoppingListItem> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                ShoppingListItem(
                    id = item.optString("id"),
                    partNumber = item.optString("partNumber"),
                    manufacturerPartNumber = item.optString("manufacturerPartNumber"),
                    name = item.optString("name"),
                    manufacturer = item.optString("manufacturer"),
                    repairTitle = item.optString("repairTitle"),
                    repairId = item.optString("repairId"),
                    area = runCatching { VehicleArea.valueOf(item.optString("area")) }
                        .getOrDefault(VehicleArea.Service),
                    quantity = item.optInt("quantity", 1),
                    source = item.optString("source"),
                    price = item.optString("price"),
                    imageUri = item.optString("imageUri").ifBlank { null },
                    shopUrl = item.optString("shopUrl").ifBlank { null },
                    realOemUrl = item.optString("realOemUrl").ifBlank { null }
                )
            )
        }
    }
}

internal fun JSONArray?.toImportedTisDocuments(): List<TisDocumentationLink> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val url = item.optString("url")
            if (url.isBlank()) continue
            add(
                TisDocumentationLink(
                    title = item.optString("title").ifBlank { "TIS ${index + 1}" },
                    url = url
                )
            )
        }
    }
}

internal fun JSONArray?.toImportedTorqueTables(
    resolveAsset: (String?) -> String?,
): List<TorqueSpecTable> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                TorqueSpecTable(
                    id = item.optString("id").ifBlank { "table-${index + 1}" },
                    title = item.optString("title").ifBlank { "Tabela momentow ${index + 1}" },
                    torqueSpecs = item.optJSONArray("torqueSpecs").toImportedTorqueSpecs(),
                    diagramImageUri = resolveAsset(item.optString("diagramImageUri").ifBlank { null }),
                    diagramAssignments = item.optJSONArray("diagramAssignments").toImportedDiagramAssignments()
                )
            )
        }
    }
}

internal fun JSONArray?.toImportedTorqueSpecs(): List<TorqueSpec> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                TorqueSpec(
                    component = item.optString("component"),
                    type = item.optString("type"),
                    thread = item.optString("thread"),
                    tighteningSpecifications = item.optString("tighteningSpecifications"),
                    torque = item.optString("torque"),
                    source = item.optString("source"),
                    notes = item.optString("notes")
                )
            )
        }
    }
}

internal fun JSONArray?.toImportedDiagramAssignments(): List<TorqueDiagramAssignment> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                TorqueDiagramAssignment(
                    torqueSpecIndex = item.optInt("torqueSpecIndex"),
                    xRatio = item.optDouble("xRatio").toFloat(),
                    yRatio = item.optDouble("yRatio").toFloat()
                )
            )
        }
    }
}

internal fun JSONArray?.toImportedYoutubeVideos(): List<YoutubeVideo> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val url = item.optString("url")
            if (url.isBlank()) continue
            add(
                YoutubeVideo(
                    title = item.optString("title").ifBlank { "Film YouTube ${index + 1}" },
                    url = url,
                    note = item.optString("note")
                )
            )
        }
    }
}

internal fun JSONArray?.toImportedPersonalNotes(
    resolveAsset: (String?) -> String?,
    personalIdFactory: (Int) -> String = { index -> "personal-import-$index" },
): List<PersonalDocumentationItem> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                PersonalDocumentationItem(
                    id = item.optString("id").ifBlank { personalIdFactory(index) },
                    type = runCatching {
                        PersonalDocumentationItemType.valueOf(item.optString("type"))
                    }.getOrDefault(PersonalDocumentationItemType.Text),
                    title = item.optString("title").ifBlank { "Wpis ${index + 1}" },
                    text = item.optString("text"),
                    uri = resolveAsset(item.optString("uri").ifBlank { null }),
                    url = item.optString("url").ifBlank { null }
                )
            )
        }
    }
}
