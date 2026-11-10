package app.vireo.data.xtream

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamCatalogSyncPolicyTest {

    @Test
    fun `missing timestamp is stale`() {
        assertFalse(isXtreamCatalogFresh(null, 10_000L, 6_000L))
    }

    @Test
    fun `timestamp inside TTL is fresh`() {
        assertTrue(isXtreamCatalogFresh(5_000L, 10_000L, 6_000L))
    }

    @Test
    fun `timestamp at TTL boundary is stale`() {
        assertFalse(isXtreamCatalogFresh(4_000L, 10_000L, 6_000L))
    }

    @Test
    fun `future timestamp is stale`() {
        assertFalse(isXtreamCatalogFresh(11_000L, 10_000L, 6_000L))
    }

    @Test
    fun `non positive TTL is stale`() {
        assertFalse(isXtreamCatalogFresh(9_000L, 10_000L, 0L))
    }

    @Test
    fun `empty first catalog can be stored`() {
        assertTrue(shouldReplaceXtreamCatalog(0, 0, 0, 0))
    }

    @Test
    fun `empty response cannot erase existing items`() {
        assertFalse(shouldReplaceXtreamCatalog(5, 100, 5, 0))
    }

    @Test
    fun `empty categories cannot erase existing categories`() {
        assertFalse(shouldReplaceXtreamCatalog(5, 100, 0, 100))
    }

    @Test
    fun `non empty response replaces existing catalog`() {
        assertTrue(shouldReplaceXtreamCatalog(5, 100, 6, 110))
    }
}
