package com.rubex.glassical

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubex.glassical.ui.theme.RealmeOrange
import kotlinx.coroutines.launch

@Composable
fun RealmeButton(
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAction: Boolean = false, // Top row (AC, %, etc)
    isAccent: Boolean = false, // Equals button
    isOperation: Boolean = false, // Right column (+, -, etc)
    isWide: Boolean = false // 00 button
) {
    // Press state management
    var isPressed by remember { mutableStateOf(false) }

    // Animation State
    val scale = remember { Animatable(1f) }
    val overlayAlpha = remember { Animatable(0f) }
    
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    
    // Shape... (lines 69-70 unused in replacement chunk but needed for context if inside)
    val shape = if (isWide) RoundedCornerShape(36.dp) else CircleShape
    // ... colors logic ...

    // Animation Effects
    LaunchedEffect(isPressed) {
        if (isPressed) {
             launch { 
                scale.animateTo(0.92f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) 
             }
             launch { overlayAlpha.animateTo(0.1f, spring()) }
             // Haptic
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else {
             launch { 
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) 
             }
             launch { overlayAlpha.animateTo(0f, spring()) }
        }
    }

    // Colors Logic
    val isDark = isSystemInDarkTheme() 
    
    // Base Colors
    val baseColor = when {
        isAccent -> RealmeOrange
        isAction -> if (isDark) Color(0xFFA5A5A5).copy(alpha = 0.15f) else Color(0xFFD1D1D6).copy(alpha = 0.6f) 
        isOperation -> if (isDark) Color(0xFF404040).copy(alpha = 0.3f) else Color(0xFF000000).copy(alpha = 0.05f) 
        else -> if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.25f) else Color(0xFFFFFFFF) 
    }

    val contentColor = when {
        isAccent -> Color.White
        isAction -> RealmeOrange 
        else -> MaterialTheme.colorScheme.onSecondary 
    }

    // Border
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.05f) 
    }



    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale.value)
            .aspectRatio(if (isWide) 2f else 1f, matchHeightConstraintsFirst = false)
            .clip(shape)
            .background(baseColor)
            .border(1.dp, borderColor, shape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            if (change.pressed) {
                                if (!isPressed) isPressed = true
                            } else {
                                if (isPressed) {
                                    isPressed = false
                                    // Only trigger click if released INSIDE bounds? 
                                    // Simple calc button: trigger on release usually, or just trigger.
                                    // Let's rely on standard clickable logic? No, we replaced it.
                                    // Actually, standard clickable is better for accessibility. 
                                    // The STUCK issue with interactionSource usually happens if collection is cancelled.
                                    // Robust fix: Use tapGestureFilter or similar?
                                    // Or just ensure onDispose resets it.
                                }
                            }
                        }
                    }
                }
            }
            // Add clickable for accessibility and standard behavior, but disable its ripple
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            // Monitor press separately for animation to separate it from the click logic
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        val upOrCancel = waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }          
    ) {


        // Inner Glow / Blur Effect (Simulated via overlay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = overlayAlpha.value))
        )
        
        Text(
            text = symbol,
            fontSize = if (isWide) 28.sp else 32.sp,
            fontWeight = FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // A surface component providing the backdrop for glass elements if we wanted a "card" look.
    // For this specific design, the entire screen is the background, and buttons are individual glass shards.
    // We can use this for the Display area if it needs a distinct background.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.02f)) // Ultra subtle
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        content()
    }
}
