package app.dreamcue.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseSyncFailureMessageTest {
    @Test
    fun mapsFirebaseConfigurationErrorToUserFacingStatus() {
        val message = firebaseSyncFailureMessage(
            RuntimeException("An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]"),
            "Sync account creation failed.",
        )

        assertEquals("Sync account setup is not available yet.", message)
    }

    @Test
    fun mapsKnownAuthErrorsToUserFacingStatus() {
        assertEquals(
            "An account already exists for this email.",
            firebaseSyncFailureMessage(RuntimeException("[ EMAIL_EXISTS ]"), "Sync account creation failed."),
        )
        assertEquals(
            "An account already exists for this email.",
            firebaseSyncFailureMessage(
                RuntimeException("The email address is already in use by another account."),
                "Sync account creation failed.",
            ),
        )
        assertEquals(
            "Email or password is incorrect.",
            firebaseSyncFailureMessage(RuntimeException("[ INVALID_LOGIN_CREDENTIALS ]"), "Sync sign-in failed."),
        )
    }

    @Test
    fun mapsPasswordResetErrorsToUserFacingStatus() {
        assertEquals(
            "Enter a valid email address.",
            firebasePasswordResetFailureMessage(RuntimeException("[ INVALID_EMAIL ]")),
        )
        assertEquals(
            "Sync account setup is not available yet.",
            firebasePasswordResetFailureMessage(RuntimeException("[ CONFIGURATION_NOT_FOUND ]")),
        )
        assertEquals(
            "Password reset email could not be sent.",
            firebasePasswordResetFailureMessage(RuntimeException("[ EMAIL_NOT_FOUND ]")),
        )
    }
}
