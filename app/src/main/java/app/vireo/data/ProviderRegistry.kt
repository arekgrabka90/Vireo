package app.vireo.data

import app.vireo.data.local.AccountEntity
import app.vireo.data.security.CryptoManager
import app.vireo.data.xtream.XtreamProvider
import app.vireo.domain.provider.ContentProvider
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralny rejestr dostawców treści.
 * Xtream jest pierwszym obsługiwanym dostawcą.
 */
@Singleton
class ProviderRegistry @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val crypto: CryptoManager,
) {
    fun createXtream(
        server: String,
        username: String,
        password: String,
    ): XtreamProvider = XtreamProvider(
        server,
        username,
        password,
        client,
        json,
    )

    fun create(account: AccountEntity): ContentProvider =
        when (account.providerType.uppercase()) {
            "XTREAM" -> createXtream(
                account.server,
                account.username,
                crypto.decrypt(account.password),
            )
            else -> error("Unsupported content provider")
        }
}
