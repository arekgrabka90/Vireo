package app.vireo.player

import app.vireo.data.ContentRepository
import app.vireo.data.EpgRepository
import app.vireo.data.LibraryRepository
import app.vireo.data.ProfileRepository
import app.vireo.data.ProfileSettingsRepository
import app.vireo.data.local.ProfileEntity
import app.vireo.data.local.ProfileSettingsEntity
import app.vireo.player.engine.Media3PlayerEngine
import app.vireo.player.source.PlaybackSource
import androidx.media3.common.PlaybackException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelIntegrationTest {

    private lateinit var session: PlaybackSession
    private lateinit var engine: Media3PlayerEngine
    private lateinit var content: ContentRepository
    private lateinit var library: LibraryRepository
    private lateinit var profiles: ProfileRepository
    private lateinit var epg: EpgRepository
    private lateinit var settings: ProfileSettingsRepository

    private val profile = ProfileEntity(
        id = 1L,
        name = "Test",
        kids = false,
    )

    private val profileSettings = ProfileSettingsEntity(
        profileId = 1L,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        session = PlaybackSession()
        engine = Mockito.mock(Media3PlayerEngine::class.java)
        content = Mockito.mock(ContentRepository::class.java)
        library = Mockito.mock(LibraryRepository::class.java)
        profiles = Mockito.mock(ProfileRepository::class.java)
        epg = Mockito.mock(EpgRepository::class.java)
        settings = Mockito.mock(ProfileSettingsRepository::class.java)

        Mockito.`when`(profiles.activeId).thenReturn(1L)
        Mockito.`when`(settings.observe(1L)).thenReturn(
            MutableStateFlow(profileSettings)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun createViewModel(
        scope: TestScope,
    ): PlayerViewModel {
        Mockito.`when`(profiles.get(1L)).thenReturn(profile)
        Mockito.`when`(
            settings.ensure(
                profileId = 1L,
                kidsMode = false,
            )
        ).thenReturn(profileSettings)

        val viewModel = PlayerViewModel(
            session = session,
            media3PlayerEngine = engine,
            contentRepository = content,
            library = library,
            profiles = profiles,
            epgRepository = epg,
            profileSettingsRepository = settings,
            applicationScope = scope.backgroundScope,
        )

        scope.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `resolved source is prepared and playback starts`() = runTest {
        val item = PlayItem(
            title = "Test movie",
            url = "https://example.invalid/movie.mp4",
            contentKey = "movie:1",
            contentType = "MOVIE",
        )
        val source = PlaybackSource(
            url = "https://example.invalid/resolved.mp4",
        )

        session.start(listOf(item), 0)

        Mockito.`when`(
            content.resolvePlaybackSource(item)
        ).thenReturn(source)

        val viewModel = createViewModel(this)
        viewModel.startPlayback(item, 12_000L)

        advanceUntilIdle()

        Mockito.verify(engine).invalidateSource()
        Mockito.verify(engine).pause()
        Mockito.verify(engine).setSource(source, 12_000L)
        Mockito.verify(engine).prepare()
        Mockito.verify(engine).play()

        assertEquals(item, viewModel.state.value.item)
    }

    @Test
    fun `paused user intent is preserved during source resolution`() = runTest {
        val item = PlayItem(
            title = "Paused movie",
            url = "https://example.invalid/movie.mp4",
            contentType = "MOVIE",
        )
        val source = PlaybackSource(
            url = "https://example.invalid/resolved.mp4",
        )

        session.start(listOf(item), 0)

        Mockito.`when`(
            content.resolvePlaybackSource(item)
        ).thenReturn(source)

        val viewModel = createViewModel(this)

        viewModel.pausePlayback()
        viewModel.startPlayback(item, 0L)

        advanceUntilIdle()

        assertFalse(viewModel.state.value.desiredPlayWhenReady)

        Mockito.verify(engine).setSource(source, 0L)
        Mockito.verify(engine).prepare()
        Mockito.verify(engine, Mockito.never()).play()
    }

    @Test
    fun `next and previous update queue state and invalidate old source`() = runTest {
        val first = PlayItem(
            title = "First",
            url = "https://example.invalid/first.mp4",
            contentType = "MOVIE",
        )
        val second = PlayItem(
            title = "Second",
            url = "https://example.invalid/second.mp4",
            contentType = "MOVIE",
        )

        session.start(listOf(first, second), 0)

        val viewModel = createViewModel(this)

        viewModel.next(0L, 0L)
        advanceUntilIdle()

        assertEquals(second, viewModel.state.value.item)
        Mockito.verify(engine).invalidateSource()

        viewModel.previous(0L, 0L)
        advanceUntilIdle()

        assertEquals(first, viewModel.state.value.item)
        Mockito.verify(engine, Mockito.times(2)).invalidateSource()
    }

    private fun playbackException(
        code: Int,
        message: String,
    ): PlaybackException {
        val error = Mockito.mock(PlaybackException::class.java)
        Mockito.`when`(error.errorCode).thenReturn(code)
        Mockito.`when`(error.message).thenReturn(message)
        return error
    }

    @Test
    fun `retry budget falls back to the next source`() = runTest {
        val item = PlayItem(
            title = "Recovery movie",
            url = "https://primary.example/movie.mp4",
            contentKey = "movie:recovery",
            contentType = "MOVIE",
            sourceId = 1L,
            alternatives = listOf(
                PlaybackAlternative(
                    sourceId = 2L,
                    streamId = "alternative-2",
                    url = "https://secondary.example/movie.mp4",
                ),
            ),
        )
        val source = PlaybackSource(
            url = "https://resolved.example/movie.mp4",
        )

        session.start(listOf(item), 0)
        Mockito.`when`(
            content.resolvePlaybackSource(item)
        ).thenReturn(source)

        val viewModel = createViewModel(this)

        // Establish the initial source before testing its retry budget.
        viewModel.startPlayback(item, 0L)
        advanceUntilIdle()

        viewModel.onPlaybackError(
            playbackException(
                PlaybackException.ERROR_CODE_TIMEOUT,
                "Temporary network timeout",
            ),
            41_000L,
        )

        assertEquals(1, viewModel.state.value.retryToken)
        assertEquals(
            41_000L,
            requireNotNull(viewModel.state.value.resumePositionMs),
        )

        viewModel.startPlayback(item, 41_000L)
        advanceUntilIdle()

        viewModel.onPlaybackError(
            playbackException(
                PlaybackException.ERROR_CODE_TIMEOUT,
                "Temporary network timeout",
            ),
            42_000L,
        )

        assertEquals(2, viewModel.state.value.retryToken)
        assertEquals(
            42_000L,
            requireNotNull(viewModel.state.value.resumePositionMs),
        )

        viewModel.startPlayback(item, 42_000L)
        advanceUntilIdle()

        viewModel.onPlaybackError(
            playbackException(
                PlaybackException.ERROR_CODE_TIMEOUT,
                "Temporary network timeout",
            ),
            43_000L,
        )

        assertEquals(3, viewModel.state.value.retryToken)
        assertEquals(
            "https://secondary.example/movie.mp4",
            viewModel.state.value.item?.url,
        )
        assertEquals(
            43_000L,
            requireNotNull(viewModel.state.value.resumePositionMs),
        )
    }

    @Test
    fun `fatal playback error does not trigger automatic retry`() = runTest {
        val item = PlayItem(
            title = "Fatal movie",
            url = "https://example.invalid/fatal.mp4",
            contentType = "MOVIE",
        )

        session.start(listOf(item), 0)
        val viewModel = createViewModel(this)

        viewModel.onPlaybackError(
            playbackException(
                Int.MIN_VALUE,
                "Fatal playback failure",
            ),
            12_000L,
        )

        assertEquals(0, viewModel.state.value.retryToken)
        assertEquals(
            PlayerTransportState.ERROR,
            viewModel.state.value.runtime.transportState,
        )
        assertEquals(
            "Fatal playback failure",
            viewModel.state.value.error,
        )
    }

    @Test
    fun `failover exhaustion ends in a safe error state`() = runTest {
        val item = PlayItem(
            title = "No alternatives",
            url = "https://example.invalid/only.mp4",
            contentType = "MOVIE",
        )

        session.start(listOf(item), 0)
        val viewModel = createViewModel(this)

        viewModel.onPlaybackError(
            playbackException(
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                "Source unavailable",
            ),
            8_000L,
        )

        assertEquals(0, viewModel.state.value.retryToken)
        assertEquals(
            PlayerTransportState.ERROR,
            viewModel.state.value.runtime.transportState,
        )
        assertTrue(viewModel.state.value.runtime.hasError)
    }

    @Test
    fun `save progress persists valid VOD and skips live or invalid data`() = runTest {
        val movie = PlayItem(
            title = "Progress movie",
            url = "https://example.invalid/movie.mp4",
            contentKey = "movie:progress",
            contentType = "MOVIE",
        )
        val live = PlayItem(
            title = "Live channel",
            url = "https://example.invalid/live.m3u8",
            contentKey = "live:channel",
            contentType = "LIVE",
        )
        val blankKey = PlayItem(
            title = "Blank key",
            url = "https://example.invalid/blank.mp4",
            contentKey = " ",
            contentType = "MOVIE",
        )
        val noDuration = PlayItem(
            title = "No duration",
            url = "https://example.invalid/unknown.mp4",
            contentKey = "movie:no-duration",
            contentType = "MOVIE",
        )

        session.start(listOf(movie), 0)
        val viewModel = createViewModel(this)

        viewModel.saveProgress(movie, 12_000L, 90_000L)
        viewModel.saveProgress(live, 12_000L, 90_000L)
        viewModel.saveProgress(blankKey, 12_000L, 90_000L)
        viewModel.saveProgress(noDuration, 12_000L, 0L)

        advanceUntilIdle()

        Mockito.verify(library).saveProgress(
            1L,
            movie,
            12_000L,
            90_000L,
        )
        Mockito.verifyNoMoreInteractions(library)
    }

    @Test
    fun `next persists current VOD progress before changing queue item`() = runTest {
        val first = PlayItem(
            title = "First movie",
            url = "https://example.invalid/first.mp4",
            contentKey = "movie:first",
            contentType = "MOVIE",
        )
        val second = PlayItem(
            title = "Second movie",
            url = "https://example.invalid/second.mp4",
            contentKey = "movie:second",
            contentType = "MOVIE",
        )

        session.start(listOf(first, second), 0)
        val viewModel = createViewModel(this)

        viewModel.next(30_000L, 120_000L)
        advanceUntilIdle()

        assertEquals(second, viewModel.state.value.item)

        Mockito.verify(library).saveProgress(
            1L,
            first,
            30_000L,
            120_000L,
        )
    }

}
