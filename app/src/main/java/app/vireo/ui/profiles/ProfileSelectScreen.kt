package app.vireo.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.data.local.ProfileEntity
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.ProfileAvatar
import app.vireo.ui.components.Wordmark

@Composable
fun ProfileSelectScreen(
    onSelected: () -> Unit,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    vm: ProfilesViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var pinTarget by remember { mutableStateOf<ProfileEntity?>(null) }

    LaunchedEffect(state.loaded, state.profiles.isEmpty()) {
        if (state.loaded && state.profiles.isEmpty()) onAdd()
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Wordmark()
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.whos_watching),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(32.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            ) {
                items(state.profiles, key = { it.id }) { p ->
                    ProfileTile(
                        profile = p,
                        manage = state.manage,
                        onClick = {
                            if (state.manage) {
                                onEdit(p.id)
                            } else if (!p.pin.isNullOrBlank()) {
                                pinTarget = p
                            } else {
                                vm.select(p, onSelected)
                            }
                        },
                    )
                }
                if (state.profiles.size < 5) {
                    item { AddTile(onClick = onAdd) }
                }
            }
            if (state.selectionError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nie udało się przygotować ustawień profilu. Spróbuj ponownie.",
                    color = colors.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (state.isSelecting) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Przygotowywanie profilu…",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(36.dp))
            FocusableBox(onClick = vm::toggleManage) {
                Text(
                    stringResource(if (state.manage) R.string.done else R.string.manage_profiles),
                    color = colors.onSurface,
                )
            }
        }
    }

    pinTarget?.let { target ->
        PinDialog(
            onDismiss = { pinTarget = null },
            onVerify = { entered, result ->
                vm.verifyPin(target, entered) { valid ->
                    if (valid) {
                        vm.select(target) {
                            pinTarget = null
                            onSelected()
                        }
                    } else {
                        result(false)
                    }
                }
            },
        )
    }
}

@Composable
private fun ProfileTile(profile: ProfileEntity, manage: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .onFocusChanged { focused = it.hasFocus }
                .clip(shape)
                .border(3.dp, if (focused) Color.White else Color.Transparent, shape)
                .clickable(onClick = onClick),
        ) {
            ProfileAvatar(profile.name, profile.color, 112.dp)
            if (manage) {
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✎", fontSize = 36.sp, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            profile.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (focused) colors.onBackground else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (profile.kids) {
            Text(
                stringResource(R.string.kids_badge),
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .onFocusChanged { focused = it.hasFocus }
                .clip(shape)
                .background(colors.surfaceVariant)
                .border(3.dp, if (focused) Color.White else Color.Transparent, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", fontSize = 44.sp, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.add_profile),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PinDialog(
    onDismiss: () -> Unit,
    onVerify: (String, (Boolean) -> Unit) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enter_pin)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { v ->
                        input = v.filter { ch -> ch.isDigit() }.take(4)
                        wrong = false
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                if (wrong) {
                    Text(stringResource(R.string.wrong_pin), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onVerify(input) { valid -> wrong = !valid }
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
