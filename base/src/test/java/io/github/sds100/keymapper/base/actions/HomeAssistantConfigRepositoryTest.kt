package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.repositories.FakePreferenceRepository
import io.github.sds100.keymapper.data.Keys
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class HomeAssistantConfigRepositoryTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = HomeAssistantCredentialCipher { key }
    private val preferences = FakePreferenceRepository()
    private val repository = HomeAssistantConfigRepository(preferences, cipher)

    private val action = ActionData.HomeAssistantService(
        description = "Light",
        serverUrl = "",
        accessToken = "",
        domain = "light",
        service = "toggle",
        entityId = "light.living_room",
        serviceData = "",
    )

    @Test
    fun `encryption failure never falls back to plaintext storage`() = runTest {
        val failingCipher = HomeAssistantCredentialCipher { error("Key unavailable") }
        val failingRepository = HomeAssistantConfigRepository(preferences, failingCipher)
        assertThrows(IllegalStateException::class.java) {
            failingRepository.save(HomeAssistantConnection("https://example.test", "secret-token"))
        }
        assertNull(preferences.get(Keys.homeAssistantConnection).first())
    }

    @Test
    fun `connection is encrypted at rest and survives repository recreation`() = runTest {
        repository.save(HomeAssistantConnection(" https://example.test/ ", " secret-token "))
        val stored = preferences.get(Keys.homeAssistantConnection).first()!!
        assertFalse(stored.contains("secret-token"))
        assertFalse(stored.contains("example.test"))
        assertEquals(
            HomeAssistantConnection("https://example.test", "secret-token"),
            HomeAssistantConfigRepository(preferences, cipher).connection.first(),
        )
    }

    @Test
    fun `shortcuts resolve the latest app connection`() = runTest {
        repository.save(HomeAssistantConnection("https://first.test", "first-token"))
        assertEquals("first-token", repository.resolve(action)?.accessToken)
        repository.save(HomeAssistantConnection("https://second.test", "second-token"))
        assertEquals("second-token", repository.resolve(action)?.accessToken)
    }

    @Test
    fun `legacy shortcut retains its connection`() = runTest {
        repository.save(HomeAssistantConnection("https://shared.test", "shared-token"))
        assertEquals(
            HomeAssistantConnection("https://legacy.test", "legacy-token"),
            repository.resolve(
                action.copy(serverUrl = "https://legacy.test", accessToken = "legacy-token"),
            ),
        )
    }

    @Test
    fun `restoring ciphertext without the device key requires configuration again`() = runTest {
        repository.save(HomeAssistantConnection("https://example.test", "secret-token"))
        val otherKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val restored =
            HomeAssistantConfigRepository(preferences, HomeAssistantCredentialCipher { otherKey })
        assertNull(restored.connection.first())
    }

    @Test
    fun `encryption uses a fresh nonce and rejects tampering`() {
        val first = cipher.encrypt("secret-token")
        val second = cipher.encrypt("secret-token")
        assertNotEquals(first, second)
        assertEquals("secret-token", cipher.decrypt(first))
        val parts = first.split('.').toMutableList()
        parts[2] = (if (parts[2][0] == 'A') "B" else "A") + parts[2].substring(1)
        assertThrows(AEADBadTagException::class.java) { cipher.decrypt(parts.joinToString(".")) }
    }

    @Test
    fun `connection never prints credentials`() {
        val connection = HomeAssistantConnection("https://example.test", "secret-token")
        assertFalse(connection.toString().contains("secret-token"))
        assertFalse(connection.toString().contains("example.test"))
    }
}
