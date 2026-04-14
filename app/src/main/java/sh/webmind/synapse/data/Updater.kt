package sh.webmind.synapse.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Latest release info as fetched from GitHub Releases API. */
data class LatestRelease(
    @SerializedName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerializedName("html_url") val htmlUrl: String = "",
    val assets: List<Asset> = emptyList()
) {
    data class Asset(
        val name: String = "",
        @SerializedName("browser_download_url") val downloadUrl: String = "",
        val size: Long = 0
    )
}

object SynapseUpdater {
    private const val RELEASES_URL =
        "https://api.github.com/repos/tejasphatak/synapse-node-app/releases/latest"

    /**
     * Fetch latest release. Returns null if check fails or no release exists.
     * Compares the tag's semver against currentVersionName; if newer, returns release info.
     */
    suspend fun checkForUpdate(currentVersionName: String): LatestRelease? = withContext(Dispatchers.IO) {
        try {
            val conn = (java.net.URL(RELEASES_URL).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "synapse-node-app/$currentVersionName")
            }
            if (conn.responseCode != 200) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val release = Gson().fromJson(body, LatestRelease::class.java) ?: return@withContext null
            // tag_name is "v1.2.3" — strip the v
            val remoteVersion = release.tagName.removePrefix("v")
            if (isNewer(remote = remoteVersion, current = currentVersionName)) release else null
        } catch (_: Exception) {
            null
        }
    }

    /** Semver-aware comparison: 1.0.10 > 1.0.9, 1.1.0 > 1.0.99 */
    fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(r.size, c.size)
        for (i in 0 until len) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }

    /** Find the .apk asset in a release. */
    fun apkAssetOf(release: LatestRelease): LatestRelease.Asset? =
        release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

    /** Download APK to app cache and return the file. */
    suspend fun downloadApk(ctx: Context, url: String): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "synapse-update.apk")
        if (file.exists()) file.delete()
        java.net.URL(url).openStream().use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file
    }

    /** Launch Android's package installer. User taps "Install" once. */
    fun promptInstall(ctx: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        ctx.startActivity(intent)
    }
}
