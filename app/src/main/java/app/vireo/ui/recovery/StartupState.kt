package app.vireo.ui.recovery

import app.vireo.data.recovery.BackupInfo

enum class RecoveryKind {
    UNSUPPORTED_OLD,
    NEWER_THAN_APP,
    CORRUPT,
    UNREADABLE,
    MIGRATION_FAILED,
    MISSING_MIGRATION,
    BACKUP_FAILED,
    OPEN_FAILED,
    REPEATED_CRASHES,
}

sealed interface StartupState {
    data object Checking : StartupState

    data object Ready : StartupState

    data class Working(val message: String) : StartupState

    data class RestartRequired(val message: String) : StartupState

    data class Recovery(
        val kind: RecoveryKind,
        val detail: String,
        val hasDatabase: Boolean,
        val latestBackup: BackupInfo?,
    ) : StartupState
}
