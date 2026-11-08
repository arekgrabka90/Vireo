package app.vireo.data.xtream

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamIndexSyncPolicyTest {

    @Test
    fun `missing sync timestamp is stale`() {
        assertFalse(
            isXtreamIndexFresh(
                lastSuccessfulSyncAt = null,
                now = 10_000L,
                ttlMs = 6_000L,
            )
        )
    }

    @Test
    fun `index inside TTL is fresh`() {
        assertTrue(
            isXtreamIndexFresh(
                lastSuccessfulSyncAt = 5_000L,
                now = 10_000L,
                ttlMs = 6_000L,
            )
        )
    }

    @Test
    fun `index at TTL boundary is stale`() {
        assertFalse(
            isXtreamIndexFresh(
                lastSuccessfulSyncAt = 4_000L,
                now = 10_000L,
                ttlMs = 6_000L,
            )
        )
    }

    @Test
    fun `future timestamp is stale`() {
        assertFalse(
            isXtreamIndexFresh(
                lastSuccessfulSyncAt = 11_000L,
                now = 10_000L,
                ttlMs = 6_000L,
            )
        )
    }

    @Test
    fun `non positive TTL is stale`() {
        assertFalse(
            isXtreamIndexFresh(
                lastSuccessfulSyncAt = 9_000L,
                now = 10_000L,
                ttlMs = 0L,
            )
        )
    }
}
