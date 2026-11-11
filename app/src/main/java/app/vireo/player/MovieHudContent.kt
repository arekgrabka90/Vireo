package app.vireo.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun MovieHudContent(
    content: PlayerHudContent.Movie,
    wide: Boolean,
    isPlaying: Boolean,
) {
    val metadata = listOfNotNull(
        content.year?.takeIf { it.isNotBlank() },
        content.genre?.takeIf { it.isNotBlank() },
        content.duration?.takeIf { it.isNotBlank() },
        content.rating?.takeIf { it.isNotBlank() }?.let { "★ $it" },
    )

    Column(
        modifier = Modifier.fillMaxWidth(if (wide) 0.66f else 0.94f),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (metadata.isNotEmpty()) {
            Text(
                metadata.joinToString("   ·   "),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        MovieDescription(
            description = content.description,
            wide = wide,
            isPlaying = isPlaying,
        )
    }
}

@Composable
private fun MovieDescription(
    description: String?,
    wide: Boolean,
    isPlaying: Boolean,
) {
    val text = description?.trim()?.takeIf { it.isNotBlank() } ?: return
    var expanded by remember(text) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.86f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = when {
                isPlaying -> 1
                expanded -> 8
                wide -> 4
                else -> 3
            },
            overflow = TextOverflow.Ellipsis,
        )

        if (!isPlaying && text.length > 180) {
            Text(
                text = if (expanded) "Less" else "More",
                modifier = Modifier.clickable { expanded = !expanded },
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
