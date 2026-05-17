package pl.garage.bmwassistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pl.garage.bmwassistant.model.Vehicle

class VehicleStorage(context: Context) {
    private val preferences = context.getSharedPreferences("garage_data", Context.MODE_PRIVATE)

    fun loadVehicles(): List<Vehicle> {
        val rawVehicles = preferences.getString("vehicles", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawVehicles)
            List(array.length()) { index ->
                array.getJSONObject(index).toVehicle()
            }
        }.getOrDefault(emptyList())
    }

    fun saveVehicles(vehicles: List<Vehicle>) {
        val array = JSONArray()
        vehicles.forEach { vehicle ->
            array.put(vehicle.toJson())
        }
        preferences.edit()
            .putString("vehicles", array.toString())
            .apply()
    }
}

private fun Vehicle.toJson(): JSONObject = JSONObject()
    .put("id", stableVehicleId())
    .put("brand", brand)
    .put("model", model)
    .put("generation", generation)
    .put("engine", engine)
    .put("year", year)
    .put("vin", vin)
    .put("mileage", mileage)
    .put("note", note)
    .put("partsCatalogUrl", partsCatalogUrl)

private fun JSONObject.toVehicle(): Vehicle {
    val brand = optString("brand")
    val model = optString("model")
    val generation = optString("generation")
    val engine = optString("engine")
    val year = optString("year")
    val vin = optString("vin")
    val mileage = optString("mileage")
    val note = optString("note")
    return Vehicle(
        brand = brand,
        model = model,
        generation = generation,
        engine = engine,
        year = year,
        vin = vin,
        mileage = mileage,
        note = note,
        id = optString("id").ifBlank {
            legacyVehicleId(vin = vin, brand = brand, model = model, generation = generation)
        },
        partsCatalogUrl = optString("partsCatalogUrl")
    )
}

private fun Vehicle.stableVehicleId(): String =
    id.ifBlank {
        legacyVehicleId(
            vin = vin,
            brand = brand,
            model = model,
            generation = generation
        )
    }

private fun legacyVehicleId(
    vin: String,
    brand: String,
    model: String,
    generation: String,
): String {
    val displayName = listOf(brand, model, generation)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    return vin.ifBlank { displayName.ifBlank { "unknown_vehicle" } }
}
