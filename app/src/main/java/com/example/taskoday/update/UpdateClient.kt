package com.example.taskoday.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.taskoday.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UpdateClient(private val context: Context) {
    fun check(): UpdateManifest? {
        if (!BuildConfig.BUILD_TYPE.equals("release", true) && !BuildConfig.BUILD_TYPE.equals("qa", true)) return null
        val trusted = BuildConfig.UPDATE_SIGNER_SHA256.lowercase()
        if (!Regex("^[0-9a-f]{64}$").matches(trusted)) return null
        val installed = if (Build.VERSION.SDK_INT >= 33) context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)) else @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0)
        val connection = (URL(BuildConfig.UPDATE_MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000; readTimeout = 8000; instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache, no-store")
        }
        try {
            val responseCode = try {
                connection.responseCode
            } catch (error: IOException) {
                throw UpdateCheckFailure.Network(error)
            }
            if (responseCode != HttpURLConnection.HTTP_OK) throw UpdateCheckFailure.Server(responseCode)
            val body = try {
                connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > 64 * 1024) {
                            throw UpdateCheckFailure.InvalidManifest(IllegalArgumentException("Manifest too large"))
                        }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
            } catch (error: IOException) {
                throw UpdateCheckFailure.Network(error)
            }
            val manifest = try {
                UpdateManifestParser.parse(
                    body.toString(Charsets.UTF_8),
                    allowedHost = BuildConfig.UPDATE_SERVER_HOST,
                    allowedPort = BuildConfig.UPDATE_SERVER_PORT,
                )
            } catch (error: Exception) {
                throw UpdateCheckFailure.InvalidManifest(error)
            }
            if (manifest.available && manifest.signerSha256 != trusted) {
                throw UpdateCheckFailure.InvalidManifest(IllegalArgumentException("Untrusted release signer"))
            }
            val installedCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else @Suppress("DEPRECATION") installed.versionCode.toLong()
            if (!UpdatePolicy.isNewer(manifest, installedCode)) return null
            return manifest
        } finally { connection.disconnect() }
    }

    fun download(manifest: UpdateManifest, onProgress: (Long) -> Unit): java.io.File {
        require(manifest.available && manifest.signerSha256 == BuildConfig.UPDATE_SIGNER_SHA256.lowercase())
        val url = URL(manifest.apkUrl!!)
        require(url.protocol == "https" && url.host == BuildConfig.UPDATE_SERVER_HOST && url.port in listOf(-1, BuildConfig.UPDATE_SERVER_PORT))
        // APKs can be large and Android may pause reads while verifying/downloading on a
        // constrained connection. Keep a finite but practical timeout; a short read timeout
        // caused valid slow downloads to be reported as interrupted.
        val conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 120000; instanceFollowRedirects = false }
        val updateCache = java.io.File(context.cacheDir, "updates").apply { mkdirs() }
        val file = java.io.File(updateCache, "taskoday-update.apk")
        try {
            require(conn.responseCode == 200)
            require(conn.contentLengthLong == -1L || conn.contentLengthLong == manifest.sizeBytes)
            conn.inputStream.use { input -> file.outputStream().use { output ->
                val buffer = ByteArray(32 * 1024); var total = 0L
                while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= manifest.sizeBytes!! && total <= 536870912L); output.write(buffer, 0, n); onProgress(total) }
                require(total == manifest.sizeBytes)
            } }
            val digest = file.inputStream().use { input ->
                val md = MessageDigest.getInstance("SHA-256")
                val bytes = ByteArray(32 * 1024)
                while (true) { val count = input.read(bytes); if (count < 0) break; md.update(bytes, 0, count) }
                md.digest().joinToString("") { "%02x".format(it) }
            }
            require(digest == manifest.sha256) { "Empreinte APK invalide" }
            val archiveSigningFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val info = if (Build.VERSION.SDK_INT >= 33) context.packageManager.getPackageArchiveInfo(file.path, PackageManager.PackageInfoFlags.of(archiveSigningFlags.toLong())) else @Suppress("DEPRECATION") context.packageManager.getPackageArchiveInfo(file.path, archiveSigningFlags)
            require(info?.packageName == UpdateManifestParser.PACKAGE) { "APK invalide" }
            val apkCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
            require(apkCode == manifest.versionCode!!.toLong()) { "Version APK invalide" }
            @Suppress("DEPRECATION")
            val certs = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners ?: info.signatures else info.signatures
            require(!certs.isNullOrEmpty()) { "Certificat APK absent" }
            val signer = MessageDigest.getInstance("SHA-256").digest(certs.single().toByteArray()).joinToString("") { "%02x".format(it) }
            require(signer == BuildConfig.UPDATE_SIGNER_SHA256.lowercase() && signer == manifest.signerSha256) { "Signature APK incompatible" }
            return file
        } catch (t: Throwable) { file.delete(); throw t } finally { conn.disconnect() }
    }
}

sealed class UpdateCheckFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable) : UpdateCheckFailure("Network request failed", cause)
    class Server(val statusCode: Int) : UpdateCheckFailure("Update server returned HTTP $statusCode")
    class InvalidManifest(cause: Throwable) : UpdateCheckFailure("Manifest or signer is invalid", cause)
}
