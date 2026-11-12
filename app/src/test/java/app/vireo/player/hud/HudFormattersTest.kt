package app.vireo.player.hud

import app.vireo.domain.model.EpgProgram
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HudFormattersTest {

    private val program = EpgProgram(channelId = "c", title = "t", startTimeMs = 0L, endTimeMs = 3_600_000L)

    @Test fun `logo url must be absolute http or https`() {
        assertTrue(isUsableLogoUrl("https://img.example/logo.png"))
        assertTrue(isUsableLogoUrl("  HTTP://img.example/logo.png "))
        assertFalse(isUsableLogoUrl(null))
        assertFalse(isUsableLogoUrl(""))
        assertFalse(isUsableLogoUrl("/relative/logo.png"))
        assertFalse(isUsableLogoUrl("file:///sdcard/logo.png"))
    }

    @Test fun `progress is null outside the programme and exact inside`() {
        assertEquals(0.5f, program.progressAt(1_800_000L)!!, 0.0001f)
        assertNull(program.progressAt(-1L))
        assertNull(program.progressAt(3_600_000L))
        assertNull(program.copy(endTimeMs = 0L).progressAt(0L))
    }

    @Test fun `remaining minutes round up and are null outside the programme`() {
        assertEquals(30, program.remainingMinutesAt(1_800_000L))
        assertEquals(1, program.remainingMinutesAt(3_599_000L))
        assertNull(program.remainingMinutesAt(3_600_000L))
    }

    @Test fun `metadata uses only existing parts`() {
        assertEquals("2026  ·  Drama  ·  ★ 8.1", joinMetadata("2026", " Drama ", null, "8.1"))
        assertNull(joinMetadata(null, "", " ", null))
    }

    @Test fun `episode label uses only existing parts`() {
        assertEquals("S01  ·  E02  ·  Pilot", formatEpisodeLabel(1, 2, "Pilot"))
        assertEquals("E05", formatEpisodeLabel(null, 5, null))
        assertNull(formatEpisodeLabel(null, null, " "))
    }

    @Test fun `player time formats minutes and hours`() {
        assertEquals("0:00", formatPlayerTime(-5L))
        assertEquals("1:05", formatPlayerTime(65_000L))
        assertEquals("1:01:01", formatPlayerTime(3_661_000L))
    }

    @Test fun `time format uses injected patterns and zone`() {
        val format = HudTimeFormat("EEE, d MMM, HH:mm", "HH:mm", Locale.US, TimeZone.getTimeZone("UTC"))
        assertEquals("Thu, 1 Jan, 00:00", format.clock(0L))
        assertEquals("00:00 – 01:00", format.range(0L, 3_600_000L))
    }
}
