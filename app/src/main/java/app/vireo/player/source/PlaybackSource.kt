package app.vireo.player.source

import app.vireo.player.PlaybackRequestIdentity
import app.vireo.player.PlaybackSourceCandidate
import app.vireo.player.normalizePlaybackHeaders
import app.vireo.player.playbackRequestIdentity

enum class PlaybackSourceRole {
    PRIMARY,
    ALTERNATIVE,
}

/**
 * HTTP playback source description.
 *
 * Credentials and request headers are deliberately excluded
 * from toString() diagnostics.
 */
class PlaybackSource(
    val url: String,
    val sourceId: Long? = null,
    val streamId: String? = null,
    headers: Map<String, String> = emptyMap(),
    val role: PlaybackSourceRole = PlaybackSourceRole.PRIMARY,
) {
    val headers: Map<String, String> =
        normalizePlaybackHeaders(headers)

    internal val requestIdentity: PlaybackRequestIdentity?
        get() = playbackRequestIdentity(url, headers)

    fun toCandidate(): PlaybackSourceCandidate? {
        val safeSourceId = sourceId ?: return null
        val safeStreamId = streamId?.takeIf { it.isNotBlank() }
            ?: return null

        if (safeSourceId <= 0L) return null

        val identity = requestIdentity ?: return null

        return PlaybackSourceCandidate(
            sourceId = safeSourceId,
            streamId = safeStreamId,
            url = identity.url,
            headers = identity.headers,
        )
    }

    override fun toString(): String =
        "PlaybackSource(role=$role, sourceId=$sourceId, " +
            "streamId=${streamId?.takeIf { it.isNotBlank() } ?: "null"}, " +
            "request=redacted)"
}
