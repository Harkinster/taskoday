package com.example.taskoday.update

import org.junit.Assert.*
import org.junit.Test

class UpdateManifestParserTest {
    private val unavailable = """{"schemaVersion":1,"available":false,"packageName":"com.harkinster.taskoday","versionCode":null,"versionName":null,"apkUrl":null,"sha256":null,"sizeBytes":null,"mandatory":false,"minSupportedVersionCode":null,"changelog":[],"publishedAt":null,"signerSha256":null}"""
    @Test fun unavailableManifestIsAccepted() { assertFalse(UpdateManifestParser.parse(unavailable).available) }
    @Test fun malformedPackageIsRejected() { assertThrows(IllegalArgumentException::class.java) { UpdateManifestParser.parse(unavailable.replace("com.harkinster.taskoday", "other.app")) } }
    @Test fun malformedJsonIsRejected() { assertThrows(RuntimeException::class.java) { UpdateManifestParser.parse("{") } }
    @Test fun unavailableCannotBeMandatory() { assertThrows(IllegalArgumentException::class.java) { UpdateManifestParser.parse(unavailable.replace("\"mandatory\":false", "\"mandatory\":true")) } }
    private val available = """{"schemaVersion":1,"available":true,"packageName":"com.harkinster.taskoday","versionCode":6,"versionName":"1.0-beta05","apkUrl":"https://harkserv.ddns.net/taskoday/releases/v6-0123456789abcdef/taskoday-1.0-beta05.apk","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","sizeBytes":1234,"mandatory":false,"minSupportedVersionCode":5,"changelog":["Améliorations"],"publishedAt":"2026-10-10T10:00:00Z","signerSha256":"4783e3e7f20c4bfec5e6b22fb0e3cfcaeb0dfddca3ebc380929c7fb1243a666b"}"""
    @Test fun newerUpdateIsDetected() { assertTrue(UpdatePolicy.isNewer(UpdateManifestParser.parse(available), 5)) }
    @Test fun mandatoryReleaseIsPreservedFromManifest() { assertTrue(UpdateManifestParser.parse(available.replace("\"mandatory\":false", "\"mandatory\":true")).mandatory) }
    @Test fun isolatedQaHostAndTlsPortAreAcceptedOnlyWhenExplicitlyConfigured() {
        val qa = available.replace("https://harkserv.ddns.net", "https://10.0.2.2:8443")
        assertTrue(UpdateManifestParser.parse(qa, allowedHost = "10.0.2.2", allowedPort = 8443).available)
        assertThrows(IllegalArgumentException::class.java) { UpdateManifestParser.parse(qa) }
    }
    @Test fun identicalAndOlderVersionsAreIgnored() { val m = UpdateManifestParser.parse(available); assertFalse(UpdatePolicy.isNewer(m, 6)); assertFalse(UpdatePolicy.isNewer(m, 7)) }
    @Test fun nonHttpsOrOtherHostIsRejected() { assertThrows(IllegalArgumentException::class.java) { UpdateManifestParser.parse(available.replace("https://harkserv.ddns.net", "http://evil.example")) } }
    @Test fun malformedDigestIsRejected() { assertThrows(IllegalArgumentException::class.java) { UpdateManifestParser.parse(available.replace("a".repeat(64), "badhash")) } }
    @Test fun badMinimumVersionIsRejected() { assertThrows(IllegalArgumentException::class.java) { UpdateManifestParser.parse(available.replace("\"minSupportedVersionCode\":5", "\"minSupportedVersionCode\":7")) } }
}
