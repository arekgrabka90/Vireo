package app.vireo.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * Testy używają osobnego aliasu klucza, więc nie dotykają klucza aplikacji.
 */
@RunWith(AndroidJUnit4::class)
class CryptoManagerKeyLossTest {

    private val alias = "vireo_test_key_" + System.nanoTime()

    private fun deleteKey() {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.deleteEntry(alias)
    }

    @After
    fun tearDown() {
        deleteKey()
    }

    @Test
    fun deletedKeyMakesCiphertextUnreadable() {
        val crypto = CryptoManager(alias)
        val secret = crypto.encrypt("haslo")
        assertTrue(crypto.canDecrypt(secret))
        assertEquals("haslo", crypto.decrypt(secret))

        deleteKey()

        assertFalse(crypto.canDecrypt(secret))
    }

    @Test
    fun recreatedKeyCannotReadOldCiphertext() {
        val crypto = CryptoManager(alias)
        val old = crypto.encrypt("stare")
        deleteKey()

        val fresh = crypto.encrypt("nowe")

        assertTrue(crypto.canDecrypt(fresh))
        assertEquals("nowe", crypto.decrypt(fresh))
        assertFalse(crypto.canDecrypt(old))
    }

    @Test
    fun pinHashDoesNotDependOnKeystoreKey() {
        val crypto = CryptoManager(alias)
        val hash = crypto.hashPin("1234")
        deleteKey()

        assertTrue(crypto.verifyPin("1234", hash))
        assertFalse(crypto.verifyPin("1235", hash))
    }
}
