package app.vireo.player.hud

import app.vireo.player.PlayerHudContent
import app.vireo.player.RecentLiveChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HudTilesTest {

    private fun live(vararg recents: RecentLiveChannel) = PlayerHudContent.Live(
        channelName = "Now", epgChannelId = null, streamId = "1", logoUrl = null,
        currentProgram = null, currentProgramDescription = null, nextProgram = null,
        recentChannels = recents.toList(),
        sourceId = 1L,
    )

    private fun channel(streamId: String) = RecentLiveChannel(1L, streamId, "Ch $streamId", null, null, 0L)

    @Test fun `no actions means no tiles`() {
        assertTrue(buildHudTiles(live(channel("2")), hasSelectChannelAction = false, hasGuideAction = false).isEmpty())
    }

    @Test fun `current channel is excluded and guide comes first`() {
        val tiles = buildHudTiles(live(channel("1"), channel("2")), hasSelectChannelAction = true, hasGuideAction = true)
        assertEquals(listOf("guide", "1:2"), tiles.map { it.key })
    }
}
