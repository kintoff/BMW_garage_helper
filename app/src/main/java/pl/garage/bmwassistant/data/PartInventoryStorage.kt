package pl.garage.bmwassistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.stableRepairId

class PartInventoryStorage(context: Context) {
    private val preferences = context.getSharedPreferences("garage_parts_data", Context.MODE_PRIVATE)

    fun loadParts(vehicle: Vehicle): List<PartInventoryItem> {
        val rawParts = preferences.getString(vehicle.partsStorageKey(), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawParts)
            List(array.length()) { index ->
                array.getJSONObject(index).toPartInventoryItem(vehicle)
            }
        }.getOrDefault(emptyList())
    }

    fun hasParts(vehicle: Vehicle): Boolean =
        preferences.contains(vehicle.partsStorageKey())

    fun saveParts(
        vehicle: Vehicle,
        parts: List<PartInventoryItem>,
    ) {
        val array = JSONArray()
        parts.forEach { part ->
            array.put(part.toJson())
        }
        preferences.edit()
            .putString(vehicle.partsStorageKey(), array.toString())
            .apply()
    }

    fun loadShoppingList(vehicle: Vehicle): List<ShoppingListItem> {
        val rawItems = preferences.getString(vehicle.shoppingStorageKey(), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawItems)
            List(array.length()) { index ->
                array.getJSONObject(index).toShoppingListItem(vehicle)
            }
        }.getOrDefault(emptyList())
    }

    fun hasShoppingList(vehicle: Vehicle): Boolean =
        preferences.contains(vehicle.shoppingStorageKey())

    fun saveShoppingList(
        vehicle: Vehicle,
        items: List<ShoppingListItem>,
    ) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(item.toJson())
        }
        preferences.edit()
            .putString(vehicle.shoppingStorageKey(), array.toString())
            .apply()
    }

    fun ensureVehicleData(vehicle: Vehicle) {
        preferences.edit().apply {
            if (!preferences.contains(vehicle.partsStorageKey())) {
                putString(vehicle.partsStorageKey(), JSONArray().toString())
            }
            if (!preferences.contains(vehicle.shoppingStorageKey())) {
                putString(vehicle.shoppingStorageKey(), JSONArray().toString())
            }
        }.apply()
    }
}

internal fun Vehicle.partsStorageKey(): String {
    val stableId = id.ifBlank { vin.ifBlank { displayName.ifBlank { "unknown_vehicle" } } }
    return "parts_$stableId"
}

internal fun Vehicle.shoppingStorageKey(): String {
    val stableId = id.ifBlank { vin.ifBlank { displayName.ifBlank { "unknown_vehicle" } } }
    return "shopping_$stableId"
}

internal fun PartInventoryItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("oemPartNumber", oemPartNumber)
    .put("manufacturerPartNumber", manufacturerPartNumber)
    .put("name", name)
    .put("manufacturer", manufacturer)
    .put("repairTitle", repairTitle)
    .put("repairId", repairId)
    .put("quantity", quantity)
    .put("purchasePrice", purchasePrice)
    .put("realOemUrl", realOemUrl)
    .put("photoUri", photoUri)

internal fun JSONObject.toPartInventoryItem(vehicle: Vehicle): PartInventoryItem = PartInventoryItem(
    id = optString("id"),
    oemPartNumber = optString("oemPartNumber"),
    manufacturerPartNumber = optString("manufacturerPartNumber"),
    name = optString("name"),
    manufacturer = optString("manufacturer"),
    repairTitle = optString("repairTitle").ifBlank { null },
    quantity = optInt("quantity", 1),
    purchasePrice = optString("purchasePrice"),
    realOemUrl = optString("realOemUrl").ifBlank { null },
    photoUri = optString("photoUri").ifBlank { null },
    repairId = optString("repairId").ifBlank {
        optString("repairTitle").takeIf { it.isNotBlank() }?.let { repairTitle ->
            stableRepairId(
                title = repairTitle,
                area = VehicleArea.Service,
                vehicleName = vehicle.displayName
            )
        }
    }
)

internal fun ShoppingListItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("partNumber", partNumber)
    .put("manufacturerPartNumber", manufacturerPartNumber)
    .put("name", name)
    .put("manufacturer", manufacturer)
    .put("repairTitle", repairTitle)
    .put("repairId", repairId)
    .put("area", area.name)
    .put("quantity", quantity)
    .put("source", source)
    .put("price", price)
    .put("imageUri", imageUri)
    .put("shopUrl", shopUrl)
    .put("realOemUrl", realOemUrl)

internal fun JSONObject.toShoppingListItem(vehicle: Vehicle): ShoppingListItem {
    val area = optString("area")
        .let { rawArea -> VehicleArea.entries.firstOrNull { it.name == rawArea } }
        ?: VehicleArea.Service
    val repairTitle = optString("repairTitle")
    return ShoppingListItem(
    id = optString("id"),
    partNumber = optString("partNumber"),
    manufacturerPartNumber = optString("manufacturerPartNumber"),
    name = optString("name"),
    manufacturer = optString("manufacturer"),
    repairTitle = repairTitle,
    repairId = optString("repairId").ifBlank {
        stableRepairId(
            title = repairTitle,
            area = area,
            vehicleName = vehicle.displayName
        )
    },
    area = area,
    quantity = optInt("quantity", 1),
    source = optString("source"),
    price = optString("price"),
    imageUri = optString("imageUri").ifBlank { null },
    shopUrl = optString("shopUrl").ifBlank { null },
    realOemUrl = optString("realOemUrl").ifBlank { null }
)
}
