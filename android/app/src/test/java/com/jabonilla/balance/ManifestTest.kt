package com.jabonilla.balance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** AC-2 checks on the app's own manifest and sources. Library entries are checked on the merged manifest by hand. */
class ManifestTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test fun ac2_noInternetNoBackup() {
        assertFalse(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
    }

    @Test fun ac2_onlyExpectedComponentsAreExported() {
        val exported = Regex("""<(activity|activity-alias|receiver|service|provider)\b[^>]*?android:name="([^"]+)"[^>]*?android:exported="true"""")
            .findAll(manifest).map { it.groupValues[2] }.toSet()
        assertEquals(setOf(".MainActivity", ".PrivacyActivity", "ViewPermissionUsageActivity"), exported)
        assertTrue(Regex("""<receiver\s+android:name="\.TimerReceiver"\s+android:exported="false"""").containsMatchIn(manifest))
    }

    @Test fun ac2_pendingIntentsAreImmutable() {
        val calls = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            // Each call ends at the first line that closes it; check the text up to the next blank line or "fun".
            Regex("""PendingIntent\.get(Activity|Broadcast|Service)\([^\n]*\n?[^\n]*""").findAll(f.readText()).map { it.value }
        }.toList()
        assertEquals(2, calls.size)
        calls.forEach { assertTrue(it, it.contains("FLAG_IMMUTABLE")) }
    }
}
