package app.vireo.data.recovery

import android.content.Context

/** Magazyn znaczników startu w SharedPreferences; zapis synchroniczny, bo ma przetrwać awarię. */
class SharedPrefsStartMarkerStore(context: Context) : StartMarkerStore {

    private val prefs = context.applicationContext
        .getSharedPreferences("vireo_startup", Context.MODE_PRIVATE)

    override var pendingStart: Boolean
        get() = prefs.getBoolean(KEY_PENDING, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PENDING, value).commit()
        }

    override var unstableStarts: Int
        get() = prefs.getInt(KEY_UNSTABLE, 0)
        set(value) {
            prefs.edit().putInt(KEY_UNSTABLE, value).commit()
        }

    private companion object {
        const val KEY_PENDING = "pending_start"
        const val KEY_UNSTABLE = "unstable_starts"
    }
}
