package app.vireo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vireo.R

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/** Pusty ekran-zaślepka dla sekcji, które dopiero zbudujemy. */
@Composable
fun PlaceholderScreen(
    title: Int,
    message: Int,
    emoji: String,
    onBack: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(32.dp),
    ) {
        if (onBack != null) {
            FocusableBox(onClick = onBack) {
                Text(stringResource(R.string.back), color = colors.onSurface)
            }
            Spacer(Modifier.height(16.dp))
        }
        ScreenTitle(stringResource(title))
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(emoji, fontSize = 48.sp)
                Text(
                    stringResource(R.string.coming_soon),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBackground,
                )
                Text(
                    stringResource(message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
