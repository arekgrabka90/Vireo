package app.vireo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSettingsJsonTest {

    @Test
    fun `empty addon configuration becomes empty object`() {
        assertEquals("{}", ProfileSettingsJson.normalize(null))
        assertEquals("{}", ProfileSettingsJson.normalize(""))
        assertEquals("{}", ProfileSettingsJson.normalize("   "))
    }

    @Test
    fun `valid addon object is retained`() {
        val result = ProfileSettingsJson.normalize(
            """{"org.example.addon":{"version":1,"enabled":true}}"""
        )

        assertEquals(
            """{"org.example.addon":{"version":1,"enabled":true}}""",
            result,
        )
    }

    @Test
    fun `invalid json becomes empty object`() {
        assertEquals("{}", ProfileSettingsJson.normalize("{invalid"))
    }

    @Test
    fun `non object json becomes empty object`() {
        assertEquals("{}", ProfileSettingsJson.normalize("""["addon"]"""))
        assertEquals("{}", ProfileSettingsJson.normalize("true"))
    }
}
