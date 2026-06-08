package pl.garage.bmwassistant.model

import java.text.Normalizer

const val REPAIR_STATUS_PLANNED = "Planowane"
const val REPAIR_STATUS_IN_PROGRESS = "W trakcie"
const val REPAIR_STATUS_FINISHED = "Zakonczona"

fun String.normalizedRepairStatusLabel(): String {
    val key = repairStatusKey()
    return when {
        key.isFinishedRepairStatusKey() -> REPAIR_STATUS_FINISHED
        key.contains("trak") || key.contains("progress") || key.contains("active") || key.contains("aktual") ->
            REPAIR_STATUS_IN_PROGRESS
        key.contains("plan") -> REPAIR_STATUS_PLANNED
        else -> trim().ifBlank { REPAIR_STATUS_PLANNED }
    }
}

fun String.isFinishedRepairStatus(): Boolean =
    repairStatusKey().isFinishedRepairStatusKey()

private fun String.repairStatusKey(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .trim()

private fun String.isFinishedRepairStatusKey(): Boolean =
    contains("zakon") ||
        contains("done") ||
        contains("zrob") ||
        contains("finish") ||
        contains("complete") ||
        contains("closed")
