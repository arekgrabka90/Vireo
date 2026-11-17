package app.vireo.data

import app.vireo.data.local.AccountEntity
import app.vireo.data.security.CredentialUnavailableException
import app.vireo.data.security.CryptoManager
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class ProviderRegistryTest {

    private fun account(type: String = "XTREAM") = AccountEntity(
        id = 7L,
        name = "Xtream",
        server = "https://example.com",
        username = "user",
        password = "v1:encrypted",
        providerType = type,
    )

    private fun registry(crypto: CryptoManager) = ProviderRegistry(OkHttpClient(), Json, crypto)

    @Test
    fun decryptFailureBecomesCredentialUnavailable() {
        val crypto = Mockito.mock(CryptoManager::class.java)
        val failure = IllegalStateException("Brak klucza")
        Mockito.`when`(crypto.decrypt("v1:encrypted")).thenThrow(failure)

        val error = assertThrows(CredentialUnavailableException::class.java) {
            registry(crypto).create(account())
        }

        assertEquals(7L, error.accountId)
        assertSame(failure, error.cause)
    }

    @Test
    fun successfulDecryptCreatesProvider() {
        val crypto = Mockito.mock(CryptoManager::class.java)
        Mockito.`when`(crypto.decrypt("v1:encrypted")).thenReturn("secret")

        assertNotNull(registry(crypto).create(account()))
    }

    @Test
    fun unsupportedProviderIsNotReportedAsMissingCredentials() {
        val crypto = Mockito.mock(CryptoManager::class.java)

        val error = assertThrows(IllegalStateException::class.java) {
            registry(crypto).create(account("OTHER"))
        }

        assertTrue(error !is CredentialUnavailableException)
    }
}
