package pl.garage.bmwassistant.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import pl.garage.bmwassistant.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.max

class AppUpdateManager(
    private val context: Context,
) {
    private val repoOwner = BuildConfig.UPDATE_REPO_OWNER.trim()
    private val repoName = BuildConfig.UPDATE_REPO_NAME.trim()

    fun isConfigured(): Boolean =
        repoOwner.isNotBlank() && repoName.isNotBlank()

    fun checkForUpdate(): AppUpdateCheckResult {
        if (!isConfigured()) return AppUpdateCheckResult.NotConfigured

        return runCatching {
            val connection = (URL(latestReleaseUrl()).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "${context.packageName}/android-app-updater")
            }

            connection.useAndDisconnect { http ->
                if (http.responseCode !in 200..299) {
                    return AppUpdateCheckResult.Error(
                        "Nie udalo sie sprawdzic release. GitHub zwrocil kod ${http.responseCode}."
                    )
                }

                val response = http.inputStream.bufferedReader().use { it.readText() }
                val release = parseRelease(JSONObject(response))
                    ?: return AppUpdateCheckResult.Error("Nie znaleziono pliku APK w najnowszym release.")

                if (release.isNewerThanCurrent()) {
                    AppUpdateCheckResult.UpdateAvailable(release)
                } else {
                    AppUpdateCheckResult.UpToDate(currentVersionName = BuildConfig.VERSION_NAME)
                }
            }
        }.getOrElse {
            AppUpdateCheckResult.Error(
                it.message?.takeIf(String::isNotBlank)
                    ?: "Nie udalo sie polaczyc z GitHub Releases."
            )
        }
    }

    fun downloadUpdate(release: AppUpdateRelease): DownloadUpdateResult =
        runCatching {
            val updatesDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDirectory.listFiles()?.forEach(File::delete)
            val targetFile = File(updatesDirectory, release.assetFileName)

            val connection = (URL(release.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "${context.packageName}/android-app-updater")
            }

            connection.useAndDisconnect { http ->
                if (http.responseCode !in 200..299) {
                    return DownloadUpdateResult.Error(
                        "Nie udalo sie pobrac APK. GitHub zwrocil kod ${http.responseCode}."
                    )
                }

                val digest = MessageDigest.getInstance("SHA-256")
                http.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                    }
                }

                val downloadedDigest = digest.digest().toHexString()
                if (release.sha256Digest != null && !release.sha256Digest.equals(downloadedDigest, ignoreCase = true)) {
                    targetFile.delete()
                    return DownloadUpdateResult.Error(
                        "Suma kontrolna APK nie zgadza sie z release na GitHub."
                    )
                }

                DownloadUpdateResult.Success(
                    file = targetFile,
                    downloadedDigest = downloadedDigest
                )
            }
        }.getOrElse {
            DownloadUpdateResult.Error(
                it.message?.takeIf(String::isNotBlank)
                    ?: "Nie udalo sie zapisac pliku APK."
            )
        }

    fun canRequestPackageInstalls(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun openUnknownSourcesSettings(): Boolean =
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                true
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }
        }.getOrDefault(false)

    fun launchInstaller(apkFile: File): Boolean =
        runCatching {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_RETURN_RESULT, false)
            }
            context.startActivity(installIntent)
            true
        }.recoverCatching {
            val fallbackUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fallbackUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            true
        }.getOrDefault(false)

    private fun parseRelease(root: JSONObject): AppUpdateRelease? {
        val tagVersionName = root.optString("tag_name").removePrefix("v").trim()
        val releaseVersionName = tagVersionName.ifBlank {
            root.optString("name").removePrefix("v").trim()
        }
        if (releaseVersionName.isBlank()) return null

        val asset = root.optJSONArray("assets")
            ?.let { assets ->
                (0 until assets.length())
                    .mapNotNull(assets::optJSONObject)
                    .firstOrNull { item ->
                        item.optString("name").endsWith(".apk", ignoreCase = true)
                    }
            }
            ?: return null

        val assetName = asset.optString("name")
        val downloadUrl = asset.optString("browser_download_url")
        if (assetName.isBlank() || downloadUrl.isBlank()) return null

        return AppUpdateRelease(
            versionName = releaseVersionName,
            downloadUrl = downloadUrl,
            assetFileName = assetName,
            publishedAt = root.optString("published_at"),
            notes = root.optString("body"),
            sha256Digest = asset.optString("digest")
                .removePrefix("sha256:")
                .trim()
                .takeIf(String::isNotBlank)
        )
    }

    private fun AppUpdateRelease.isNewerThanCurrent(): Boolean {
        val latestParts = versionParts(versionName)
        val currentParts = versionParts(BuildConfig.VERSION_NAME)
        val size = max(latestParts.size, currentParts.size)
        for (index in 0 until size) {
            val latest = latestParts.getOrElse(index) { 0 }
            val current = currentParts.getOrElse(index) { 0 }
            if (latest > current) return true
            if (latest < current) return false
        }
        return false
    }

    private fun latestReleaseUrl(): String =
        "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
}

data class AppUpdateRelease(
    val versionName: String,
    val downloadUrl: String,
    val assetFileName: String,
    val publishedAt: String,
    val notes: String,
    val sha256Digest: String?,
)

sealed interface AppUpdateCheckResult {
    data object NotConfigured : AppUpdateCheckResult
    data class UpToDate(val currentVersionName: String) : AppUpdateCheckResult
    data class UpdateAvailable(val release: AppUpdateRelease) : AppUpdateCheckResult
    data class Error(val message: String) : AppUpdateCheckResult
}

sealed interface DownloadUpdateResult {
    data class Success(
        val file: File,
        val downloadedDigest: String,
    ) : DownloadUpdateResult

    data class Error(val message: String) : DownloadUpdateResult
}

private fun versionParts(versionName: String): List<Int> =
    versionName.removePrefix("v")
        .split('.', '-', '_')
        .mapNotNull { part -> part.toIntOrNull() }

private fun ByteArray.toHexString(): String =
    joinToString(separator = "") { byte -> "%02x".format(byte) }

private inline fun <T> HttpURLConnection.useAndDisconnect(block: (HttpURLConnection) -> T): T =
    try {
        block(this)
    } finally {
        disconnect()
    }
