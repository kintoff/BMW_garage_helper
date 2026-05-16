package pl.garage.bmwassistant.data

import android.content.Context
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.TorqueDiagramAssignment
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.YoutubeVideo
import org.json.JSONArray
import org.json.JSONObject

class RepairProjectStorage(context: Context) {
    private val preferences = context.getSharedPreferences("garage_repair_projects", Context.MODE_PRIVATE)

    fun loadRepairs(vehicle: Vehicle): List<RepairProject> =
        preferences.getString(repairsKey(vehicle), null)
            ?.let(::repairsFromJson)
            .orEmpty()

    fun saveRepairs(vehicle: Vehicle, repairs: List<RepairProject>) {
        preferences.edit()
            .putString(repairsKey(vehicle), repairsToJson(repairs).toString())
            .apply()
    }

    fun loadDocumentation(vehicle: Vehicle): List<RepairDocumentation> =
        preferences.getString(documentationKey(vehicle), null)
            ?.let(::documentationFromJson)
            .orEmpty()

    fun saveDocumentation(vehicle: Vehicle, documentation: List<RepairDocumentation>) {
        preferences.edit()
            .putString(documentationKey(vehicle), documentationToJson(documentation).toString())
            .apply()
    }

    private fun repairsKey(vehicle: Vehicle): String = "repairs_${vehicle.storageKey()}"

    private fun documentationKey(vehicle: Vehicle): String = "documentation_${vehicle.storageKey()}"
}

private fun repairsToJson(repairs: List<RepairProject>): JSONArray =
    JSONArray().apply {
        repairs.forEach { repair ->
            put(
                JSONObject()
                    .put("title", repair.title)
                    .put("area", repair.area.name)
                    .put("vehicleName", repair.vehicleName)
                    .put("status", repair.status)
                    .put("priority", repair.priority)
                    .put("problemDescription", repair.problemDescription)
                    .put("goal", repair.goal)
                    .put("checklist", JSONArray(repair.checklist))
                    .put("partsToIdentify", JSONArray(repair.partsToIdentify))
                    .put("documentsToCollect", JSONArray(repair.documentsToCollect))
            )
        }
    }

private fun repairsFromJson(rawJson: String): List<RepairProject> =
    runCatching {
        val array = JSONArray(rawJson)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RepairProject(
                        title = item.optString("title"),
                        area = runCatching { VehicleArea.valueOf(item.optString("area")) }
                            .getOrDefault(VehicleArea.Engine),
                        vehicleName = item.optString("vehicleName"),
                        status = item.optString("status"),
                        priority = item.optString("priority"),
                        problemDescription = item.optString("problemDescription"),
                        goal = item.optString("goal"),
                        checklist = item.optJSONArray("checklist").toStringList(),
                        partsToIdentify = item.optJSONArray("partsToIdentify").toStringList(),
                        documentsToCollect = item.optJSONArray("documentsToCollect").toStringList()
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

private fun documentationToJson(documentation: List<RepairDocumentation>): JSONArray =
    JSONArray().apply {
        documentation.forEach { item ->
            put(
                JSONObject()
                    .put("title", item.title)
                    .put("area", item.area.name)
                    .put("repairTitle", item.repairTitle)
                    .put("summary", item.summary)
                    .put("tisLinks", JSONArray(item.effectiveTisDocuments().map { it.url }))
                    .put("tisDocuments", tisDocumentsToJson(item.effectiveTisDocuments()))
                    .put("torqueSpecs", torqueSpecsToJson(item.torqueSpecs))
                    .put("torqueDiagramImageUri", item.torqueDiagramImageUri)
                    .put("torqueDiagramAssignments", torqueDiagramAssignmentsToJson(item.torqueDiagramAssignments))
                    .put("torqueTables", torqueTablesToJson(item.effectiveTorqueTables()))
                    .put("youtubeLinks", JSONArray(item.youtubeLinks))
                    .put("youtubeVideos", youtubeVideosToJson(item.effectiveYoutubeVideos()))
                    .put("personalNotes", personalNotesToJson(item.personalNotes))
            )
        }
    }

private fun documentationFromJson(rawJson: String): List<RepairDocumentation> =
    runCatching {
        val array = JSONArray(rawJson)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RepairDocumentation(
                        title = item.optString("title"),
                        area = runCatching { VehicleArea.valueOf(item.optString("area")) }
                            .getOrDefault(VehicleArea.Engine),
                        repairTitle = item.optString("repairTitle"),
                        summary = item.optString("summary"),
                        tisLinks = item.optJSONArray("tisLinks").toStringList(),
                        tisDocuments = item.optJSONArray("tisDocuments").toTisDocuments(),
                        torqueSpecs = item.optJSONArray("torqueSpecs").toTorqueSpecs(),
                        torqueDiagramImageUri = item.optString("torqueDiagramImageUri").ifBlank { null },
                        torqueDiagramAssignments = item.optJSONArray("torqueDiagramAssignments")
                            .toTorqueDiagramAssignments(),
                        torqueTables = item.optJSONArray("torqueTables").toTorqueTables(),
                        youtubeLinks = item.optJSONArray("youtubeLinks").toStringList(),
                        youtubeVideos = item.optJSONArray("youtubeVideos").toYoutubeVideos(),
                        personalNotes = item.optJSONArray("personalNotes").toPersonalNotes()
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

private fun torqueSpecsToJson(torqueSpecs: List<TorqueSpec>): JSONArray =
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

private fun JSONArray?.toTorqueSpecs(): List<TorqueSpec> {
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

private fun RepairDocumentation.effectiveTorqueTables(): List<TorqueSpecTable> =
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

private fun RepairDocumentation.effectiveTisDocuments(): List<TisDocumentationLink> =
    tisDocuments.ifEmpty {
        tisLinks.mapIndexed { index, link ->
            TisDocumentationLink(
                title = "TIS ${index + 1}",
                url = link
            )
        }
    }

private fun RepairDocumentation.effectiveYoutubeVideos(): List<YoutubeVideo> =
    youtubeVideos.ifEmpty {
        youtubeLinks.mapIndexed { index, link ->
            YoutubeVideo(
                title = "Film YouTube ${index + 1}",
                url = link
            )
        }
    }

private fun tisDocumentsToJson(links: List<TisDocumentationLink>): JSONArray =
    JSONArray().apply {
        links.forEach { link ->
            put(
                JSONObject()
                    .put("title", link.title)
                    .put("url", link.url)
            )
        }
    }

private fun JSONArray?.toTisDocuments(): List<TisDocumentationLink> {
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

private fun youtubeVideosToJson(videos: List<YoutubeVideo>): JSONArray =
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

private fun JSONArray?.toYoutubeVideos(): List<YoutubeVideo> {
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

private fun personalNotesToJson(items: List<PersonalDocumentationItem>): JSONArray =
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

private fun JSONArray?.toPersonalNotes(): List<PersonalDocumentationItem> {
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

private fun torqueTablesToJson(tables: List<TorqueSpecTable>): JSONArray =
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

private fun JSONArray?.toTorqueTables(): List<TorqueSpecTable> {
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

private fun torqueDiagramAssignmentsToJson(assignments: List<TorqueDiagramAssignment>): JSONArray =
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

private fun JSONArray?.toTorqueDiagramAssignments(): List<TorqueDiagramAssignment> {
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

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(optString(index))
        }
    }
}

private fun Vehicle.storageKey(): String {
    val stableId = vin.ifBlank { displayName.ifBlank { "unknown_vehicle" } }
    return stableId
}
