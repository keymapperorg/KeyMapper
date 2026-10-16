package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class HomeAssistantConnection(val serverUrl: String, val accessToken: String) {
    override fun toString(): String = "HomeAssistantConnection(redacted)"
}

class HomeAssistantConfigRepository @Inject constructor(
    private val preferences: PreferenceRepository,
    private val cipher: HomeAssistantCredentialCipher,
) {
    val connection = preferences.get(Keys.homeAssistantConnection).map { value ->
        value?.let {
            runCatching { Json.decodeFromString<HomeAssistantConnection>(cipher.decrypt(it)) }
                .onFailure {
                    Timber.tag(
                        "HomeAssistant",
                    ).e("Saved connection could not be decrypted; configure it again")
                }.getOrNull()
        }
    }.flowOn(Dispatchers.IO)

    fun save(connection: HomeAssistantConnection) {
        preferences.set(
            Keys.homeAssistantConnection,
            cipher.encrypt(
                Json.encodeToString(
                    connection.copy(
                        serverUrl = connection.serverUrl.trim().trimEnd('/'),
                        accessToken = connection.accessToken.trim(),
                    ),
                ),
            ),
        )
    }

    suspend fun resolve(action: ActionData.HomeAssistantService): HomeAssistantConnection? {
        // Old actions retain their embedded connection until edited and saved again.
        if (action.serverUrl.isNotBlank() && action.accessToken.isNotBlank()) {
            return HomeAssistantConnection(action.serverUrl, action.accessToken)
        }
        return connection.first()
    }
}
