package app.vireo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import coil.compose.AsyncImage

@Composable
fun PosterCard(
    title: Title,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val colors = MaterialTheme.colorScheme
    val live = title.type == ContentType.LIVE

    Column(modifier = modifier.width(if (live) 164.dp else 132.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (live) 16f / 9f else 2f / 3f)
                .onFocusChanged { focused = it.hasFocus }
                .clip(shape)
                .background(colors.surfaceVariant)
                .border(2.dp, if (focused) colors.primary else Color.Transparent, shape)
                .clickable(onClick = onClick),
        ) {
            if (!title.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = title.posterUrl,
                    contentDescription = title.name,
                    contentScale = if (live) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(if (live) 14.dp else 0.dp),
                )
            } else {
                Text(
                    title.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title.name,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val rating = title.rating?.takeIf { it.isNotBlank() && it != "0" }
        if (rating != null) {
            Text(
                "★ $rating",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
