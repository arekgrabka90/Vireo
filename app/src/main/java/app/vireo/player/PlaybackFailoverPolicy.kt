package app.vireo.player

import java.util.Locale

/**
 * Identity represents the actual HTTP playback request.
 *
 * It is intentionally redacted in diagnostics because URLs and headers
 * may contain credentials or temporary access tokens.
 */
internal data class PlaybackRequestIdentity(
    val url: String,
    val headers: Map<String, String>,
) {
    override fun toString(): String = "PlaybackRequestIdentity(redacted)"
}

internal data class PlaybackSourceCandidate(
    val sourceId: Long,
    val streamId: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

/**
 * HTTP header validation based on the HTTP token character set.
 */
private fun isSafeHeader(
    name: String,
    value: String,
): Boolean {
    if (name.isBlank() || value.isBlank()) return false

    val separators = "()<>@,;:\\\"/[]?={} \t"

    val validName = name.all { char ->
        char.code in 33..126 && char !in separators
    }

    val validValue = value.all { char ->
        char == '\t' || char.code in 32..126
    }

    return validName && validValue
}

internal fun normalizePlaybackHeaders(
    headers: Map<String, String>,
): Map<String, String> =
    headers.entries
        .filter { (name, value) ->
            isSafeHeader(name.trim(), value.trim())
        }
        .associate { (name, value) ->
            name.trim().lowercase(Locale.ROOT) to value.trim()
        }
        .toSortedMap()

internal fun playbackRequestIdentity(
    url: String?,
    headers: Map<String, String> = emptyMap(),
): PlaybackRequestIdentity? {
    val normalizedUrl = url
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return PlaybackRequestIdentity(
        url = normalizedUrl,
        headers = normalizePlaybackHeaders(headers),
    )
}

/**
 * Selects the first valid candidate that:
 * - has a valid source and stream identifier,
 * - has a usable playback URL,
 * - is not the currently playing request,
 * - has not already been attempted,
 * - does not duplicate another request identity.
 */
internal fun nextFailoverCandidate(
    candidates: List<PlaybackSourceCandidate>,
    current: PlaybackRequestIdentity?,
    attempted: Set<PlaybackRequestIdentity>,
): PlaybackSourceCandidate? {
    val seen = mutableSetOf<PlaybackRequestIdentity>()

    for (candidate in candidates) {
        if (candidate.sourceId <= 0L) continue
        if (candidate.streamId.isBlank()) continue

        val identity = playbackRequestIdentity(
            url = candidate.url,
            headers = candidate.headers,
        ) ?: continue

        if (!seen.add(identity)) continue
        if (identity == current) continue
        if (identity in attempted) continue

        return candidate
    }

    return null
}
