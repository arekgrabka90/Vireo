package app.vireo.di

import android.content.Context
import androidx.room.Room
import app.vireo.data.local.AccountDao
import app.vireo.data.local.ProfileDao
import app.vireo.data.local.VireoDatabase
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
    fun database(@ApplicationContext context: Context): VireoDatabase =
        Room.databaseBuilder(context, VireoDatabase::class.java, "vireo.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun accountDao(db: VireoDatabase): AccountDao = db.accountDao()

    @Provides
    fun profileDao(db: VireoDatabase): ProfileDao = db.profileDao()
}
