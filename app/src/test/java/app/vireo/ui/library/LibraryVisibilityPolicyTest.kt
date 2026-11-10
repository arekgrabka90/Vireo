package app.vireo.ui.library

import app.vireo.data.local.LibraryItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryVisibilityPolicyTest {

    private fun item(
        key: String,
        watched: Boolean = false,
        watchlisted: Boolean = true,
        positionMs: Long = 0L,
    ) = LibraryItemEntity(
        profileId = 1L,
        contentKey = key,
        title = key,
        contentType = "MOVIE",
        posterUrl = null,
        backdropUrl = null,
        positionMs = positionMs,
        durationMs = 100_000L,
        watchlisted = watchlisted,
        watched = watched,
        updatedAt = 1L,
    )

    @Test
    fun `showWatched true keeps watched and unwatched My List entries`() {
        val watched = item("tmdb:MOVIE:1", watched = true)
        val unwatched = item("tmdb:MOVIE:2")

        val result = applyLibraryVisibilityPolicy(
            watchlist = listOf(watched, unwatched),
            continueWatching = emptyList(),
            allItems = listOf(watched, unwatched),
            showWatched = true,
            continueWatchingEnabled = true,
        )

        assertEquals(listOf(watched, unwatched), result.watchlist)
    }

    @Test
    fun `showWatched false filters only watched My List entries`() {
        val watched = item("tmdb:MOVIE:1", watched = true)
        val unwatched = item("tmdb:MOVIE:2")

        val result = applyLibraryVisibilityPolicy(
            watchlist = listOf(watched, unwatched),
            continueWatching = emptyList(),
            allItems = listOf(watched, unwatched),
            showWatched = false,
            continueWatchingEnabled = true,
        )

        assertEquals(listOf(unwatched), result.watchlist)
        assertEquals(setOf(watched.contentKey), result.watchedContentKeys)
    }

    @Test
    fun `continueWatching enabled exposes progress entries`() {
        val inProgress = item("tmdb:MOVIE:3", positionMs = 20_000L)

        val result = applyLibraryVisibilityPolicy(
            watchlist = emptyList(),
            continueWatching = listOf(inProgress),
            allItems = listOf(inProgress),
            showWatched = true,
            continueWatchingEnabled = true,
        )

        assertEquals(listOf(inProgress), result.continueWatching)
    }

    @Test
    fun `continueWatching disabled hides entries without mutating source list`() {
        val inProgress = item("tmdb:MOVIE:4", positionMs = 30_000L)
        val source = listOf(inProgress)

        val result = applyLibraryVisibilityPolicy(
            watchlist = emptyList(),
            continueWatching = source,
            allItems = source,
            showWatched = true,
            continueWatchingEnabled = false,
        )

        assertTrue(result.continueWatching.isEmpty())
        assertEquals(1, source.size)
        assertEquals(30_000L, source.first().positionMs)
    }

    @Test
    fun `watched markers remain available independently of My List filter`() {
        val watched = item(
            "tmdb:MOVIE:5",
            watched = true,
            watchlisted = false,
        )

        val result = applyLibraryVisibilityPolicy(
            watchlist = emptyList(),
            continueWatching = emptyList(),
            allItems = listOf(watched),
            showWatched = false,
            continueWatchingEnabled = false,
        )

        assertEquals(setOf(watched.contentKey), result.watchedContentKeys)
    }
}
