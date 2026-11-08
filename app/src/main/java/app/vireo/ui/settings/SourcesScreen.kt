package app.vireo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.ScreenTitle

@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    vm: SourcesViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val active = state.active

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            FocusableBox(onClick = onBack) {
                Text(stringResource(R.string.back), color = colors.onSurface)
            }
        }
        item { ScreenTitle(stringResource(R.string.set_sources)) }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceVariant)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.src_active),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                if (active == null) {
                    Text(stringResource(R.string.src_none), color = colors.onSurface)
                } else {
                    Text(active.server, color = colors.onSurface)
                    Text(active.username, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    FocusableBox(onClick = vm::remove) {
                        Text(stringResource(R.string.src_remove), color = colors.error)
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.src_add),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onBackground,
            )
        }
        item {
            OutlinedTextField(
                value = state.server,
                onValueChange = vm::onServer,
                label = { Text(stringResource(R.string.server)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.username,
                onValueChange = vm::onUsername,
                label = { Text(stringResource(R.string.username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.password,
                onValueChange = vm::onPassword,
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.error?.let { err ->
            item { Text(err, color = colors.error, style = MaterialTheme.typography.bodySmall) }
        }
        if (state.connected) {
            item { Text(stringResource(R.string.src_connected), color = colors.primary) }
        }
        item {
            Button(
                onClick = vm::save,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.src_save))
                }
            }
        }
    }
}
