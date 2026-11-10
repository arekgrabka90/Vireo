package app.vireo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EpgCachePolicyTest {

    @Test
    fun `default interval preserves legacy fifteen minute ttl`() {
        assertEquals(
            EpgCachePolicy.DEFAULT_REFRESH_TTL_MS,
            EpgCachePolicy.refreshTtlMs(15),
        )
    }

    @Test
    fun `profile interval is converted to milliseconds`() {
        assertEquals(
            6L * 60L * 60L * 1000L,
            EpgCachePolicy.refreshTtlMs(360),
        )
    }

    @Test
    fun `interval below minimum is clamped`() {
        assertEquals(
            15L * 60L * 1000L,
            EpgCachePolicy.refreshTtlMs(1),
        )
    }

    @Test
    fun `interval above maximum is clamped`() {
        assertEquals(
            24L * 60L * 60L * 1000L,
            EpgCachePolicy.refreshTtlMs(2000),
        )
    }
}
