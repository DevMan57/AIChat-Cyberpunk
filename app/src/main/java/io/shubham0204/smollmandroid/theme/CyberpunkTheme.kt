/*
 * Copyright (C) 2024 AI Chat Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cyberpunk Theme - Lightsaber Green & Purple Edition
 * Carbon fiber textured dark mode with neon accents
 */

// Lightsaber Colors
object LightsaberColors {
    // Primary - Green (User Messages)
    val green = Color(0xFF00FF00)  // Pure neon green
    val greenBright = Color(0xFF39FF14)  // Bright neon green
    val greenDark = Color(0xFF00CC00)
    
    // Secondary - Purple (AI/Character Messages)
    val purple = Color(0xFF9D00FF)  // Vivid purple
    val purpleBright = Color(0xFFBF40BF)  // Bright magenta-purple
    val purpleDark = Color(0xFF7A00CC)
    
    // Glow variants
    val greenGlow = Color(0xFF00FF00).copy(alpha = 0.6f)
    val purpleGlow = Color(0xFF9D00FF).copy(alpha = 0.6f)
}

// Carbon Fiber Background Colors
object CarbonFiberColors {
    val baseDark = Color(0xFF0A0A0A)
    val baseMedium = Color(0xFF0D0D0D)
    val baseLight = Color(0xFF1A1A1A)
    val weaveLight = Color(0xFF2A2A2A)
    val weaveDark = Color(0xFF151515)
}

// Cyberpunk Color Palette (Updated)
object CyberpunkColors {
    // Backgrounds - Carbon Fiber themed
    val backgroundPrimary = CarbonFiberColors.baseDark
    val backgroundSecondary = CarbonFiberColors.baseMedium
    val backgroundElevated = CarbonFiberColors.baseLight
    val backgroundCard = Color(0xFF141414)
    
    // Lightsaber Accents
    val lightsaberGreen = LightsaberColors.green
    val lightsaberPurple = LightsaberColors.purple
    val lightsaberGreenBright = LightsaberColors.greenBright
    val lightsaberPurpleBright = LightsaberColors.purpleBright
    
    // Glow effects
    val greenGlow = LightsaberColors.greenGlow
    val purpleGlow = LightsaberColors.purpleGlow
    
    // Text Colors
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB8B8B8)
    val textMuted = Color(0xFF808080)
    val textGreen = LightsaberColors.green
    val textPurple = LightsaberColors.purple
    
    // Status Colors
    val error = Color(0xFFFF3333)
    val success = LightsaberColors.green
    val warning = Color(0xFFFFAA00)
    val info = Color(0xFF00CCFF)
    
    // UI Elements - Lightsaber themed
    val borderGreen = LightsaberColors.green.copy(alpha = 0.5f)
    val borderPurple = LightsaberColors.purple.copy(alpha = 0.5f)
    val borderCyan = Color(0xFF00CCFF).copy(alpha = 0.4f)
    val surfaceGlowGreen = LightsaberColors.green.copy(alpha = 0.1f)
    val surfaceGlowPurple = LightsaberColors.purple.copy(alpha = 0.1f)
}

