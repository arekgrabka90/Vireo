package app.vireo.data.recovery

import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import java.io.File

/**
 * Odczytuje stan pliku bazy przed otwarciem przez Room.
 *
 * Używa własnego [DatabaseErrorHandler], który niczego nie robi: domyślny
 * handler Androida usuwa plik uszkodzonej bazy, a tego tu nie wolno.
 */
class SqliteDatabaseInspector(private val dbFile: File) {

    fun probe(): DatabaseProbe {
        if (!dbFile.isFile || dbFile.length() == 0L) return DatabaseProbe.Absent
        return try {
            read(SQLiteDatabase.OPEN_READONLY)
        } catch (e: SQLiteDatabaseCorruptException) {
            DatabaseProbe.Corrupt(describe(e))
        } catch (e: SQLiteException) {
            try {
                read(SQLiteDatabase.OPEN_READWRITE)
            } catch (inner: SQLiteDatabaseCorruptException) {
                DatabaseProbe.Corrupt(describe(inner))
            } catch (inner: SQLiteException) {
                DatabaseProbe.Unreadable(describe(inner))
            }
        }
    }

    /** null: plik przeszedł `PRAGMA quick_check`; w przeciwnym razie opis problemu. */
    fun integrityIssue(): String? = try {
        val db = SQLiteDatabase.openDatabase(
            dbFile.path, null, SQLiteDatabase.OPEN_READONLY, NoDeleteErrorHandler,
        )
        db.use {
            it.rawQuery("PRAGMA quick_check(1)", null).use { cursor ->
                val first = if (cursor.moveToFirst()) cursor.getString(0) else null
                when {
                    first == null -> "brak wyniku kontroli spójności"
                    first.equals("ok", ignoreCase = true) -> null
                    else -> first.take(200)
                }
            }
        }
    } catch (e: SQLiteException) {
        describe(e)
    }

    private fun read(flags: Int): DatabaseProbe {
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, flags, NoDeleteErrorHandler)
        return db.use {
            val version = it.version
            if (version == 0 && tableCount(it) == 0) {
                DatabaseProbe.Absent
            } else {
                DatabaseProbe.Readable(version)
            }
        }
    }

    private fun tableCount(db: SQLiteDatabase): Int =
        db.rawQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'",
            null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

    private fun describe(e: SQLiteException): String =
        (e.javaClass.simpleName + ": " + e.message.orEmpty().replace(Regex("\\s+"), " ").trim())
            .take(220)

    private object NoDeleteErrorHandler : DatabaseErrorHandler {
        override fun onCorruption(dbObj: SQLiteDatabase) {
            // Celowo bez usuwania pliku.
        }
    }
}
