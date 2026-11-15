package app.vireo.data.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseClassifierTest {

    private class SQLiteDatabaseCorruptException(message: String) : RuntimeException(message)

    @Test
    fun absentProbeIsReady() {
        assertEquals(DatabaseHealth.Ready, DatabaseClassifier.classify(DatabaseProbe.Absent))
    }

    @Test
    fun currentVersionIsReady() {
        assertEquals(
            DatabaseHealth.Ready,
            DatabaseClassifier.classify(DatabaseProbe.Readable(16), current = 16, minSupported = 14),
        )
    }

    @Test
    fun olderSupportedVersionsNeedBackup() {
        assertEquals(
            DatabaseHealth.NeedsBackup(14),
            DatabaseClassifier.classify(DatabaseProbe.Readable(14), current = 16, minSupported = 14),
        )
        assertEquals(
            DatabaseHealth.NeedsBackup(15),
            DatabaseClassifier.classify(DatabaseProbe.Readable(15), current = 16, minSupported = 14),
        )
    }

    @Test
    fun versionsBelowMinimumAreUnsupported() {
        assertEquals(
            DatabaseHealth.UnsupportedOld(13),
            DatabaseClassifier.classify(DatabaseProbe.Readable(13), current = 16, minSupported = 14),
        )
        assertEquals(
            DatabaseHealth.UnsupportedOld(0),
            DatabaseClassifier.classify(DatabaseProbe.Readable(0), current = 16, minSupported = 14),
        )
    }

    @Test
    fun newerVersionIsNewerThanApp() {
        assertEquals(
            DatabaseHealth.NewerThanApp(17),
            DatabaseClassifier.classify(DatabaseProbe.Readable(17), current = 16, minSupported = 14),
        )
    }

    @Test
    fun corruptAndUnreadableProbesPassThrough() {
        assertEquals(
            DatabaseHealth.Corrupt("x"),
            DatabaseClassifier.classify(DatabaseProbe.Corrupt("x")),
        )
        assertEquals(
            DatabaseHealth.Unreadable("y"),
            DatabaseClassifier.classify(DatabaseProbe.Unreadable("y")),
        )
    }

    @Test
    fun defaultPolicyIsConsistent() {
        assertTrue(DatabasePolicy.MIN_SUPPORTED_VERSION <= DatabasePolicy.CURRENT_VERSION)
        assertEquals(
            DatabaseHealth.Ready,
            DatabaseClassifier.classify(DatabaseProbe.Readable(DatabasePolicy.CURRENT_VERSION)),
        )
    }

    @Test
    fun corruptExceptionInCauseChainIsCorrupt() {
        val error = IllegalStateException("wrapper", SQLiteDatabaseCorruptException("file is not a database"))
        assertTrue(DatabaseClassifier.classifyOpenFailure(error) is OpenFailure.Corrupt)
    }

    @Test
    fun missingMigrationMessageIsMissingMigration() {
        val error = IllegalStateException(
            "A migration from 13 to 14 was required but not found. Please provide the necessary Migration path."
        )
        assertTrue(DatabaseClassifier.classifyOpenFailure(error) is OpenFailure.MissingMigration)
    }

    @Test
    fun downgradeMessageIsNewerThanApp() {
        val error = RuntimeException("Can't downgrade database from version 15 to 14")
        assertTrue(DatabaseClassifier.classifyOpenFailure(error) is OpenFailure.NewerThanApp)
    }

    @Test
    fun migrationFailureMessageIsMigrationFailed() {
        val error = RuntimeException("Migration didn't properly handle: library_items")
        assertTrue(DatabaseClassifier.classifyOpenFailure(error) is OpenFailure.MigrationFailed)
    }

    @Test
    fun unknownErrorIsOther() {
        assertTrue(DatabaseClassifier.classifyOpenFailure(IllegalStateException("coś innego")) is OpenFailure.Other)
    }

    @Test
    fun detailIsSingleLineAndBounded() {
        val message = "linia1\nlinia2\t" + "x".repeat(1_000)
        val detail = DatabaseClassifier.classifyOpenFailure(RuntimeException(message)).detail
        assertFalse(detail.contains('\n'))
        assertFalse(detail.contains('\t'))
        assertTrue(detail.length <= 220)
    }
}
