package pl.garage.bmwassistant.database.repository

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.garage.bmwassistant.model.Vehicle
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import sun.misc.Unsafe

class GarageRepositoryInternalTest {

    @Test
    fun privateVehicleJsonRoundTripPreservesFields() {
        val repository = allocateRepositoryWithoutConstructor()
        val vehicle = Vehicle(
            brand = "BMW",
            model = "E61 520d",
            generation = "E61",
            engine = "M47N2 2.0d",
            year = "2006",
            vin = "WBATEST001",
            mileage = "285000",
            note = "warsztat",
            id = "vehicle_1",
            partsCatalogUrl = "https://czescidobmw.pl/test"
        )

        val json = repository.invokePrivate<JSONObject>("toJson", vehicle)
        val restored = repository.invokePrivate<Vehicle>("toVehicle", json)

        assertEquals("BMW", json.getString("brand"))
        assertEquals("", json.optString("id"))
        assertEquals(vehicle.copy(id = ""), restored)
    }

    @Test
    fun unzipVehicleBackupExtractsFilesInsideTargetDirectory() {
        val repository = allocateRepositoryWithoutConstructor()
        val targetDirectory = Files.createTempDirectory("garage-repo-test").toFile()
        val archiveBytes = zipBytes(
            "manifest.json" to """{"format":"BMW_GARAGE_VEHICLE_BACKUP"}""".toByteArray(),
            "files/note.txt" to "sample".toByteArray()
        )

        repository.invokePrivate<Unit>(
            "unzipVehicleBackup",
            ByteArrayInputStream(archiveBytes),
            targetDirectory
        )

        assertTrue(File(targetDirectory, "manifest.json").exists())
        assertEquals("sample", File(targetDirectory, "files/note.txt").readText())
        targetDirectory.deleteRecursively()
    }

    @Test
    fun unzipVehicleBackupRejectsPathTraversalEntries() {
        val repository = allocateRepositoryWithoutConstructor()
        val targetDirectory = Files.createTempDirectory("garage-repo-test").toFile()
        val archiveBytes = zipBytes("../escape.txt" to "bad".toByteArray())

        val error = runCatching {
            repository.invokePrivate<Unit>(
                "unzipVehicleBackup",
                ByteArrayInputStream(archiveBytes),
                targetDirectory
            )
        }.exceptionOrNull()
        val cause = error?.cause ?: error

        assertTrue(cause is IllegalStateException)
        assertTrue(cause?.message?.contains("Invalid backup entry path.") == true)
        assertNull(File(targetDirectory.parentFile, "escape.txt").takeIf(File::exists))
        targetDirectory.deleteRecursively()
    }

    private fun zipBytes(vararg entries: Pair<String, ByteArray>): ByteArray =
        java.io.ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }

    private fun allocateRepositoryWithoutConstructor(): GarageRepository =
        unsafe.allocateInstance(GarageRepository::class.java) as GarageRepository

    private companion object {
        val unsafe: Unsafe by lazy {
            val field = Unsafe::class.java.getDeclaredField("theUnsafe")
            field.isAccessible = true
            field.get(null) as Unsafe
        }
    }
}

private fun <T> GarageRepository.invokePrivate(
    name: String,
    vararg args: Any,
): T {
    val parameterTypes = args.map { arg ->
        when (arg) {
            is ByteArrayInputStream -> java.io.InputStream::class.java
            is JSONObject -> JSONObject::class.java
            else -> arg.javaClass
        }
    }.toTypedArray()
    val method = GarageRepository::class.java.getDeclaredMethod(name, *parameterTypes)
    method.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return method.invoke(this, *args) as T
}
