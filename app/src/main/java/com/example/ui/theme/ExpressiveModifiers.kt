package com.example.ui.theme

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spring Scale modifier in Gemini / Material Expressive style:
 * Compresses the element to [pressedScale] (default 0.97f) on touch down
 * and rebounds with spring physics on release, optionally triggering native haptic feedback.
 */
fun Modifier.springScaleClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    hapticEnabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "spring_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    isPressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    isPressed = false
                }
            }
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = ripple(),
            role = Role.Button,
            onClick = {
                if (hapticEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                onClick()
            }
        )
}

/**
 * Gemini Ambient Card styling with 26.dp rounded corners, soft diffused ambient glow,
 * and a subtle gradient border when active.
 */
@Composable
fun Modifier.geminiAmbientCard(
    cornerRadius: Dp = 26.dp,
    isActive: Boolean = false,
    glowColor: Color = MaterialTheme.colorScheme.primary
): Modifier {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(cornerRadius)

    val borderBrush = if (isActive) {
        Brush.linearGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.55f),
                AmbientGlowViolet.copy(alpha = 0.35f),
                AmbientGlowCyan.copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                outlineColor.copy(alpha = 0.65f),
                outlineColor.copy(alpha = 0.30f)
            )
        )
    }

    return this
        .drawBehind {
            // Soft diffused ambient halo (simulating blurRadius: 30, opacity: 0.05-0.09)
            val haloAlpha = if (isActive) 0.10f else 0.05f
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = haloAlpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.55f),
                    radius = size.maxDimension * 0.72f
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx() + 8.dp.toPx())
            )
        }
        .clip(shape)
        .background(surfaceColor, shape)
        .border(
            width = if (isActive) 1.5.dp else 1.dp,
            brush = borderBrush,
            shape = shape
        )
}
