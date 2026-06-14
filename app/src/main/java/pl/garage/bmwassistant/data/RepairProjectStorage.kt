package pl.garage.bmwassistant.data

import android.content.Context
import android.net.Uri
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairCheckpoint
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.REPAIR_STATUS_FINISHED
import pl.garage.bmwassistant.model.REPAIR_STATUS_IN_PROGRESS
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.TorqueDiagramAssignment
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.YoutubeVideo
import pl.garage.bmwassistant.model.normalizedRepairStatusLabel
import pl.garage.bmwassistant.model.stableRepairId
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class RepairProjectStorage(private val context: Context) {
    private val preferences = context.getSharedPreferences("garage_repair_projects", Context.MODE_PRIVATE)

    fun loadRepairs(vehicle: Vehicle): List<RepairProject> =
        preferences.getString(repairsKey(vehicle), null)
            ?.let(::repairsFromJson)
            .orEmpty()

    fun hasRepairs(vehicle: Vehicle): Boolean =
        preferences.contains(repairsKey(vehicle))

    fun saveRepairs(vehicle: Vehicle, repairs: List<RepairProject>) {
        preferences.edit()
            .putString(repairsKey(vehicle), repairsToJson(repairs).toString())
            .apply()
    }

    fun loadDocumentation(vehicle: Vehicle): List<RepairDocumentation> {
        val repairs = loadRepairs(vehicle)
        return preferences.getString(documentationKey(vehicle), null)
            ?.let { documentationFromJson(it, repairs) }
            .orEmpty()
    }

    fun hasDocumentation(vehicle: Vehicle): Boolean =
        preferences.contains(documentationKey(vehicle))

    fun saveDocumentation(vehicle: Vehicle, documentation: List<RepairDocumentation>) {
        preferences.edit()
            .putString(documentationKey(vehicle), documentationToJson(documentation).toString())
            .apply()
    }

    fun ensureVehicleData(vehicle: Vehicle) {
        preferences.edit().apply {
            if (!preferences.contains(repairsKey(vehicle))) {
                putString(repairsKey(vehicle), JSONArray().toString())
            }
            if (!preferences.contains(documentationKey(vehicle))) {
                putString(documentationKey(vehicle), JSONArray().toString())
            }
        }.apply()
    }

    fun createRepairArchiveExport(
        vehicle: Vehicle,
        repair: RepairProject,
        documentation: RepairDocumentation,
        shoppingItems: List<ShoppingListItem>,
    ): ByteArray {
        var assetCounter = 0
        val assets = JSONArray()
        val assetFiles = mutableListOf<ArchiveAsset>()

        fun embedAsset(rawUri: String?): String? {
            if (rawUri.isNullOrBlank()) return rawUri
            if (rawUri.startsWith("http://") || rawUri.startsWith("https://")) return rawUri
            val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return rawUri
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input -> input.readBytes() }
            }.getOrNull() ?: return rawUri
            val assetId = "asset_${++assetCounter}"
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: "$assetId.bin"
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]+"), "_")
                .ifBlank { "$assetId.bin" }
            val entryPath = "assets/$assetId-$safeName"
            assetFiles += ArchiveAsset(
                id = assetId,
                fileName = fileName,
                path = entryPath,
                bytes = bytes
            )
            assets.put(
                JSONObject()
                    .put("id", assetId)
                    .put("fileName", fileName)
                    .put("path", entryPath)
            )
            return "asset://$assetId"
        }

        val exportedShopping = shoppingItems.ifEmpty { documentation.archivedShoppingList }
            .map { it.withMappedUris(::embedAsset) }
        val exportedDocumentation = documentation
            .copy(archivedShoppingList = exportedShopping)
            .withMappedUris(::embedAsset)

        val manifest = JSONObject()
            .put("format", "BMW_GARAGE_REPAIR_ARCHIVE")
            .put("version", 2)
            .put("vehicle", vehicle.displayName)
            .put("repair", repairsToJson(listOf(repair)).getJSONObject(0))
            .put("documentation", documentationToJson(listOf(exportedDocumentation)).getJSONObject(0))
            .put("shoppingList", shoppingListToJson(exportedShopping))
            .put("assets", assets)

        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                assetFiles.forEach { asset ->
                    zip.putNextEntry(ZipEntry(asset.path))
                    zip.write(asset.bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
    }

    fun peekRepairArchiveTitle(rawArchive: ByteArray): String? =
        runCatching {
            val root = readRepairArchive(rawArchive)?.manifest ?: return@runCatching null
            if (root.optString("format") != "BMW_GARAGE_REPAIR_ARCHIVE") return@runCatching null
            root.optJSONObject("repair")?.optString("title")?.takeIf { it.isNotBlank() }
        }.getOrNull()

    fun importRepairArchive(
        vehicle: Vehicle,
        rawArchive: ByteArray,
        importAsArchived: Boolean,
    ): ImportedRepairArchive? =
        runCatching {
            val archive = readRepairArchive(rawArchive) ?: return@runCatching null
            val root = archive.manifest
            if (root.optString("format") != "BMW_GARAGE_REPAIR_ARCHIVE") return@runCatching null
            val repairObject = root.optJSONObject("repair") ?: return@runCatching null
            val documentationObject = root.optJSONObject("documentation") ?: return@runCatching null
            val assets = root.optJSONArray("assets").toAssetMap()
            val importedRepair = repairsFromJson(JSONArray().put(repairObject).toString()).firstOrNull()
                ?: return@runCatching null
            val newRepairId = "${importedRepair.id}_import_${System.currentTimeMillis()}"
            fun restoreAsset(rawUri: String?): String? {
                if (rawUri.isNullOrBlank() || !rawUri.startsWith("asset://")) return rawUri
                val assetId = rawUri.removePrefix("asset://")
                val asset = assets[assetId] ?: return rawUri
                val bytes = archive.assetBytes[asset.path]
                    ?: archive.assetBytes[assetId]
                    ?: asset.data?.let { Base64.getDecoder().decode(it) }
                    ?: return rawUri
                val directory = File(context.filesDir, "imported_repair_archives/$newRepairId")
                directory.mkdirs()
                val safeName = asset.fileName.replace(Regex("[^A-Za-z0-9._-]+"), "_")
                    .ifBlank { "$assetId.bin" }
                val file = File(directory, "${System.currentTimeMillis()}_$safeName")
                file.writeBytes(bytes)
                return Uri.fromFile(file).toString()
            }

            val repair = importedRepair.copy(
                id = newRepairId,
                vehicleName = vehicle.displayName,
                status = if (importAsArchived) REPAIR_STATUS_FINISHED else REPAIR_STATUS_IN_PROGRESS
            )
            val importedDocumentation = documentationFromJson(
                JSONArray().put(documentationObject).toString(),
                listOf(repair)
            ).firstOrNull() ?: return@runCatching null
            val importedShopping = (root.optJSONArray("shoppingList").toShoppingList() +
                documentationObject.optJSONArray("archivedShoppingList").toShoppingList())
                .distinctBy { it.archiveImportKey() }
                .mapIndexed { index, item ->
                    item.withMappedUris(::restoreAsset).copy(
                        id = "shopping-${newRepairId}-${System.currentTimeMillis()}-$index",
                        repairTitle = repair.title,
                        repairId = repair.id,
                        area = repair.area
                    )
                }
            val restoredDocumentation = importedDocumentation
                .withMappedUris(::restoreAsset)
                .copy(
                    repairId = repair.id,
                    repairTitle = repair.title,
                    area = repair.area,
                    archivedShoppingList = if (importAsArchived) importedShopping else emptyList()
                )

            ImportedRepairArchive(
                repair = repair,
                documentation = restoredDocumentation,
                shoppingList = if (importAsArchived) emptyList() else importedShopping
            )
        }.getOrNull()

    private fun repairsKey(vehicle: Vehicle): String = "repairs_${vehicle.storageKey()}"

    private fun documentationKey(vehicle: Vehicle): String = "documentation_${vehicle.storageKey()}"
}