// Cyberpunk Color Scheme for Material3
val CyberpunkDarkColorScheme = darkColorScheme(
    primary = CyberpunkColors.lightsaberGreen,
    onPrimary = CyberpunkColors.backgroundPrimary,
    primaryContainer = CyberpunkColors.lightsaberGreen.copy(alpha = 0.2f),
    onPrimaryContainer = CyberpunkColors.lightsaberGreen,
    secondary = CyberpunkColors.lightsaberPurple,
    onSecondary = CyberpunkColors.backgroundPrimary,
    secondaryContainer = CyberpunkColors.lightsaberPurple.copy(alpha = 0.2f),
    onSecondaryContainer = CyberpunkColors.lightsaberPurple,
    tertiary = CyberpunkColors.lightsaberPurpleBright,
    onTertiary = CyberpunkColors.backgroundPrimary,
    tertiaryContainer = CyberpunkColors.lightsaberPurpleBright.copy(alpha = 0.2f),
    onTertiaryContainer = CyberpunkColors.lightsaberPurpleBright,
    background = CyberpunkColors.backgroundPrimary,
    onBackground = CyberpunkColors.textPrimary,
    surface = CyberpunkColors.backgroundSecondary,
    onSurface = CyberpunkColors.textPrimary,
    surfaceVariant = CyberpunkColors.backgroundElevated,
    onSurfaceVariant = CyberpunkColors.textSecondary,
    error = CyberpunkColors.error,
    onError = CyberpunkColors.textPrimary,
    errorContainer = CyberpunkColors.error.copy(alpha = 0.2f),
    onErrorContainer = CyberpunkColors.error,
    outline = CyberpunkColors.borderGreen,
    outlineVariant = CyberpunkColors.borderPurple,
    surfaceTint = CyberpunkColors.lightsaberGreen,
    inverseSurface = CyberpunkColors.textPrimary,
    inverseOnSurface = CyberpunkColors.backgroundPrimary,
    inversePrimary = CyberpunkColors.lightsaberPurple,
    surfaceDim = CyberpunkColors.backgroundPrimary,
    surfaceBright = CyberpunkColors.backgroundElevated,
    surfaceContainerLowest = CyberpunkColors.backgroundPrimary,
    surfaceContainerLow = CyberpunkColors.backgroundSecondary,
    surfaceContainer = CyberpunkColors.backgroundElevated,
    surfaceContainerHigh = Color(0xFF202020),
    surfaceContainerHighest = Color(0xFF252525),
    scrim = Color(0xFF000000).copy(alpha = 0.9f)
)

// Cyberpunk Theme Composition Local
val LocalCyberpunkTheme = staticCompositionLocalOf { true }

/**
 * Carbon Fiber Background Modifier
 * Creates a subtle carbon fiber weave pattern
 */
fun Modifier.carbonFiberBackground(
    baseColor: Color = CarbonFiberColors.baseMedium,
    weaveColor: Color = CarbonFiberColors.weaveLight
): Modifier = composed {
    this.drawBehind {
        // Draw base color
        drawRect(color = baseColor)
        
        // Draw carbon fiber weave pattern
        val weaveSize = 8f
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Diagonal weave pattern (simplified)
        val path = Path()
        var x = 0f
        while (x < canvasWidth + canvasHeight) {
            // Diagonal lines at 45 degrees
            path.moveTo(x, 0f)
            path.lineTo(x - canvasHeight, canvasHeight)
            x += weaveSize * 2
        }
        
        // Opposing diagonal
        x = 0f
        while (x < canvasWidth + canvasHeight) {
            path.moveTo(x - canvasHeight, 0f)
            path.lineTo(x, canvasHeight)
            x += weaveSize * 2
        }
        
        drawPath(
            path = path,
            color = weaveColor.copy(alpha = 0.15f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
    }
}

/**
 * Glassmorphism effect modifier with lightsaber tint
 */
fun Modifier.glassmorphism(
    backgroundColor: Color = CyberpunkColors.backgroundCard.copy(alpha = 0.8f),
    blurRadius: Dp = 16.dp
): Modifier = composed {
    this.background(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Neon glow border modifier - Lightsaber themed
 */
fun Modifier.neonBorder(
    color: Color = CyberpunkColors.lightsaberGreen,
    width: Dp = 1.dp,
    glowRadius: Dp = 8.dp,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
): Modifier = composed {
    this.border(
        width = width,
        color = color.copy(alpha = 0.8f),
        shape = shape
    )
    .drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            paint.asFrameworkPaint().apply {
                isAntiAlias = true
                setShadowLayer(
                    glowRadius.toPx(),
                    0f,
                    0f,
                    color.copy(alpha = 0.6f).toArgb()
                )
            }
        }
    }
}

/**
 * Animated gradient background with lightsaber colors
 */
@Composable
fun Modifier.animatedGradientBackground(
    durationMillis: Int = 6000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )
    
    this.background(
        brush = Brush.linearGradient(
            colors = listOf(
                CyberpunkColors.lightsaberGreen.copy(alpha = 0.08f),
                CyberpunkColors.lightsaberPurple.copy(alpha = 0.05f),
                CyberpunkColors.lightsaberGreen.copy(alpha = 0.03f)
            ),
            start = Offset(0f, 0f),
            end = Offset(
                x = 1000f * offset,
                y = 1000f * (1 - offset)
            )
        )
    )
}

/**
 * Neon shadow modifier - Lightsaber themed
 */
fun Modifier.neonShadow(
    color: Color = CyberpunkColors.lightsaberGreen,
    elevation: Dp = 8.dp
): Modifier = composed {
    this.shadow(
        elevation = elevation,
        shape = RoundedCornerShape(16.dp),
        spotColor = color.copy(alpha = 0.6f),
        ambientColor = color.copy(alpha = 0.3f)
    )
}

/**
 * Cyberpunk card style with lightsaber accents
 */
@Composable
fun CyberpunkCard(
    modifier: Modifier = Modifier,
    neonColor: Color = CyberpunkColors.lightsaberGreen,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = CyberpunkColors.backgroundCard,
                shape = RoundedCornerShape(16.dp)
            )
            .neonBorder(neonColor)
            .padding(16.dp)
    ) {
        content()
    }
}

