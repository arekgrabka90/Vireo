package app.vireo.data

import android.content.Context
import android.content.pm.PackageManager
import app.vireo.data.local.ProfileSettingsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSettingsFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun create(
        profileId: Long,
        kidsMode: Boolean = false,
    ): ProfileSettingsEntity {
        val localeTag = context.resources.configuration.locales
            .get(0)
            ?.toLanguageTag()
            ?.takeIf { it.isNotBlank() && it != "und" }
            ?: "en-US"

        val isTv = context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_LEANBACK
        )

        return createDefaults(
            profileId = profileId,
            localeTag = localeTag,
            isTv = isTv,
            kidsMode = kidsMode,
        )
    }

    companion object {
        fun createDefaults(
            profileId: Long,
            localeTag: String,
            isTv: Boolean,
            kidsMode: Boolean = false,
        ): ProfileSettingsEntity {
            val language = localeTag
                .takeIf { it.isNotBlank() && it != "und" }
                ?: "en-US"

            return ProfileSettingsEntity(
                profileId = profileId,
                contentLanguage = language,
                preferredAudioLanguage = "system",
                preferredSubtitleLanguage = "system",
                subtitlesEnabled = false,
                subtitleSizeSp = if (isTv) 24 else 18,
                themeMode = "system",
                accentColor = "violet",
                autoplayNext = true,
                liveTvHudTechStyle = "classic",
                liveTvHudShowEpgDescription = true,
                liveTvStartOnLastChannel = true,
                epgRefreshIntervalMinutes = 360,
                showWatched = true,
                continueWatchingEnabled = true,
                addonConfigsJson = "{}",
            )
        }
    }
}

object ProfileSettingsJson {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return "{}"

        return try {
            val parsed = json.parseToJsonElement(raw)
            if (parsed is JsonObject) parsed.toString() else "{}"
        } catch (_: Exception) {
            "{}"
        }
    }
}
