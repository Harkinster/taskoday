package com.example.taskoday.update

import com.google.gson.JsonParser
import java.net.URI

data class UpdateManifest(
    val available: Boolean, val packageName: String, val versionCode: Int?, val versionName: String?,
    val apkUrl: String?, val sha256: String?, val sizeBytes: Long?, val mandatory: Boolean,
    val minSupportedVersionCode: Int?, val changelog: List<String>, val signerSha256: String?,
)

object UpdateManifestParser {
    const val PACKAGE = "com.harkinster.taskoday"
    private const val HOST = "harkserv.ddns.net"

    fun parse(json: String, allowedHost: String = HOST, allowedPort: Int = 443): UpdateManifest {
        require(json.length <= 64 * 1024) { "Manifeste trop volumineux" }
        val root = JsonParser.parseString(json).asJsonObject
        require(root.get("schemaVersion")?.asInt == 1) { "Version de schéma inconnue" }
        require(root.get("packageName")?.asString == PACKAGE) { "Package inattendu" }
        val available = root.get("available")?.asBoolean ?: error("Champ available manquant")
        val mandatory = root.get("mandatory")?.asBoolean ?: error("Champ mandatory manquant")
        val changes = root.get("changelog")?.asJsonArray?.map { it.asString } ?: error("Changelog manquant")
        require(changes.size <= 30 && changes.all { it.isNotBlank() && it.length <= 400 })
        if (!available) {
            require(!mandatory && changes.isEmpty())
            listOf("versionCode", "versionName", "apkUrl", "sha256", "sizeBytes", "minSupportedVersionCode", "publishedAt", "signerSha256").forEach {
                require(root.get(it)?.isJsonNull == true) { "Le champ $it doit être null sans mise à jour" }
            }
            return UpdateManifest(false, PACKAGE, null, null, null, null, null, false, null, emptyList(), null)
        }
        val code = root.get("versionCode")?.asInt ?: error("versionCode manquant")
        val name = root.get("versionName")?.asString ?: error("versionName manquant")
        val url = root.get("apkUrl")?.asString ?: error("apkUrl manquant")
        val uri = URI(url)
        require(uri.scheme == "https" && uri.host == allowedHost && uri.userInfo == null && uri.port in listOf(-1, allowedPort)) { "URL non autorisée" }
        require(Regex("^/taskoday(?:-test)?/releases/v[0-9]+-[0-9a-f]{16}/taskoday-[A-Za-z0-9._+-]+\\.apk$").matches(uri.path))
        require(code > 0 && Regex("^[A-Za-z0-9][A-Za-z0-9._+-]{0,63}$").matches(name))
        require(code >= (root.get("minSupportedVersionCode")?.asInt ?: 1))
        val hash = root.get("sha256")?.asString ?: error("SHA-256 manquant")
        val signer = root.get("signerSha256")?.asString ?: error("Certificat manquant")
        require(Regex("^[0-9a-f]{64}$").matches(hash) && Regex("^[0-9a-f]{64}$").matches(signer))
        val size = root.get("sizeBytes")?.asLong ?: error("Taille manquante")
        require(size in 1..536870912L && changes.isNotEmpty())
        val minimum = root.get("minSupportedVersionCode")?.asInt ?: error("Version minimale manquante")
        require(minimum > 0)
        val publishedAt = root.get("publishedAt")?.asString ?: error("Date de publication manquante")
        require(runCatching { java.time.OffsetDateTime.parse(publishedAt) }.isSuccess)
        return UpdateManifest(true, PACKAGE, code, name, url, hash, size, mandatory, minimum, changes, signer)
    }
}

object UpdatePolicy {
    fun isNewer(manifest: UpdateManifest, installedVersionCode: Long): Boolean =
        manifest.available && (manifest.versionCode ?: 0) > installedVersionCode
}