/**
 * Character card style for selector with lightsaber glow
 */
@Composable
fun CharacterCardStyle(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    neonColor: Color = CyberpunkColors.lightsaberGreen,
    content: @Composable () -> Unit
) {
    val borderColor = if (isSelected) neonColor else CyberpunkColors.borderGreen.copy(alpha = 0.3f)
    val glowRadius = if (isSelected) 16.dp else 4.dp
    
    Box(
        modifier = modifier
            .background(
                color = CyberpunkColors.backgroundCard.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            )
            .neonBorder(borderColor, glowRadius = glowRadius)
            .padding(12.dp)
    ) {
        content()
    }
}

/**
 * Chat bubble style - Lightsaber themed
 * User messages: Green
 * AI messages: Purple
 */
@Composable
fun CyberpunkChatBubble(
    isUser: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundColor = if (isUser) {
        CyberpunkColors.lightsaberGreen.copy(alpha = 0.12f)
    } else {
        CyberpunkColors.lightsaberPurple.copy(alpha = 0.12f)
    }
    
    val borderColor = if (isUser) {
        CyberpunkColors.lightsaberGreen.copy(alpha = 0.6f)
    } else {
        CyberpunkColors.lightsaberPurple.copy(alpha = 0.5f)
    }
    
    val glowColor = if (isUser) {
        CyberpunkColors.greenGlow
    } else {
        CyberpunkColors.purpleGlow
    }
    
    Box(
        modifier = modifier
            .drawBehind {
                // Draw glow effect behind the bubble
                drawIntoCanvas { canvas ->
                    val paint = Paint()
                    paint.asFrameworkPaint().apply {
                        isAntiAlias = true
                        setShadowLayer(
                            12f,
                            0f,
                            0f,
                            glowColor.toArgb()
                        )
                    }
                }
            }
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                )
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                )
            )
            .padding(12.dp)
    ) {
        content()
    }
}

/**
 * Voice wave visualization colors - Lightsaber themed
 */
object VoiceWaveColors {
    val active = listOf(
        CyberpunkColors.lightsaberGreen,
        CyberpunkColors.lightsaberGreenBright,
        CyberpunkColors.lightsaberPurple,
        CyberpunkColors.lightsaberPurpleBright
    )
    
    val inactive = listOf(
        CyberpunkColors.textMuted.copy(alpha = 0.5f),
        CyberpunkColors.textMuted.copy(alpha = 0.3f)
    )
    
    val recording = listOf(
        CyberpunkColors.lightsaberGreen,
        CyberpunkColors.lightsaberGreenBright
    )
}

/**
 * Lightsaber glow effect for special elements
 */
@Composable
fun Modifier.lightsaberGlow(
    color: Color = CyberpunkColors.lightsaberGreen,
    intensity: Dp = 12.dp
): Modifier = composed {
    this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            paint.asFrameworkPaint().apply {
                isAntiAlias = true
                setShadowLayer(
                    intensity.toPx(),
                    0f,
                    0f,
                    color.copy(alpha = 0.7f).toArgb()
                )
            }
        }
    }
}