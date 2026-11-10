package app.vireo.data.xtream

internal fun isXtreamCatalogFresh(
    lastSuccessfulSyncAt: Long?,
    now: Long,
    ttlMs: Long,
): Boolean {
    if (lastSuccessfulSyncAt == null || ttlMs <= 0L) return false
    if (lastSuccessfulSyncAt > now) return false
    return now - lastSuccessfulSyncAt < ttlMs
}

internal fun shouldReplaceXtreamCatalog(
    previousCategoryCount: Int,
    previousItemCount: Int,
    incomingCategoryCount: Int,
    incomingItemCount: Int,
): Boolean {
    if (previousCategoryCount > 0 && incomingCategoryCount == 0) return false
    if (previousItemCount > 0 && incomingItemCount == 0) return false
    return true
}
