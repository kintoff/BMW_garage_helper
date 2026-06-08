package pl.garage.bmwassistant.database.catalog

import java.io.File

data class VehicleDatabaseDescriptor(
    val vehicleId: String,
    val vehicleDirectory: File,
    val databaseFile: File,
    val filesDirectory: File,
)
