package pl.garage.bmwassistant.data

import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea

fun sampleActiveRepairAreasFor(vehicle: Vehicle): Set<VehicleArea> {
    val text = listOf(
        vehicle.model,
        vehicle.generation,
        vehicle.engine,
        vehicle.note
    ).joinToString(" ").lowercase()

    return buildSet {
        addAll(sampleRepairsFor(vehicle).map { it.area })

        if (listOf("zwrotnica", "zawieszenie", "wahacz", "amortyzator").any { it in text }) {
            add(VehicleArea.Suspension)
        }
        if (listOf("silnik", "m47", "dde", "egr", "turbo", "dpf").any { it in text }) {
            add(VehicleArea.Engine)
        }
        if (listOf("elektryka", "elektronika", "modul", "czujnik", "blad").any { it in text }) {
            add(VehicleArea.Electronics)
        }
        if (listOf("korozja", "nadwozie", "drzwi", "szyba").any { it in text }) {
            add(VehicleArea.Body)
        }
        if (listOf("olej", "filtr", "serwis", "plyn").any { it in text }) {
            add(VehicleArea.Service)
        }
    }
}