data class ImportedRepairArchive(
    val repair: RepairProject,
    val documentation: RepairDocumentation,
    val shoppingList: List<ShoppingListItem>,
)

internal data class ExportedAsset(
    val fileName: String,
    val path: String,
    val data: String? = null,
)

internal data class ArchiveAsset(
    val id: String,
    val fileName: String,
    val path: String,
    val bytes: ByteArray,
)

internal data class RepairArchivePayload(
    val manifest: JSONObject,
    val assetBytes: Map<String, ByteArray>,
)

internal fun repairsToJson(repairs: List<RepairProject>): JSONArray =
    JSONArray().apply {
        repairs.forEach { repair ->
            put(
                JSONObject()
                    .put("title", repair.title)
                    .put("id", repair.id)
                    .put("area", repair.area.name)
                    .put("vehicleName", repair.vehicleName)
                    .put("status", repair.status.normalizedRepairStatusLabel())
                    .put("priority", repair.priority)
                    .put("problemDescription", repair.problemDescription)
                    .put("goal", repair.goal)
                    .put("checklist", JSONArray(repair.checklist))
                    .put("checkpoints", checkpointsToJson(repair.checkpoints))
                    .put("partsToIdentify", JSONArray(repair.partsToIdentify))
                    .put("documentsToCollect", JSONArray(repair.documentsToCollect))
            )
        }
    }

