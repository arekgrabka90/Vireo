package app.vireo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSettingsDefaultsTest {

    @Test
    fun `handheld defaults use compact subtitle size`() {
        val settings = ProfileSettingsFactory.createDefaults(
            profileId = 7L,
            localeTag = "pl-PL",
            isTv = false,
        )

        assertEquals(7L, settings.profileId)
        assertEquals("pl-PL", settings.contentLanguage)
        assertEquals(18, settings.subtitleSizeSp)
        assertEquals("system", settings.themeMode)
        assertEquals("{}", settings.addonConfigsJson)
    }

    @Test
    fun `tv defaults use larger subtitle size`() {
        val settings = ProfileSettingsFactory.createDefaults(
            profileId = 8L,
            localeTag = "en-GB",
            isTv = true,
        )

        assertEquals(24, settings.subtitleSizeSp)
        assertEquals("en-GB", settings.contentLanguage)
    }

    @Test
    fun `invalid locale falls back to english`() {
        val settings = ProfileSettingsFactory.createDefaults(
            profileId = 9L,
            localeTag = "",
            isTv = false,
        )

        assertEquals("en-US", settings.contentLanguage)
    }

    @Test
    fun `live hud and epg description are enabled by default`() {
        val settings = ProfileSettingsFactory.createDefaults(
            profileId = 10L,
            localeTag = "pl-PL",
            isTv = true,
        )

        assertTrue(settings.liveTvHudShowEpgDescription)
        assertEquals("classic", settings.liveTvHudTechStyle)
        assertTrue(settings.liveTvStartOnLastChannel)
    }
}
