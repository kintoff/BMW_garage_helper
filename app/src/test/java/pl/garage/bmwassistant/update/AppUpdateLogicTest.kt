package pl.garage.bmwassistant.update

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun parseReleaseFallsBackToNameAndDefaultsCurrencyDigestAndPrerelease() {
        val release = parseRelease(
            JSONObject()
                .put("tag_name", "")
                .put("name", "v0.3.0")
                .put(
                    "assets",
                    JSONArray().put(
                        JSONObject()
                            .put("name", "bmw-garage.apk")
                            .put("browser_download_url", "https://example.com/app.apk")
                    )
                )
        )

        assertEquals("0.3.0", release?.versionName)
        assertEquals(null, release?.sha256Digest)
        assertEquals(false, release?.isPrerelease)
    }

    @Test
    fun parseReleaseSupportsOfferArrayAndPrereleaseFlag() {
        val release = parseRelease(
            JSONObject()
                .put("tag_name", "v0.4.0")
                .put("prerelease", true)
                .put(
                    "assets",
                    JSONArray()
                        .put(JSONObject().put("name", "notes.txt").put("browser_download_url", "https://example.com/notes.txt"))
                        .put(JSONObject().put("name", "bmw-garage.apk").put("browser_download_url", "https://example.com/app.apk"))
                )
        )

        assertEquals("0.4.0", release?.versionName)
        assertEquals(true, release?.isPrerelease)
        assertEquals("https://example.com/app.apk", release?.downloadUrl)
    }

    @Test
    fun parseReleaseReturnsNullWhenVersionIsBlank() {
        val release = parseRelease(
            JSONObject()
                .put("tag_name", "")
                .put("name", "")
                .put(
                    "assets",
                    JSONArray().put(
                        JSONObject()
                            .put("name", "bmw-garage.apk")
                            .put("browser_download_url", "https://example.com/app.apk")
                    )
                )
        )

        assertNull(release)
    }

    @Test
    fun versionComparisonHandlesHigherLowerAndEqualVersions() {
        assertTrue(isNewerVersion("0.2.0", "0.1.9"))
        assertFalse(isNewerVersion("0.2.0", "0.2.0"))
        assertFalse(isNewerVersion("0.1.9", "0.2.0"))
        assertTrue(isNewerVersion("v1.0.10", "1.0.2"))
        assertTrue(isNewerVersion("1.0.0.1", "1.0.0"))
        assertFalse(isNewerVersion("1.0", "1.0.0.1"))
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

    @Test
    fun writeVerifiedApkAllowsMissingChecksumAndReportsCalculatedDigest() {
        val tempFile = File.createTempFile("app-update", ".apk")
        tempFile.deleteOnExit()

        val result = writeVerifiedApk(
            input = "apk-binary".toByteArray().inputStream(),
            targetFile = tempFile,
            expectedSha256Digest = null
        )

        assertTrue(result is DownloadUpdateResult.Success)
        assertEquals(
            "apk-binary".toByteArray().toHexStringSha256(),
            (result as DownloadUpdateResult.Success).downloadedDigest
        )
    }

    @Test
    fun toHexStringFormatsBytesAsLowercaseHex() {
        assertEquals("000fff", byteArrayOf(0x00, 0x0f, 0xff.toByte()).toHexString())
    }

    private fun ByteArray.toHexStringSha256(): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(this)
            .toHexString()
}
