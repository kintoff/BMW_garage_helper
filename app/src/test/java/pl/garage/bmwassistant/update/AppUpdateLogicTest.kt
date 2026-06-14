package pl.garage.bmwassistant.update

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppUpdateLogicTest {

    @Test
    fun updateConfigurationRequiresOwnerAndRepo() {
        assertTrue(isUpdateConfigurationValid("kintoff", "BMW_garage_helper"))
        assertFalse(isUpdateConfigurationValid("", "BMW_garage_helper"))
        assertFalse(isUpdateConfigurationValid("kintoff", ""))
    }

    @Test
    fun parseReleaseReadsVersionApkAndDigest() {
        val release = parseRelease(
            JSONObject()
                .put("tag_name", "v0.2.0")
                .put("published_at", "2026-06-14T10:00:00Z")
                .put("body", "Nowa wersja")
                .put(
                    "assets",
                    JSONArray().put(
                        JSONObject()
                            .put("name", "bmw-garage.apk")
                            .put("browser_download_url", "https://example.com/app.apk")
                            .put("digest", "sha256:abcd1234")
                    )
                )
        )

        assertEquals("0.2.0", release?.versionName)
        assertEquals("bmw-garage.apk", release?.assetFileName)
        assertEquals("abcd1234", release?.sha256Digest)
    }

    @Test
    fun parseReleaseReturnsNullWhenApkAssetIsMissing() {
        val release = parseRelease(
            JSONObject()
                .put("tag_name", "v0.2.0")
                .put(
                    "assets",
                    JSONArray().put(
                        JSONObject()
                            .put("name", "notes.txt")
                            .put("browser_download_url", "https://example.com/notes.txt")
                    )
                )
        )

        assertEquals(null, release)
    }

    @Test
    fun versionComparisonHandlesHigherLowerAndEqualVersions() {
        assertTrue(isNewerVersion("0.2.0", "0.1.9"))
        assertFalse(isNewerVersion("0.2.0", "0.2.0"))
        assertFalse(isNewerVersion("0.1.9", "0.2.0"))
        assertTrue(isNewerVersion("v1.0.10", "1.0.2"))
    }

    @Test
    fun writeVerifiedApkReturnsSuccessWhenChecksumMatches() {
        val tempFile = File.createTempFile("app-update", ".apk")
        tempFile.deleteOnExit()
        val bytes = "apk-binary".toByteArray()

        val result = writeVerifiedApk(
            input = bytes.inputStream(),
            targetFile = tempFile,
            expectedSha256Digest = bytes.toHexStringSha256()
        )

        assertTrue(result is DownloadUpdateResult.Success)
        assertEquals("apk-binary", tempFile.readText())
    }

    @Test
    fun writeVerifiedApkDeletesFileWhenChecksumDoesNotMatch() {
        val tempFile = File.createTempFile("app-update", ".apk")
        tempFile.deleteOnExit()

        val result = writeVerifiedApk(
            input = "apk-binary".toByteArray().inputStream(),
            targetFile = tempFile,
            expectedSha256Digest = "deadbeef"
        )

        assertTrue(result is DownloadUpdateResult.Error)
        assertFalse(tempFile.exists())
    }

    private fun ByteArray.toHexStringSha256(): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(this)
            .toHexString()
}
