package app.vireo.ui.recovery

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Bramka startowa: treść aplikacji (a więc i dostęp do bazy) pojawia się
 * dopiero po pomyślnej kontroli bazy.
 */
@Composable
fun StartupGate(
    onRestart: () -> Unit,
    vm: StartupViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) vm.exportToUri(uri)
    }

    when (val current = state) {
        StartupState.Ready -> content()
        StartupState.Checking -> BusyScreen(null)
        is StartupState.Working -> BusyScreen(current.message)
        is StartupState.RestartRequired -> RestartScreen(current.message, onRestart)
        is StartupState.Recovery -> DatabaseRecoveryScreen(
            state = current,
            notice = notice,
            onRetry = vm::recheck,
            onContinueWithoutBackup = vm::continueWithoutBackup,
            onExport = {
                try {
                    exportLauncher.launch(vm.suggestedExportName())
                } catch (e: ActivityNotFoundException) {
                    vm.exportToAppFolder()
                }
            },
            onRestore = vm::restoreLatestBackup,
            onMoveAside = vm::moveDatabaseAside,
        )
    }
}
