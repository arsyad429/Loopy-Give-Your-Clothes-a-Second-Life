package id.ac.loopy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.loopy.R

private val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_bold, FontWeight.Bold),
)

private fun loopyType(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Jakarta, fontSize = size.sp, lineHeight = lineHeight.sp, fontWeight = weight)

private val ScreenTypography = Typography(
    headlineMedium = loopyType(26, 32, FontWeight.Bold),
    titleLarge = loopyType(18, 24, FontWeight.Bold),
    titleMedium = loopyType(15, 22, FontWeight.SemiBold),
    bodyLarge = loopyType(16, 24),
    bodyMedium = loopyType(14, 21),
    bodySmall = loopyType(12, 18),
    labelLarge = loopyType(14, 20, FontWeight.SemiBold),
    labelMedium = loopyType(12, 18, FontWeight.SemiBold),
)

@Composable
internal fun LoopyScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val screenColors = if (colors.background.luminance() > 0.5f) {
        colors.copy(
            background = Color(0xFFFAF8F3),
            primary = Color(0xFF246348),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE7EFDF),
            onPrimaryContainer = Color(0xFF223D31),
            surface = Color.White,
            onSurface = Color(0xFF223D31),
            onBackground = Color(0xFF223D31),
            onSurfaceVariant = Color(0xFF596B60),
            outline = Color(0xFF727F73),
            outlineVariant = Color(0xFFDCE5D6),
            tertiaryContainer = Color(0xFFFFEDC4),
            onTertiaryContainer = Color(0xFF563A11),
        )
    } else {
        colors.copy(
            background = Color(0xFF161D18),
            surface = Color(0xFF222B24),
            onBackground = Color(0xFFE4EBE4),
            onSurface = Color(0xFFE4EBE4),
            onSurfaceVariant = Color(0xFFBAC9BD),
            primaryContainer = Color(0xFF2D4435),
            onPrimaryContainer = Color(0xFFD9EADF),
            outlineVariant = Color(0xFF526357),
            tertiaryContainer = Color(0xFF594217),
            onTertiaryContainer = Color(0xFFFFDEA0),
        )
    }

    MaterialTheme(colorScheme = screenColors, typography = ScreenTypography) {
        Box(
            modifier = Modifier.fillMaxSize().background(screenColors.background).imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(start = if (onBack == null) 24.dp else 12.dp, end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_clothing_back),
                                contentDescription = "Kembali",
                                tint = screenColors.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Text(title, color = screenColors.primary, style = ScreenTypography.titleLarge)
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    content = content,
                )

                if (footer != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = footer,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LoopyIntro(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineMedium)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun LoopyInfo(title: String, description: String, isError: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val background = if (isError) colors.errorContainer else colors.primaryContainer
    val foreground = if (isError) colors.onErrorContainer else colors.onPrimaryContainer
    val announcement = if (isError) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier

    Column(
        modifier = announcement.fillMaxWidth().background(background, RoundedCornerShape(20.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = foreground, style = MaterialTheme.typography.titleMedium)
        Text(description, color = foreground, style = MaterialTheme.typography.bodyMedium)
    }
}
