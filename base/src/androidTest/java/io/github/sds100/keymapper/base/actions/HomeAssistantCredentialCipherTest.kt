package io.github.sds100.keymapper.base.actions

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeAssistantCredentialCipherTest {
    @Test
    fun connectionUsesNonExportableDeviceKeyAndFreshNonce() {
        val cipher = HomeAssistantCredentialCipher()
        val first = cipher.encrypt("test-token")
        val second = cipher.encrypt("test-token")
        assertNotEquals(first, second)
        assertEquals("test-token", HomeAssistantCredentialCipher().decrypt(first))

        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertNull(store.getKey("keymapper_home_assistant_connection_v1", null).encoded)
    }
}
