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
}
