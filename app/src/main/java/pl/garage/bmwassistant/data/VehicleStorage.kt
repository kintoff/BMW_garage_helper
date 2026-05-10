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
    .put("brand", brand)
    .put("model", model)
    .put("generation", generation)
    .put("engine", engine)
    .put("year", year)
    .put("vin", vin)
    .put("mileage", mileage)
    .put("note", note)

private fun JSONObject.toVehicle(): Vehicle = Vehicle(
    brand = optString("brand"),
    model = optString("model"),
    generation = optString("generation"),
    engine = optString("engine"),
    year = optString("year"),
    vin = optString("vin"),
    mileage = optString("mileage"),
    note = optString("note")
)
