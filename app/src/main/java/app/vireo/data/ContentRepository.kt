package app.vireo.data

import app.vireo.data.local.AccountDao
import app.vireo.data.local.AccountEntity
import app.vireo.data.xtream.XtreamProvider
import app.vireo.domain.model.AccountInfo
import app.vireo.domain.provider.ContentProvider
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    private val dao: AccountDao,
    private val client: OkHttpClient,
    private val json: Json,
) {
    private var cached: ContentProvider? = null

    suspend fun hasAccount(): Boolean = dao.getActive() != null

    suspend fun provider(): ContentProvider? {
        cached?.let { return it }
        val account = dao.getActive() ?: return null
        return XtreamProvider(account.server, account.username, account.password, client, json)
            .also { cached = it }
    }

    suspend fun login(server: String, username: String, password: String): Result<AccountInfo> {
        val provider = XtreamProvider(server, username.trim(), password, client, json)
        val result = provider.authenticate()
        if (result.isSuccess) {
            dao.clear()
            dao.insert(
                AccountEntity(
                    name = "Xtream",
                    server = server.trim(),
                    username = username.trim(),
                    password = password,
                ),
            )
            cached = provider
        }
        return result
    }

    suspend fun logout() {
        dao.clear()
        cached = null
    }
}
