package app.vireo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

val AvatarColors = listOf(
    Color(0xFF7C5CFF), Color(0xFFE5484D), Color(0xFF30A46C), Color(0xFFF76B15),
    Color(0xFF0091FF), Color(0xFFD6409F), Color(0xFF12A594), Color(0xFFFFB224),
)

@Composable
fun ProfileAvatar(name: String, colorIndex: Int, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 6))
            .background(AvatarColors[colorIndex.mod(AvatarColors.size)]),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            color = Color.White,
            fontSize = (size.value * 0.45f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun Wordmark(modifier: Modifier = Modifier, size: Int = 28) {
    Text(
        "VIREO",
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        fontSize = size.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 6.sp,
    )
}
