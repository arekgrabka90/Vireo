package app.vireo.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.ui.components.AvatarColors
import app.vireo.ui.components.ProfileAvatar
import app.vireo.ui.components.Wordmark

@Composable
fun ProfileEditScreen(
    onDone: () -> Unit,
    vm: ProfileEditViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val titleRes = when {
        state.isFirst -> R.string.welcome_title
        state.isNew -> R.string.new_profile
        else -> R.string.edit_profile
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark()
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
        )
        if (state.isFirst) {
            Text(
                stringResource(R.string.welcome_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
        ProfileAvatar(state.name.ifBlank { "?" }, state.color, 112.dp)
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::onName,
                label = { Text(stringResource(R.string.profile_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(AvatarColors) { i, c ->
                    Swatch(color = c, selected = i == state.color, onClick = { vm.onColor(i) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.kids_profile),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = state.kids, onCheckedChange = vm::onKids)
            }
            OutlinedTextField(
                value = state.pin,
                onValueChange = vm::onPin,
                label = { Text(stringResource(R.string.pin_optional)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { vm.save(onDone) }, enabled = state.name.isNotBlank()) {
                    Text(stringResource(R.string.save))
                }
                if (!state.isFirst) {
                    TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) }
                }
                if (state.canDelete) {
                    TextButton(onClick = { vm.delete(onDone) }) {
                        Text(stringResource(R.string.delete), color = colors.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(40.dp)
            .onFocusChanged { focused = it.hasFocus }
            .clip(CircleShape)
            .background(color)
            .border(3.dp, if (selected || focused) Color.White else Color.Transparent, CircleShape)
            .clickable(onClick = onClick),
    )
}
