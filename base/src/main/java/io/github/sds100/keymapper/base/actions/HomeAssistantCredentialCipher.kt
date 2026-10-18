package io.github.sds100.keymapper.base.actions

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

class HomeAssistantCredentialCipher internal constructor(
    private val keyProvider: () -> SecretKey,
) {
    @Inject
    constructor() : this({ loadKey() })

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        cipher.updateAAD(ALIAS.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return "v1.${encoder.encodeToString(cipher.iv)}.${encoder.encodeToString(encrypted)}"
    }

    fun decrypt(value: String): String {
        val parts = value.split('.')
        require(parts.size == 3 && parts[0] == "v1")
        val decoder = Base64.getDecoder()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            keyProvider(),
            GCMParameterSpec(128, decoder.decode(parts[1])),
        )
        cipher.updateAAD(ALIAS.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(decoder.decode(parts[2])), Charsets.UTF_8)
    }

    private companion object {
        const val ALIAS = "keymapper_home_assistant_connection_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        val KEY_LOCK = Any()

        private fun loadKey(): SecretKey = synchronized(KEY_LOCK) {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                .apply {
                    init(
                        KeyGenParameterSpec.Builder(
                            ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                            .setKeySize(256)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .build(),
                    )
                }.generateKey()
        }
    }
}
