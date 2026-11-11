package app.vireo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Element klikalny z wyraźnym focusem (pilot TV + dotyk). */
@Composable
fun FocusableBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            focused || hovered -> 1.025f
            pressed -> 0.985f
            else -> 1f
        },
        label = "control-focus-scale",
    )
    val shape = RoundedCornerShape(12.dp)
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hoverable(interactionSource)
            .onFocusChanged { focused = it.hasFocus; onFocusChange?.invoke(it.hasFocus) }
            .clip(shape)
            .background(
                if (selected || focused) colors.primary.copy(alpha = 0.16f)
                else colors.surfaceVariant.copy(alpha = 0.78f),
            )
            .border(
                width = if (focused || hovered || selected) 1.5.dp else 0.dp,
                color = if (focused || selected) colors.primary
                    else if (hovered) colors.primary.copy(alpha = 0.55f)
                    else Color.Transparent,
                shape = shape,
            )
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 15.dp, vertical = 10.dp),
        content = content,
    )
}
