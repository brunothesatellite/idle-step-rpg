package com.brunothesatellite.idlesteprpg.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateCheckerTest {

    @Test
    fun newerPatchVersionIsDetected() {
        assertTrue(UpdateChecker.isNewer("0.6.1", "0.6.2"))
        assertTrue(UpdateChecker.isNewer("0.6.1", "v0.6.2"))
    }

    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("0.6.1", "0.6.1"))
        assertFalse(UpdateChecker.isNewer("0.6.1", "v0.6.1"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("0.6.2", "0.6.1"))
    }

    @Test
    fun numericComparisonHandlesTwoDigits() {
        assertTrue(UpdateChecker.isNewer("0.6.9", "0.6.10"))
        assertFalse(UpdateChecker.isNewer("0.6.10", "0.6.9"))
    }

    @Test
    fun majorAndMinorVersionsAreCompared() {
        assertTrue(UpdateChecker.isNewer("0.9.9", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("0.6.9", "0.7.0"))
        assertFalse(UpdateChecker.isNewer("1.0.0", "0.9.9"))
    }

    @Test
    fun tagSuffixIsIgnored() {
        assertFalse(UpdateChecker.isNewer("0.6.2", "v0.6.2-beta"))
        assertTrue(UpdateChecker.isNewer("0.6.1", "v0.6.2-beta"))
    }

    @Test
    fun sha256IsExtractedFromReleaseNotes() {
        val body = """
            ## Installation

            ```bash
            adb install -r La-Marche-des-Ames-v0.6.2.apk
            ```

            SHA-256 APK : 23E0DBCC3D54207486437B2C1B1F4E3DED4BA785EE6F8D6FFE13F800D51B18F8
        """.trimIndent()
        assertEquals(
            "23e0dbcc3d54207486437b2c1b1f4e3ded4ba785ee6f8d6ffe13f800d51b18f8",
            UpdateChecker.extractSha256(body)
        )
    }

    @Test
    fun missingSha256ReturnsNull() {
        assertNull(UpdateChecker.extractSha256("Aucune empreinte dans ces notes."))
    }

    @Test
    fun latestReleaseIsParsedWithApkAsset() {
        val json = """
            {
              "tag_name": "v0.6.2",
              "html_url": "https://github.com/brunothesatellite/idle-step-rpg/releases/tag/v0.6.2",
              "body": "SHA-256 APK : 23E0DBCC3D54207486437B2C1B1F4E3DED4BA785EE6F8D6FFE13F800D51B18F8",
              "assets": [
                {
                  "name": "La-Marche-des-Ames-v0.6.2.aab",
                  "browser_download_url": "https://example.test/app.aab"
                },
                {
                  "name": "La-Marche-des-Ames-v0.6.2.apk",
                  "browser_download_url": "https://example.test/app.apk"
                }
              ]
            }
        """.trimIndent()
        val info = UpdateChecker.parseLatest(json)
        assertEquals("v0.6.2", info?.tag)
        assertEquals("0.6.2", info?.version)
        assertEquals("https://example.test/app.apk", info?.apkUrl)
        assertEquals(
            "https://github.com/brunothesatellite/idle-step-rpg/releases/tag/v0.6.2",
            info?.releaseUrl
        )
        assertEquals(
            "23e0dbcc3d54207486437b2c1b1f4e3ded4ba785ee6f8d6ffe13f800d51b18f8",
            info?.sha256
        )
    }

    @Test
    fun releaseWithoutApkIsRejected() {
        val json = """
            {
              "tag_name": "v0.6.2",
              "html_url": "https://example.test/release",
              "assets": [
                { "name": "app.aab", "browser_download_url": "https://example.test/app.aab" }
              ]
            }
        """.trimIndent()
        assertNull(UpdateChecker.parseLatest(json))
    }

    @Test
    fun invalidJsonIsRejected() {
        assertNull(UpdateChecker.parseLatest("not json"))
    }

    @Test
    fun localSha256IsComputed() {
        val file = File.createTempFile("update-test", ".apk")
        try {
            file.writeText("abc")
            assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                UpdateChecker.sha256(file)
            )
        } finally {
            file.delete()
        }
    }
}
