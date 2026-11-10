package app.vireo.data

import app.vireo.data.local.AppSettingsEntity
import app.vireo.data.local.ProfileSettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSettingsSanitizerTest {

    @Test
    fun `profile values are clamped and invalid options use defaults`() {
        val result = ProfileSettingsSanitizer.profile(
            ProfileSettingsEntity(
                profileId = 42L,
                contentLanguage = "not a locale!",
                preferredAudioLanguage = "",
                preferredSubtitleLanguage = "es-ES",
                subtitleSizeSp = 100,
                themeMode = "unknown",
                accentColor = "invalid",
                liveTvHudTechStyle = "invalid",
                epgRefreshIntervalMinutes = 1,
                addonConfigsJson = "{broken",
            )
        )

        assertEquals(42L, result.profileId)
        assertEquals("en-US", result.contentLanguage)
        assertEquals("system", result.preferredAudioLanguage)
        assertEquals("es-ES", result.preferredSubtitleLanguage)
        assertEquals(48, result.subtitleSizeSp)
        assertEquals("system", result.themeMode)
        assertEquals("violet", result.accentColor)
        assertEquals("classic", result.liveTvHudTechStyle)
        assertEquals(15, result.epgRefreshIntervalMinutes)
        assertEquals("{}", result.addonConfigsJson)
    }

    @Test
    fun `valid profile preferences are preserved`() {
        val original = ProfileSettingsEntity(
            profileId = 7L,
            contentLanguage = "pl-PL",
            preferredAudioLanguage = "en-US",
            preferredSubtitleLanguage = "system",
            subtitleSizeSp = 22,
            themeMode = "dark",
            accentColor = "teal",
            liveTvHudTechStyle = "chips",
            epgRefreshIntervalMinutes = 720,
        )

        assertEquals(original, ProfileSettingsSanitizer.profile(original))
    }

    @Test
    fun `app settings always target singleton row and normalize language`() {
        val result = ProfileSettingsSanitizer.app(
            AppSettingsEntity(
                id = 99,
                appLanguageTag = "bad language",
                notificationsEnabled = false,
                diagnosticsEnabled = true,
            )
        )

        assertEquals(1, result.id)
        assertEquals("system", result.appLanguageTag)
        assertEquals(false, result.notificationsEnabled)
        assertEquals(true, result.diagnosticsEnabled)
    }
}
