/*
 * Copyright (C) 2024 Shubham Panchal
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

package io.shubham0204.smollmandroid.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import io.shubham0204.smollmandroid.theme.CyberpunkDarkColorScheme
import io.shubham0204.smollmandroid.theme.LocalCyberpunkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

private val lightScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    )

private val darkScheme =
    darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )

/**
 * App Theme Modes
 */
enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    CYBERPUNK
}

@Single
class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _currentThemeMode = MutableStateFlow(loadThemeMode())
    val currentThemeMode: StateFlow<AppThemeMode> = _currentThemeMode
    
    val isCyberpunk: Boolean
        get() = _currentThemeMode.value == AppThemeMode.CYBERPUNK
    
    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
    }
    
    fun setThemeMode(mode: AppThemeMode) {
        _currentThemeMode.value = mode
        saveThemeMode(mode)
    }
    
    fun cycleTheme() {
        val nextMode = when (_currentThemeMode.value) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.CYBERPUNK
            AppThemeMode.CYBERPUNK -> AppThemeMode.SYSTEM
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
        }
        setThemeMode(nextMode)
    }
    
    fun toggleCyberpunk() {
        val newMode = if (_currentThemeMode.value == AppThemeMode.CYBERPUNK) {
            AppThemeMode.DARK
        } else {
            AppThemeMode.CYBERPUNK
        }
        setThemeMode(newMode)
    }
    
    private fun loadThemeMode(): AppThemeMode {
        val modeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.CYBERPUNK.name)
        return try {
            AppThemeMode.valueOf(modeName!!)
        } catch (e: Exception) {
            AppThemeMode.CYBERPUNK
        }
    }
    
    private fun saveThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}

@Composable
fun SmolLMAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    cyberpunkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable() () -> Unit,
) {
    val colorScheme =
        when {
            cyberpunkTheme -> CyberpunkDarkColorScheme
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> darkScheme
            else -> lightScheme
        }
    
    CompositionLocalProvider(
        LocalCyberpunkTheme provides cyberpunkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
            typography = if (cyberpunkTheme) CyberpunkTypography else AppTypography
        )
    }
}

@Composable
fun SmolLMAndroidTheme(
    themeMode: AppThemeMode = AppThemeMode.CYBERPUNK,
    dynamicColor: Boolean = false,
    content: @Composable() () -> Unit,
) {
    val isDarkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.CYBERPUNK -> true
    }
    
    val isCyberpunk = themeMode == AppThemeMode.CYBERPUNK
    
    SmolLMAndroidTheme(
        darkTheme = isDarkTheme,
        cyberpunkTheme = isCyberpunk,
        dynamicColor = dynamicColor,
        content = content
    )
}