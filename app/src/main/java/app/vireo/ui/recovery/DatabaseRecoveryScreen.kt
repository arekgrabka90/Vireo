package app.vireo.ui.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vireo.R
import app.vireo.ui.components.FocusableBox
import kotlinx.coroutines.delay

@Composable
fun BusyScreen(message: String?) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        if (message != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text(message, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
fun RestartScreen(message: String, onRestart: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2_000L)
        onRestart()
    }
    ScreenColumn {
        Text(message, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Aplikacja uruchomi się ponownie. Jeśli tak się nie stanie, otwórz ją ręcznie.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ActionButton("Uruchom ponownie", onRestart)
    }
}

@Composable
fun DatabaseRecoveryScreen(
    state: StartupState.Recovery,
    notice: String?,
    onRetry: () -> Unit,
    onContinueWithoutBackup: () -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onMoveAside: () -> Unit,
) {
    var confirmMoveAside by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    ScreenColumn {
        Text(
            "Problem z bazą danych",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onBackground,
        )
        Text(describe(state.kind), color = colors.onBackground)
        if (state.detail.isNotBlank()) {
            Text(
                state.detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        if (notice != null) {
            Text(notice, color = colors.primary)
        }
        Spacer(Modifier.height(8.dp))
        ActionButton("Spróbuj ponownie", onRetry)
        if (state.kind == RecoveryKind.BACKUP_FAILED) {
            ActionButton("Kontynuuj bez kopii (na własne ryzyko)", onContinueWithoutBackup)
        }
        if (state.hasDatabase) {
            ActionButton("Eksportuj kopię bazy", onExport)
        }
        val backup = state.latestBackup
        if (backup != null) {
            ActionButton(
                "Przywróć ostatnią kopię (v${backup.version}, ${backup.timestamp} UTC)",
                onRestore,
            )
        }
        if (state.hasDatabase) {
            ActionButton("Odsuń bazę i zacznij od nowa") { confirmMoveAside = true }
        }
    }

    if (confirmMoveAside) {
        AlertDialog(
            onDismissRequest = { confirmMoveAside = false },
            title = { Text("Odsunąć bazę i zacząć od nowa?") },
            text = {
                Text(
                    "Obecne pliki bazy zostaną przeniesione do osobnego katalogu aplikacji " +
                        "(nie zostaną usunięte), a po ponownym uruchomieniu aplikacja utworzy " +
                        "nową, pustą bazę. Profile, biblioteka i źródła nie będą widoczne, " +
                        "dopóki nie przywrócisz kopii.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmMoveAside = false
                    onMoveAside()
                }) { Text("Odsuń bazę") }
            },
            dismissButton = {
                TextButton(onClick = { confirmMoveAside = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ScreenColumn(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    FocusableBox(onClick = onClick) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun describe(kind: RecoveryKind): String = when (kind) {
    RecoveryKind.UNSUPPORTED_OLD ->
        "Baza pochodzi ze starszej wersji aplikacji, która nie jest już obsługiwana. Dane nie zostały zmienione."
    RecoveryKind.NEWER_THAN_APP ->
        "Baza pochodzi z nowszej wersji aplikacji. Zainstaluj nowszą wersję albo odsuń bazę i zacznij od nowa. Dane nie zostały zmienione."
    RecoveryKind.CORRUPT ->
        "Plik bazy wygląda na uszkodzony. Dane nie zostały zmienione ani usunięte."
    RecoveryKind.UNREADABLE ->
        "Nie można odczytać pliku bazy. Dane nie zostały zmienione."
    RecoveryKind.MIGRATION_FAILED ->
        "Aktualizacja bazy nie powiodła się. Baza nie została zresetowana."
    RecoveryKind.MISSING_MIGRATION ->
        "Brakuje ścieżki aktualizacji tej wersji bazy. Baza nie została zresetowana."
    RecoveryKind.BACKUP_FAILED ->
        "Nie udało się utworzyć kopii przed aktualizacją bazy. Możesz spróbować ponownie albo kontynuować bez kopii."
    RecoveryKind.OPEN_FAILED ->
        "Nie udało się otworzyć bazy. Dane nie zostały zmienione."
}
