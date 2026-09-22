package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * CloudBridge Design System Canonical Color Palette
 * Specified tokens:
 * - Background: #F8FAFC
 * - Primary:    #2563EB
 * - Text:       #111827
 * - Success:    #16A34A
 * - Error:      #DC2626
 */
object CloudBridgeDesignTokens {
  val Background = Color(0xFFF8FAFC)
  val Primary = Color(0xFF2563EB)
  val Text = Color(0xFF111827)
  val Success = Color(0xFF16A34A)
  val Error = Color(0xFFDC2626)

  // Supporting tones
  val PrimaryContainer = Color(0xFFEFF6FF)
  val OnPrimaryContainer = Color(0xFF1E40AF)
  val Secondary = Color(0xFF64748B)
  val Surface = Color(0xFFFFFFFF)
  val SurfaceVariant = Color(0xFFF1F5F9)
  val Border = Color(0xFFE2E8F0)
  val SuccessContainer = Color(0xFFDCFCE7)
  val ErrorContainer = Color(0xFFFEE2E2)
  val Warning = Color(0xFFD97706)
  val WarningContainer = Color(0xFFFEF3C7)
}

/**
 * Extended color attributes for status indicators (Success, Warning, custom accents)
 */
@Immutable
data class CloudBridgeExtendedColors(
  val success: Color = CloudBridgeDesignTokens.Success,
  val successContainer: Color = CloudBridgeDesignTokens.SuccessContainer,
  val error: Color = CloudBridgeDesignTokens.Error,
  val errorContainer: Color = CloudBridgeDesignTokens.ErrorContainer,
  val warning: Color = CloudBridgeDesignTokens.Warning,
  val warningContainer: Color = CloudBridgeDesignTokens.WarningContainer,
  val textPrimary: Color = CloudBridgeDesignTokens.Text,
  val textSecondary: Color = CloudBridgeDesignTokens.Secondary,
  val border: Color = CloudBridgeDesignTokens.Border
)

val LocalCloudBridgeExtendedColors = staticCompositionLocalOf {
  CloudBridgeExtendedColors()
}

val MaterialTheme.extendedColors: CloudBridgeExtendedColors
  @Composable
  @ReadOnlyComposable
  get() = LocalCloudBridgeExtendedColors.current

private val LightColorScheme =
  lightColorScheme(
    primary = CloudBridgeDesignTokens.Primary,
    onPrimary = Color.White,
    primaryContainer = CloudBridgeDesignTokens.PrimaryContainer,
    onPrimaryContainer = CloudBridgeDesignTokens.OnPrimaryContainer,
    secondary = CloudBridgeDesignTokens.Secondary,
    onSecondary = Color.White,
    background = CloudBridgeDesignTokens.Background,
    onBackground = CloudBridgeDesignTokens.Text,
    surface = CloudBridgeDesignTokens.Surface,
    onSurface = CloudBridgeDesignTokens.Text,
    surfaceVariant = CloudBridgeDesignTokens.SurfaceVariant,
    onSurfaceVariant = CloudBridgeDesignTokens.Secondary,
    outline = CloudBridgeDesignTokens.Border,
    error = CloudBridgeDesignTokens.Error,
    onError = Color.White,
    errorContainer = CloudBridgeDesignTokens.ErrorContainer,
    onErrorContainer = Color(0xFF991B1B)
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color.White,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = CloudBridgeDesignTokens.Error,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
  )

/**
 * CloudBridge Theme providing colorScheme, typography, and extendedColors to the UI tree.
 */
@Composable
fun CloudBridgeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent CloudBridge design system branding
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val extendedColors = remember(darkTheme) {
    if (darkTheme) {
      CloudBridgeExtendedColors(
        success = Color(0xFF22C55E),
        successContainer = Color(0xFF14532D),
        error = CloudBridgeDesignTokens.Error,
        errorContainer = Color(0xFF7F1D1D),
        warning = Color(0xFFF59E0B),
        warningContainer = Color(0xFF78350F),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        border = Color(0xFF334155)
      )
    } else {
      CloudBridgeExtendedColors(
        success = CloudBridgeDesignTokens.Success,
        successContainer = CloudBridgeDesignTokens.SuccessContainer,
        error = CloudBridgeDesignTokens.Error,
        errorContainer = CloudBridgeDesignTokens.ErrorContainer,
        warning = CloudBridgeDesignTokens.Warning,
        warningContainer = CloudBridgeDesignTokens.WarningContainer,
        textPrimary = CloudBridgeDesignTokens.Text,
        textSecondary = CloudBridgeDesignTokens.Secondary,
        border = CloudBridgeDesignTokens.Border
      )
    }
  }

  CompositionLocalProvider(LocalCloudBridgeExtendedColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = CloudBridgeTypography,
      content = content
    )
  }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) = CloudBridgeTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)


