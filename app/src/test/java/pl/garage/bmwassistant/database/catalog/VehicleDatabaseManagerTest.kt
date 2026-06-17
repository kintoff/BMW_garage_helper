package pl.garage.bmwassistant.database.catalog

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VehicleDatabaseManagerTest {

    @Test
    fun createDescriptorBuildsExpectedPaths() {
        val root = Files.createTempDirectory("vehicle-db-manager").toFile()
        val manager = VehicleDatabaseManager(FakeContext(root))

        val descriptor = manager.createDescriptor("vehicle_123")

        assertEquals("vehicle_123", descriptor.vehicleId)
        assertTrue(descriptor.vehicleDirectory.absolutePath.endsWith("files-root/vehicles/vehicle_123"))
        assertTrue(descriptor.databaseFile.absolutePath.endsWith("db-root/vehicle_vehicle_123.db"))
        assertTrue(descriptor.filesDirectory.absolutePath.endsWith("files-root/vehicles/vehicle_123/files"))
        root.deleteRecursively()
    }

    @Test
    fun ensureVehicleStorageCreatesDirectories() {
        val root = Files.createTempDirectory("vehicle-db-manager").toFile()
        val manager = VehicleDatabaseManager(FakeContext(root))

        val descriptor = manager.ensureVehicleStorage("vehicle_abc")

        assertTrue(descriptor.vehicleDirectory.exists())
        assertTrue(descriptor.filesDirectory.exists())
        assertTrue(descriptor.databaseFile.parentFile?.exists() == true)
        root.deleteRecursively()
    }

    private class FakeContext(
        private val root: File
    ) : ContextWrapper(null) {
        private val filesRoot = File(root, "files-root")
        private val dbRoot = File(root, "db-root")

        override fun getApplicationContext(): Context = this

        override fun getFilesDir(): File {
            filesRoot.mkdirs()
            return filesRoot
        }

        override fun getDatabasePath(name: String): File {
            dbRoot.mkdirs()
            return File(dbRoot, name)
        }
    }
}