internal fun repairsFromJson(rawJson: String): List<RepairProject> =
    runCatching {
        val array = JSONArray(rawJson)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RepairProject(
                        title = item.optString("title"),
                        id = item.optString("id").ifBlank {
                            stableRepairId(
                                title = item.optString("title"),
                                area = runCatching { VehicleArea.valueOf(item.optString("area")) }
                                    .getOrDefault(VehicleArea.Engine),
                                vehicleName = item.optString("vehicleName")
                            )
                        },
                        area = runCatching { VehicleArea.valueOf(item.optString("area")) }
                            .getOrDefault(VehicleArea.Engine),
                        vehicleName = item.optString("vehicleName"),
                        status = item.optString("status").normalizedRepairStatusLabel(),
                        priority = item.optString("priority"),
                        problemDescription = item.optString("problemDescription"),
                        goal = item.optString("goal"),
                        checklist = item.optJSONArray("checklist").toStringList(),
                        checkpoints = item.optJSONArray("checkpoints").toRepairCheckpoints()
                            .ifEmpty {
                                item.optJSONArray("checklist").toStringList().mapIndexed { checkpointIndex, text ->
                                    RepairCheckpoint(
                                        id = "checkpoint-${checkpointIndex + 1}",
                                        text = text,
                                        isDone = false
                                    )
                                }
                            },
                        partsToIdentify = item.optJSONArray("partsToIdentify").toStringList(),
                        documentsToCollect = item.optJSONArray("documentsToCollect").toStringList()
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

internal fun checkpointsToJson(checkpoints: List<RepairCheckpoint>): JSONArray =
    JSONArray().apply {
        checkpoints.forEach { checkpoint ->
            put(
                JSONObject()
                    .put("id", checkpoint.id)
                    .put("text", checkpoint.text)
                    .put("isDone", checkpoint.isDone)
            )
        }
    }

internal fun JSONArray?.toRepairCheckpoints(): List<RepairCheckpoint> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val text = item.optString("text")
            if (text.isBlank()) continue
            add(
                RepairCheckpoint(
                    id = item.optString("id").ifBlank { "checkpoint-${index + 1}" },
                    text = text,
                    isDone = item.optBoolean("isDone", false)
                )
            )
        }
    }
}

