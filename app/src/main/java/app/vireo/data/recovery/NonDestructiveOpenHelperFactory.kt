package app.vireo.data.recovery

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

private const val TAG = "VireoDb"

/**
 * Fabryka otwierania bazy, która nigdy nie usuwa pliku uszkodzonej bazy.
 *
 * Domyślne `SupportSQLiteOpenHelper.Callback.onCorruption` usuwa plik bazy.
 * Tu całe otwieranie jest delegowane do [delegate], ale plik zostaje na
 * miejscu; obsługą (eksport, kopia, odsunięcie) zajmuje się ekran odzyskiwania.
 */
class NonDestructiveOpenHelperFactory(
    private val delegate: SupportSQLiteOpenHelper.Factory,
) : SupportSQLiteOpenHelper.Factory {

    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration,
    ): SupportSQLiteOpenHelper {
        val wrapped = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
            .name(configuration.name)
            .callback(NonDestructiveCallback(configuration.callback))
            .noBackupDirectory(configuration.useNoBackupDirectory)
            .allowDataLossOnRecovery(configuration.allowDataLossOnRecovery)
            .build()
        return delegate.create(wrapped)
    }

    companion object {
        /** Fabryka używana przez aplikację i testy. */
        fun default(): SupportSQLiteOpenHelper.Factory =
            NonDestructiveOpenHelperFactory(FrameworkSQLiteOpenHelperFactory())
    }
}

/** Deleguje wszystko poza `onCorruption`, które tylko zapisuje wpis w logu. */
class NonDestructiveCallback(
    private val delegate: SupportSQLiteOpenHelper.Callback,
) : SupportSQLiteOpenHelper.Callback(delegate.version) {

    override fun onConfigure(db: SupportSQLiteDatabase) {
        delegate.onConfigure(db)
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        delegate.onCreate(db)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        delegate.onUpgrade(db, oldVersion, newVersion)
    }

    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        delegate.onDowngrade(db, oldVersion, newVersion)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        delegate.onOpen(db)
    }

    override fun onCorruption(db: SupportSQLiteDatabase) {
        Log.w(TAG, "Wykryto uszkodzenie bazy danych; plik nie zostaje usunięty.")
    }
}
