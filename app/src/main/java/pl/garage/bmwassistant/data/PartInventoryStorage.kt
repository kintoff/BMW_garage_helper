package pl.garage.bmwassistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea

class PartInventoryStorage(context: Context) {
    private val preferences = context.getSharedPreferences("garage_parts_data", Context.MODE_PRIVATE)

    fun loadParts(vehicle: Vehicle): List<PartInventoryItem> {
        val rawParts = preferences.getString(vehicle.storageKey(), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawParts)
            List(array.length()) { index ->
                array.getJSONObject(index).toPartInventoryItem()
            }
        }.getOrDefault(emptyList())
    }

    fun saveParts(
        vehicle: Vehicle,
        parts: List<PartInventoryItem>,
    ) {
        val array = JSONArray()
        parts.forEach { part ->
            array.put(part.toJson())
        }
        preferences.edit()
            .putString(vehicle.storageKey(), array.toString())
            .apply()
    }

    fun loadShoppingList(vehicle: Vehicle): List<ShoppingListItem> {
        val rawItems = preferences.getString(vehicle.shoppingStorageKey(), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawItems)
            List(array.length()) { index ->
                array.getJSONObject(index).toShoppingListItem()
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
}

private fun Vehicle.storageKey(): String {
    val stableId = id.ifBlank { vin.ifBlank { displayName.ifBlank { "unknown_vehicle" } } }
    return "parts_$stableId"
}

private fun Vehicle.shoppingStorageKey(): String {
    val stableId = id.ifBlank { vin.ifBlank { displayName.ifBlank { "unknown_vehicle" } } }
    return "shopping_$stableId"
}

private fun PartInventoryItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("oemPartNumber", oemPartNumber)
    .put("manufacturerPartNumber", manufacturerPartNumber)
    .put("name", name)
    .put("manufacturer", manufacturer)
    .put("repairTitle", repairTitle)
    .put("quantity", quantity)
    .put("purchasePrice", purchasePrice)
    .put("realOemUrl", realOemUrl)
    .put("photoUri", photoUri)

private fun JSONObject.toPartInventoryItem(): PartInventoryItem = PartInventoryItem(
    id = optString("id"),
    oemPartNumber = optString("oemPartNumber"),
    manufacturerPartNumber = optString("manufacturerPartNumber"),
    name = optString("name"),
    manufacturer = optString("manufacturer"),
    repairTitle = optString("repairTitle").ifBlank { null },
    quantity = optInt("quantity", 1),
    purchasePrice = optString("purchasePrice"),
    realOemUrl = optString("realOemUrl").ifBlank { null },
    photoUri = optString("photoUri").ifBlank { null }
)

private fun ShoppingListItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("partNumber", partNumber)
    .put("manufacturerPartNumber", manufacturerPartNumber)
    .put("name", name)
    .put("manufacturer", manufacturer)
    .put("repairTitle", repairTitle)
    .put("area", area.name)
    .put("quantity", quantity)
    .put("source", source)
    .put("price", price)
    .put("imageUri", imageUri)
    .put("shopUrl", shopUrl)
    .put("realOemUrl", realOemUrl)

private fun JSONObject.toShoppingListItem(): ShoppingListItem = ShoppingListItem(
    id = optString("id"),
    partNumber = optString("partNumber"),
    manufacturerPartNumber = optString("manufacturerPartNumber"),
    name = optString("name"),
    manufacturer = optString("manufacturer"),
    repairTitle = optString("repairTitle"),
    area = optString("area")
        .let { rawArea -> VehicleArea.entries.firstOrNull { it.name == rawArea } }
        ?: VehicleArea.Service,
    quantity = optInt("quantity", 1),
    source = optString("source"),
    price = optString("price"),
    imageUri = optString("imageUri").ifBlank { null },
    shopUrl = optString("shopUrl").ifBlank { null },
    realOemUrl = optString("realOemUrl").ifBlank { null }
)