internal fun documentationToJson(documentation: List<RepairDocumentation>): JSONArray =
    JSONArray().apply {
        documentation.forEach { item ->
            put(
                JSONObject()
                    .put("title", item.title)
                    .put("repairId", item.repairId)
                    .put("area", item.area.name)
                    .put("repairTitle", item.repairTitle)
                    .put("summary", item.summary)
                    .put("archivedShoppingList", shoppingListToJson(item.archivedShoppingList))
                    .put("tisLinks", JSONArray(item.effectiveTisDocuments().map { it.url }))
                    .put("tisDocuments", tisDocumentsToJson(item.effectiveTisDocuments()))
                    .put("torqueSpecs", torqueSpecsToJson(item.torqueSpecs))
                    .put("torqueDiagramImageUri", item.torqueDiagramImageUri)
                    .put("torqueDiagramAssignments", torqueDiagramAssignmentsToJson(item.torqueDiagramAssignments))
                    .put("torqueTables", torqueTablesToJson(item.effectiveTorqueTables()))
                    .put("youtubeLinks", JSONArray(item.youtubeLinks))
                    .put("youtubeVideos", youtubeVideosToJson(item.effectiveYoutubeVideos()))
                    .put("personalNotes", personalNotesToJson(item.personalNotes))
                    .put("userNotes", item.userNotes)
            )
        }
    }

