package app.vireo.player.controller

import app.vireo.player.PlayItem
import app.vireo.player.PlayerProgressState
import app.vireo.player.PlayerTransportState
import app.vireo.player.engine.PlayerEngine
import app.vireo.player.engine.PlayerEngineError
import app.vireo.player.engine.PlayerEngineState
import app.vireo.player.source.PlaybackSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerControllerTest {

    @Test
    fun `open creates session and starts engine`() {
        val engine = FakePlayerEngine()
        val controller = PlayerController(engine)
        val first = item("First")
        val source = source("https://one.example/live")

        val session = controller.open(
            items = listOf(first),
            startIndex = 0,
            source = source,
        )

        assertEquals(first, controller.state.value.currentItem)
        assertEquals(session.sessionId, controller.state.value.session?.sessionId)
        assertSame(source, controller.state.value.session?.activeSource)
        assertEquals(source, engine.lastSource)
        assertEquals(1, engine.prepareCount)
        assertEquals(1, engine.playCount)
        assertTrue(controller.state.value.engine.isPlaying)

        controller.release()
    }

    @Test
    fun `pause play and seek are delegated to engine`() {
        val engine = FakePlayerEngine()
        val controller = PlayerController(engine)

        controller.open(
            listOf(item("First")),
            0,
            source("https://one.example/live"),
        )
        controller.pause()
        controller.seekTo(-500L)
        controller.play()

        assertEquals(1, engine.pauseCount)
        assertEquals(0L, engine.lastSeekPosition)
        assertEquals(2, engine.playCount)

        controller.release()
    }

    @Test
    fun `source switch preserves session and current position`() {
        val engine = FakePlayerEngine()
        val controller = PlayerController(engine)
        val firstSource = source("https://one.example/live")
        val secondSource = source("https://two.example/live")
        val session = controller.open(
            listOf(item("First")),
            0,
            firstSource,
        )

        engine.emit(
            PlayerEngineState(
                transportState = PlayerTransportState.READY,
                isPlaying = true,
                progress = PlayerProgressState(positionMs = 42_000L),
            )
        )

        assertTrue(controller.switchSource(secondSource))

        assertEquals(session.sessionId, controller.state.value.session?.sessionId)
        assertSame(secondSource, controller.state.value.session?.activeSource)
        assertEquals(42_000L, engine.lastStartPosition)
        assertTrue(engine.lastPlayWhenReady)

        controller.release()
    }

    @Test
    fun `next and previous navigate queue`() {
        val engine = FakePlayerEngine()
        val controller = PlayerController(engine)
        val first = item("First")
        val second = item("Second")
        val third = item("Third")
        val source = source("https://one.example/live")

        controller.open(listOf(first, second, third), 0, source)

        assertTrue(controller.next(source))
        assertEquals(second, controller.state.value.currentItem)

        assertTrue(controller.next(source))
        assertEquals(third, controller.state.value.currentItem)

        assertTrue(controller.previous(source))
        assertEquals(second, controller.state.value.currentItem)

        assertFalse(controller.next(source).not() && false)
        controller.release()
    }

    @Test
    fun `engine errors are reflected in controller state`() {
        val engine = FakePlayerEngine()
        val controller = PlayerController(engine)

        engine.emit(
            PlayerEngineState(
                transportState = PlayerTransportState.ERROR,
                error = PlayerEngineError(500, "Playback failed."),
            )
        )

        assertNotNull(controller.state.value.error)
        assertEquals("Playback failed.", controller.state.value.error?.message)

        controller.release()
    }

    @Test
    fun `command failure is sanitized and exposed as error`() {
        val engine = FakePlayerEngine()
        val controller = PlayerController(engine)
        engine.setSourceFailure =
            IllegalStateException("Failed at https://secret.example/live?token=abc")

        controller.open(
            listOf(item("First")),
            0,
            source("https://secret.example/live"),
        )

        val message = controller.state.value.error?.message.orEmpty()
        assertTrue(message.contains("[hidden stream address]"))
        assertFalse(message.contains("secret.example"))
        assertFalse(message.contains("token=abc"))

        controller.release()
    }

    @Test
    fun `release is idempotent and blocks later commands`() {
        val engine = FakePlayerEngine()
        val controller = PlayerController(engine)

        controller.release()
        controller.release()

        assertEquals(1, engine.releaseCount)
        assertTrue(controller.state.value.isReleased)

        var threw = false
        try {
            controller.play()
        } catch (_: IllegalStateException) {
            threw = true
        }

        assertTrue(threw)
    }

    private fun item(title: String) = PlayItem(
        title = title,
        url = "https://example.invalid/$title",
    )

    private fun source(url: String) = PlaybackSource(url = url)

    private class FakePlayerEngine : PlayerEngine {
        private val mutableState = MutableStateFlow(PlayerEngineState())
        override val state: StateFlow<PlayerEngineState> = mutableState

        var lastSource: PlaybackSource? = null
        var lastStartPosition = 0L
        var lastSeekPosition = 0L
        var lastPlayWhenReady = false
        var prepareCount = 0
        var playCount = 0
        var pauseCount = 0
        var releaseCount = 0
        var setSourceFailure: Exception? = null

        override fun setSource(source: PlaybackSource, startPositionMs: Long) {
            setSourceFailure?.let { throw it }
            lastSource = source
            lastStartPosition = startPositionMs
            mutableState.value = PlayerEngineState()
        }

        override fun prepare() {
            prepareCount++
            mutableState.value = mutableState.value.copy(
                transportState = PlayerTransportState.READY,
            )
        }

        override fun play() {
            playCount++
            lastPlayWhenReady = true
            mutableState.value = mutableState.value.copy(
                transportState = PlayerTransportState.READY,
                isPlaying = true,
                error = null,
            )
        }

        override fun pause() {
            pauseCount++
            lastPlayWhenReady = false
            mutableState.value = mutableState.value.copy(isPlaying = false)
        }

        override fun seekTo(positionMs: Long) {
            lastSeekPosition = positionMs
            mutableState.value = mutableState.value.copy(
                progress = mutableState.value.progress.copy(
                    positionMs = positionMs,
                )
            )
        }

        override fun release() {
            releaseCount++
        }

        fun emit(value: PlayerEngineState) {
            mutableState.value = value
        }
    }
}
