package app.vireo.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResumePolicyTest {

    @Test
    fun `live pause boundary is strictly above two minutes`() {
        assertEquals(ResumeAction.RESUME_IN_PLACE, PlaybackResumePolicy.onResume(true, 120_000L))
        assertEquals(ResumeAction.RESET_SOURCE, PlaybackResumePolicy.onResume(true, 120_001L))
    }

    @Test
    fun `vod always keeps its position`() {
        assertEquals(ResumeAction.RESUME_IN_PLACE, PlaybackResumePolicy.onResume(false, 3_600_000L))
        assertEquals(
            ResumeAction.RESUME_IN_PLACE,
            PlaybackResumePolicy.onResume(false, 3_600_000L, sourceReleased = true),
        )
    }

    @Test
    fun `released live source is always reset`() {
        assertEquals(
            ResumeAction.RESET_SOURCE,
            PlaybackResumePolicy.onResume(true, 0L, sourceReleased = true),
        )
    }

    @Test
    fun `negative durations are treated as zero`() {
        assertEquals(ResumeAction.RESUME_IN_PLACE, PlaybackResumePolicy.onResume(true, -5L))
        assertFalse(PlaybackResumePolicy.shouldReleaseLiveInBackground(true, -1L))
    }

    @Test
    fun `live background release happens at sixty seconds`() {
        assertFalse(PlaybackResumePolicy.shouldReleaseLiveInBackground(true, 59_999L))
        assertTrue(PlaybackResumePolicy.shouldReleaseLiveInBackground(true, 60_000L))
        assertFalse(PlaybackResumePolicy.shouldReleaseLiveInBackground(false, 600_000L))
    }
}
