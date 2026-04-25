package app.dreamcue.ui

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconAssetTest {
    @Test
    fun launcherIconMatchesReferenceDesignPixels() {
        val foregroundFile = File("src/main/res/drawable/ic_launcher_foreground.png")
        val referenceFile = File("src/test/resources/dreamcue_reference_icon.png")
        val background = File("src/main/res/values/ic_launcher_background.xml").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(foregroundFile.exists())
        assertTrue(referenceFile.exists())
        assertTrue(background.contains("#0F3D2E"))
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
        assertTrue(sha256(foregroundFile) == sha256(referenceFile))
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