internal fun documentationFromJson(
    rawJson: String,
    repairs: List<RepairProject>,
): List<RepairDocumentation> =
    runCatching {
        val array = JSONArray(rawJson)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val area = runCatching { VehicleArea.valueOf(item.optString("area")) }
                    .getOrDefault(VehicleArea.Engine)
                val repairTitle = item.optString("repairTitle")
                val migratedRepairId = item.optString("repairId").ifBlank {
                    repairs.firstOrNull { repair ->
                        repair.title == repairTitle && repair.area == area
                    }?.id ?: stableRepairId(
                        title = repairTitle,
                        area = area,
                        vehicleName = repairs.firstOrNull { it.title == repairTitle }?.vehicleName.orEmpty()
                    )
                }
                add(
                    RepairDocumentation(
                        title = item.optString("title"),
                        area = area,
                        repairTitle = repairTitle,
                        summary = item.optString("summary"),
                        archivedShoppingList = item.optJSONArray("archivedShoppingList").toShoppingList(),
                        tisLinks = item.optJSONArray("tisLinks").toStringList(),
                        tisDocuments = item.optJSONArray("tisDocuments").toTisDocuments(),
                        torqueSpecs = item.optJSONArray("torqueSpecs").toTorqueSpecs(),
                        torqueDiagramImageUri = item.optString("torqueDiagramImageUri").ifBlank { null },
                        torqueDiagramAssignments = item.optJSONArray("torqueDiagramAssignments")
                            .toTorqueDiagramAssignments(),
                        torqueTables = item.optJSONArray("torqueTables").toTorqueTables(),
                        youtubeLinks = item.optJSONArray("youtubeLinks").toStringList(),
                        youtubeVideos = item.optJSONArray("youtubeVideos").toYoutubeVideos(),
                        personalNotes = item.optJSONArray("personalNotes").toPersonalNotes(),
                        userNotes = item.optString("userNotes"),
                        repairId = migratedRepairId
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

internal fun shoppingListToJson(items: List<ShoppingListItem>): JSONArray =
    JSONArray().apply {
        items.forEach { item ->
            put(
                JSONObject()
                    .put("id", item.id)
                    .put("partNumber", item.partNumber)
                    .put("manufacturerPartNumber", item.manufacturerPartNumber)
                    .put("name", item.name)
                    .put("manufacturer", item.manufacturer)
                    .put("repairTitle", item.repairTitle)
                    .put("repairId", item.repairId)
                    .put("area", item.area.name)
                    .put("quantity", item.quantity)
                    .put("source", item.source)
                    .put("price", item.price)
                    .put("imageUri", item.imageUri)
                    .put("shopUrl", item.shopUrl)
                    .put("realOemUrl", item.realOemUrl)
            )
        }
    }

internal fun JSONArray?.toShoppingList(): List<ShoppingListItem> {
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

internal fun torqueSpecsToJson(torqueSpecs: List<TorqueSpec>): JSONArray =
    JSONArray().apply {
        torqueSpecs.forEach { spec ->
            put(
                JSONObject()
                    .put("component", spec.component)
                    .put("type", spec.type)
                    .put("thread", spec.thread)
                    .put("tighteningSpecifications", spec.tighteningSpecifications)
                    .put("torque", spec.torque)
                    .put("source", spec.source)
                    .put("notes", spec.notes)
            )
        }
    }

internal fun JSONArray?.toTorqueSpecs(): List<TorqueSpec> {
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

internal fun RepairDocumentation.effectiveTisDocuments(): List<TisDocumentationLink> =
    tisDocuments.ifEmpty {
        tisLinks.mapIndexed { index, link ->
            TisDocumentationLink(
                title = "TIS ${index + 1}",
                url = link
            )
        }
    }

internal fun RepairDocumentation.effectiveYoutubeVideos(): List<YoutubeVideo> =
    youtubeVideos.ifEmpty {
        youtubeLinks.mapIndexed { index, link ->
            YoutubeVideo(
                title = "Film YouTube ${index + 1}",
                url = link
            )
        }
    }

internal fun tisDocumentsToJson(links: List<TisDocumentationLink>): JSONArray =
    JSONArray().apply {
        links.forEach { link ->
            put(
                JSONObject()
                    .put("title", link.title)
                    .put("url", link.url)
            )
        }
    }

internal fun JSONArray?.toTisDocuments(): List<TisDocumentationLink> {
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

internal fun youtubeVideosToJson(videos: List<YoutubeVideo>): JSONArray =
    JSONArray().apply {
        videos.forEach { video ->
            put(
                JSONObject()
                    .put("title", video.title)
                    .put("url", video.url)
                    .put("note", video.note)
            )
        }
    }

internal fun JSONArray?.toYoutubeVideos(): List<YoutubeVideo> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                YoutubeVideo(
                    title = item.optString("title").ifBlank { "Film YouTube ${index + 1}" },
                    url = item.optString("url"),
                    note = item.optString("note")
                )
            )
        }
    }.filter { it.url.isNotBlank() }
}

internal fun personalNotesToJson(items: List<PersonalDocumentationItem>): JSONArray =
    JSONArray().apply {
        items.forEach { item ->
            put(
                JSONObject()
                    .put("id", item.id)
                    .put("type", item.type.name)
                    .put("title", item.title)
                    .put("text", item.text)
                    .put("uri", item.uri)
                    .put("url", item.url)
            )
        }
    }

internal fun JSONArray?.toPersonalNotes(): List<PersonalDocumentationItem> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                PersonalDocumentationItem(
                    id = item.optString("id").ifBlank { "personal-note-${index + 1}" },
                    type = runCatching {
                        PersonalDocumentationItemType.valueOf(item.optString("type"))
                    }.getOrDefault(PersonalDocumentationItemType.Text),
                    title = item.optString("title").ifBlank { "Wpis ${index + 1}" },
                    text = item.optString("text"),
                    uri = item.optString("uri").ifBlank { null },
                    url = item.optString("url").ifBlank { null }
                )
            )
        }
    }
}

internal fun torqueTablesToJson(tables: List<TorqueSpecTable>): JSONArray =
    JSONArray().apply {
        tables.forEach { table ->
            put(
                JSONObject()
                    .put("id", table.id)
                    .put("title", table.title)
                    .put("torqueSpecs", torqueSpecsToJson(table.torqueSpecs))
                    .put("diagramImageUri", table.diagramImageUri)
                    .put("diagramAssignments", torqueDiagramAssignmentsToJson(table.diagramAssignments))
            )
        }
    }

internal fun JSONArray?.toTorqueTables(): List<TorqueSpecTable> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                TorqueSpecTable(
                    id = item.optString("id").ifBlank { "table-${index + 1}" },
                    title = item.optString("title").ifBlank { "Tabela momentow ${index + 1}" },
                    torqueSpecs = item.optJSONArray("torqueSpecs").toTorqueSpecs(),
                    diagramImageUri = item.optString("diagramImageUri").ifBlank { null },
                    diagramAssignments = item.optJSONArray("diagramAssignments").toTorqueDiagramAssignments()
                )
            )
        }
    }
}

