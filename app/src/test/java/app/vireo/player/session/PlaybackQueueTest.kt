package app.vireo.player.session

import app.vireo.player.PlayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueTest {

    private fun item(title: String) = PlayItem(
        title = title,
        url = "https://example.invalid/$title.m3u8",
    )

    @Test
    fun `empty queue has no current item or navigation`() {
        val queue = PlaybackQueue.create(emptyList())

        assertTrue(queue.isEmpty)
        assertNull(queue.current)
        assertFalse(queue.hasNext)
        assertFalse(queue.hasPrevious)
        assertNull(queue.next())
        assertNull(queue.previous())
    }

    @Test
    fun `start index is safely normalized`() {
        val queue = PlaybackQueue.create(
            items = listOf(item("one"), item("two")),
            startIndex = 99,
        )

        assertEquals(1, queue.currentIndex)
        assertEquals("two", queue.current?.title)
    }

    @Test
    fun `next and previous return independent snapshots`() {
        val original = PlaybackQueue.create(
            listOf(item("one"), item("two"), item("three")),
        )

        val second = original.next()
        assertEquals(0, original.currentIndex)
        assertEquals(1, second?.currentIndex)
        assertEquals("two", second?.current?.title)

        val firstAgain = second?.previous()
        assertEquals(0, firstAgain?.currentIndex)
        assertEquals("one", firstAgain?.current?.title)
    }

    @Test
    fun `navigation does not move beyond queue boundaries`() {
        val queue = PlaybackQueue.create(
            listOf(item("one"), item("two")),
        )

        val last = queue.moveTo(1)

        assertNull(last?.next())
        assertNull(queue.previous())
        assertNull(queue.moveTo(-1))
        assertNull(queue.moveTo(2))
    }

    @Test
    fun `session states receive independent identifiers`() {
        val queue = PlaybackQueue.create(listOf(item("one")))

        val first = PlaybackSessionState.create(queue)
        val second = PlaybackSessionState.create(queue)

        assertTrue(first.sessionId.isNotBlank())
        assertTrue(second.sessionId.isNotBlank())
        assertTrue(first.sessionId != second.sessionId)
    }

    @Test
    fun `queue snapshots preserve the supplied item order`() {
        val source = mutableListOf(item("one"), item("two"))
        val queue = PlaybackQueue.create(source)

        source.clear()

        assertEquals(2, queue.size)
        assertEquals("one", queue.current?.title)
        assertEquals("two", queue.next()?.current?.title)
    }
}
