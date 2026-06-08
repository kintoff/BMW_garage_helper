package pl.garage.bmwassistant.database.catalog

import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.garage.bmwassistant.model.Vehicle

@Entity(tableName = "vehicles")
data class VehicleCatalogEntity(
    @PrimaryKey
    val vehicleId: String,
    val brand: String,
    val model: String,
    val generation: String,
    val engine: String,
    val year: String,
    val vin: String,
    val mileage: String,
    val note: String,
    val partsCatalogUrl: String,
    val databasePath: String,
    val filesDirectoryPath: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val lastBackupAtEpochMillis: Long? = null,
    val importSource: String? = null,
    val isArchived: Boolean = false,
)

fun VehicleCatalogEntity.toVehicle(): Vehicle = Vehicle(
    brand = brand,
    model = model,
    generation = generation,
    engine = engine,
    year = year,
    vin = vin,
    mileage = mileage,
    note = note,
    id = vehicleId,
    partsCatalogUrl = partsCatalogUrl
)