internal fun torqueDiagramAssignmentsToJson(assignments: List<TorqueDiagramAssignment>): JSONArray =
    JSONArray().apply {
        assignments.forEach { assignment ->
            put(
                JSONObject()
                    .put("torqueSpecIndex", assignment.torqueSpecIndex)
                    .put("xRatio", assignment.xRatio.toDouble())
                    .put("yRatio", assignment.yRatio.toDouble())
            )
        }
    }

internal fun JSONArray?.toTorqueDiagramAssignments(): List<TorqueDiagramAssignment> {
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

internal fun JSONArray?.toAssetMap(): Map<String, ExportedAsset> {
    if (this == null) return emptyMap()
    return buildMap {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            put(
                id,
                ExportedAsset(
                    fileName = item.optString("fileName").ifBlank { "$id.bin" },
                    path = item.optString("path").ifBlank { id },
                    data = item.optString("data").ifBlank { null }
                )
            )
        }
    }
}

internal fun readRepairArchive(rawArchive: ByteArray): RepairArchivePayload? =
    readZippedRepairArchive(rawArchive) ?: readLegacyJsonRepairArchive(rawArchive)

internal fun readZippedRepairArchive(rawArchive: ByteArray): RepairArchivePayload? =
    runCatching {
        var manifest: JSONObject? = null
        val assetBytes = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(rawArchive)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val bytes = zip.readBytes()
                if (entry.name == "manifest.json") {
                    manifest = JSONObject(bytes.toString(Charsets.UTF_8))
                } else if (!entry.isDirectory) {
                    assetBytes[entry.name] = bytes
                }
                zip.closeEntry()
            }
        }
        manifest?.let { RepairArchivePayload(it, assetBytes) }
    }.getOrNull()

internal fun readLegacyJsonRepairArchive(rawArchive: ByteArray): RepairArchivePayload? =
    runCatching {
        val manifest = JSONObject(rawArchive.toString(Charsets.UTF_8))
        if (manifest.optString("format") != "BMW_GARAGE_REPAIR_ARCHIVE") return@runCatching null
        val assetBytes = buildMap {
            val assets = manifest.optJSONArray("assets")
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val item = assets.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val data = item.optString("data")
                    if (id.isNotBlank() && data.isNotBlank()) {
                        put(id, Base64.getDecoder().decode(data))
                    }
                }
            }
        }
        RepairArchivePayload(manifest, assetBytes)
    }.getOrNull()

internal fun RepairDocumentation.withMappedUris(
    mapper: (String?) -> String?,
): RepairDocumentation =
    copy(
        archivedShoppingList = archivedShoppingList.map { it.withMappedUris(mapper) },
        torqueDiagramImageUri = mapper(torqueDiagramImageUri),
        torqueTables = effectiveTorqueTables().map { table ->
            table.copy(diagramImageUri = mapper(table.diagramImageUri))
        },
        personalNotes = personalNotes.map { note ->
            note.copy(uri = mapper(note.uri))
        }
    )

internal fun ShoppingListItem.withMappedUris(
    mapper: (String?) -> String?,
): ShoppingListItem =
    copy(imageUri = mapper(imageUri))

internal fun ShoppingListItem.archiveImportKey(): String =
    listOf(
        id,
        repairId,
        partNumber,
        manufacturerPartNumber,
        name,
        manufacturer,
        quantity.toString(),
        source,
        price,
        imageUri.orEmpty(),
        shopUrl.orEmpty(),
        realOemUrl.orEmpty()
    ).joinToString("|")

internal fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(optString(index))
        }
    }
}

internal fun Vehicle.storageKey(): String {
    val stableId = id.ifBlank { vin.ifBlank { displayName.ifBlank { "unknown_vehicle" } } }
    return stableId
}
