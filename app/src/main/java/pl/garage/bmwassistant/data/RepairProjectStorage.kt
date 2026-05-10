package pl.garage.bmwassistant.data

import android.content.Context
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
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
                    .put("tisLinks", JSONArray(item.tisLinks))
                    .put("torqueSpecs", torqueSpecsToJson(item.torqueSpecs))
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
                        torqueSpecs = item.optJSONArray("torqueSpecs").toTorqueSpecs()
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
