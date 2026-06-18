package pl.garage.bmwassistant.database.catalog

import android.content.Context
import androidx.room.Room
import pl.garage.bmwassistant.database.vehicle.VehicleDatabase
import pl.garage.bmwassistant.database.vehicle.VEHICLE_DATABASE_MIGRATION_1_2
import pl.garage.bmwassistant.database.vehicle.VEHICLE_DATABASE_MIGRATION_2_3
import pl.garage.bmwassistant.database.vehicle.VEHICLE_DATABASE_MIGRATION_3_4
import pl.garage.bmwassistant.model.Vehicle
import java.io.File
import java.util.UUID

class VehicleDatabaseManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val openedVehicleDatabases = mutableMapOf<String, VehicleDatabase>()
    private val catalogDatabase: GarageCatalogDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            GarageCatalogDatabase::class.java,
            CATALOG_DATABASE_NAME
        ).build()
    }

    private val catalogDao: VehicleCatalogDao
        get() = catalogDatabase.vehicleCatalogDao()

    suspend fun getActiveVehicles(): List<VehicleCatalogEntity> =
        catalogDao.getActiveVehicles()

    suspend fun getVehicleById(vehicleId: String): VehicleCatalogEntity? =
        catalogDao.getVehicleById(vehicleId)

    suspend fun registerVehicle(vehicle: Vehicle): VehicleCatalogEntity {
        val vehicleId = vehicle.id.ifBlank { createVehicleId() }
        val descriptor = ensureVehicleStorage(vehicleId)
        val now = System.currentTimeMillis()
        val current = catalogDao.getVehicleById(vehicleId)
        val entity = VehicleCatalogEntity(
            vehicleId = vehicleId,
            brand = vehicle.brand,
            model = vehicle.model,
            generation = vehicle.generation,
            engine = vehicle.engine,
            year = vehicle.year,
            vin = vehicle.vin,
            mileage = vehicle.mileage,
            note = vehicle.note,
            partsCatalogUrl = vehicle.partsCatalogUrl,
            databasePath = descriptor.databaseFile.absolutePath,
            filesDirectoryPath = descriptor.filesDirectory.absolutePath,
            createdAtEpochMillis = current?.createdAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
            lastBackupAtEpochMillis = current?.lastBackupAtEpochMillis,
            importSource = current?.importSource,
            isArchived = current?.isArchived ?: false
        )
        catalogDao.insert(entity)
        return entity
    }

    suspend fun deleteVehicle(vehicleId: String) {
        if (vehicleId.isBlank()) return
        val descriptor = vehicleDescriptor(vehicleId)
        openedVehicleDatabases.remove(vehicleId)?.close()
        catalogDao.deleteById(vehicleId)
        descriptor.databaseFile.delete()
        File("${descriptor.databaseFile.absolutePath}-wal").delete()
        File("${descriptor.databaseFile.absolutePath}-shm").delete()
        descriptor.vehicleDirectory.deleteRecursively()
    }

    fun createDescriptor(vehicleId: String): VehicleDatabaseDescriptor =
        vehicleDescriptor(vehicleId)

    fun ensureVehicleStorage(vehicleId: String): VehicleDatabaseDescriptor {
        val descriptor = vehicleDescriptor(vehicleId)
        descriptor.vehicleDirectory.mkdirs()
        descriptor.filesDirectory.mkdirs()
        descriptor.databaseFile.parentFile?.mkdirs()
        return descriptor
    }

    fun openVehicleDatabase(vehicleId: String): VehicleDatabase {
        val normalizedVehicleId = vehicleId.ifBlank { createVehicleId() }
        return openedVehicleDatabases.getOrPut(normalizedVehicleId) {
            val descriptor = ensureVehicleStorage(normalizedVehicleId)
            Room.databaseBuilder(
                appContext,
                VehicleDatabase::class.java,
                descriptor.databaseFile.absolutePath
            )
                .addMigrations(
                    VEHICLE_DATABASE_MIGRATION_1_2,
                    VEHICLE_DATABASE_MIGRATION_2_3,
                    VEHICLE_DATABASE_MIGRATION_3_4
                )
                .build()
        }
    }

    private fun vehicleDescriptor(vehicleId: String): VehicleDatabaseDescriptor {
        val vehicleDirectory = File(appContext.filesDir, "vehicles/$vehicleId")
        return VehicleDatabaseDescriptor(
            vehicleId = vehicleId,
            vehicleDirectory = vehicleDirectory,
            databaseFile = appContext.getDatabasePath("${VEHICLE_DATABASE_PREFIX}_$vehicleId.db"),
            filesDirectory = File(vehicleDirectory, VEHICLE_FILES_DIRECTORY_NAME)
        )
    }

    private fun createVehicleId(): String =
        "vehicle_${UUID.randomUUID()}"

    private companion object {
        const val CATALOG_DATABASE_NAME = "garage_catalog.db"
        const val VEHICLE_DATABASE_PREFIX = "vehicle"
        const val VEHICLE_FILES_DIRECTORY_NAME = "files"
    }
}
