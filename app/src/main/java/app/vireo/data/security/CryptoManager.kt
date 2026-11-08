package app.vireo.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

class CryptoManager {
    companion object {
        private const val KEY_ALIAS = "vireo_account_secret_v1"
        private const val AES_PREFIX = "v1:"
        private const val PIN_PREFIX = "pbkdf2"
        private const val ITERATIONS = 120_000
        private const val SALT_BYTES = 16
        private const val HASH_BITS = 256
    }

    private fun secretKey(createIfMissing: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        check(createIfMissing) {
            "Brak klucza szyfrującego VIREO w Android Keystore"
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(createIfMissing = true))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return AES_PREFIX + Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(value: String): String {
        require(value.startsWith(AES_PREFIX)) { "Unsupported encrypted value" }
        val combined = Base64.decode(value.removePrefix(AES_PREFIX), Base64.NO_WRAP)
        require(combined.size > 12) { "Invalid encrypted value" }

        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(createIfMissing = false),
            GCMParameterSpec(128, iv),
        )
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun hashPin(pin: String): String {
        require(pin.length == 4 && pin.all(Char::isDigit)) {
            "PIN must contain four digits"
        }
        val algorithm = if (Build.VERSION.SDK_INT >= 26) "sha256" else "sha1"
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derivePin(pin, salt, ITERATIONS, algorithm)
        val separator = 36.toChar().toString()
        return listOf(
            PIN_PREFIX,
            algorithm,
            ITERATIONS.toString(),
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(hash, Base64.NO_WRAP),
        ).joinToString(separator)
    }

    fun verifyPin(pin: String, encoded: String?): Boolean {
        if (pin.length != 4 || !pin.all(Char::isDigit) || encoded.isNullOrBlank()) {
            return false
        }
        return try {
            val parts = encoded.split('$')
            val algorithm: String
            val iterations: Int
            val saltPart: String
            val hashPart: String

            when {
                // Wsteczna zgodność z wcześniejszym formatem SHA-256.
                parts.size == 4 && parts[0] == PIN_PREFIX -> {
                    algorithm = "sha256"
                    iterations = parts[1].toInt()
                    saltPart = parts[2]
                    hashPart = parts[3]
                }
                parts.size == 5 && parts[0] == PIN_PREFIX -> {
                    algorithm = parts[1]
                    iterations = parts[2].toInt()
                    saltPart = parts[3]
                    hashPart = parts[4]
                }
                else -> return false
            }

            if (algorithm !in setOf("sha1", "sha256")) return false
            if (iterations !in 50_000..1_000_000) return false

            val salt = Base64.decode(saltPart, Base64.NO_WRAP)
            val expected = Base64.decode(hashPart, Base64.NO_WRAP)
            val actual = derivePin(pin, salt, iterations, algorithm)
            MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) {
            false
        }
    }

    private fun derivePin(
        pin: String,
        salt: ByteArray,
        iterations: Int,
        algorithm: String,
    ): ByteArray {
        val factoryAlgorithm = when (algorithm) {
            "sha1" -> "PBKDF2WithHmacSHA1"
            "sha256" -> "PBKDF2WithHmacSHA256"
            else -> throw IllegalArgumentException("Unsupported PIN algorithm")
        }
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance(factoryAlgorithm)
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}
