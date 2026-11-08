package app.vireo.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.vireo.data.local.AccountDao
import app.vireo.data.local.ProfileDao
import app.vireo.data.local.VireoDatabase
import app.vireo.data.security.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun cryptoManager(): CryptoManager = CryptoManager()

    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
        crypto: CryptoManager,
    ): VireoDatabase =
        Room.databaseBuilder(context, VireoDatabase::class.java, "vireo.db")
            .addMigrations(migration2To3(crypto))
            .build()

    private fun migration2To3(crypto: CryptoManager) = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val accounts = mutableListOf<Pair<Long, String>>()
            db.query("SELECT id, password FROM accounts").use { cursor ->
                while (cursor.moveToNext()) {
                    accounts += cursor.getLong(0) to cursor.getString(1)
                }
            }
            accounts.forEach { (id, password) ->
                if (!password.startsWith("v1:")) {
                    db.execSQL(
                        "UPDATE accounts SET password = ? WHERE id = ?",
                        arrayOf(crypto.encrypt(password), id),
                    )
                }
            }

            val profiles = mutableListOf<Pair<Long, String>>()
            db.query("SELECT id, pin FROM profiles WHERE pin IS NOT NULL AND pin != ''").use { cursor ->
                while (cursor.moveToNext()) {
                    profiles += cursor.getLong(0) to cursor.getString(1)
                }
            }
            profiles.forEach { (id, pin) ->
                if (!pin.startsWith("pbkdf2\$")) {
                    db.execSQL(
                        "UPDATE profiles SET pin = ? WHERE id = ?",
                        arrayOf(crypto.hashPin(pin), id),
                    )
                }
            }
        }
    }

    @Provides
    fun accountDao(db: VireoDatabase): AccountDao = db.accountDao()

    @Provides
    fun profileDao(db: VireoDatabase): ProfileDao = db.profileDao()
}
