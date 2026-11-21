package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnimationStyle
import com.example.model.IconStyle
import com.example.model.IslandThemePreset
import com.example.ui.components.DynamicIslandPill
import com.example.ui.components.GlassCard
import com.example.ui.components.IslandCallbacks
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltraViolet
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.MainUiState
import java.util.Locale

private val SWATCH_BACKGROUND_COLORS = listOf(
    0xFF000000L, 0xFF0D1119L, 0xFF141824L, 0xFF091318L, 0xFF170D24L, 0xFF1A140DL
)

private val SWATCH_ACCENT_COLORS = listOf(
    0xFF00E5FFL, 0xFF7C4DFFL, 0xFF00E676L, 0xFFFFB300L, 0xFFFF4D6DL, 0xFF38BDF8L, 0xFFFFFFFFL
)

@Composable
fun CustomizationStudioScreen(
    uiState: MainUiState,
    onTogglePreviewExpand: () -> Unit,
    onApplyThemePreset: (IslandThemePreset) -> Unit,
    onUpdateDimensions: (widthDp: Float?, heightDp: Float?, offsetXFraction: Float?, offsetYDp: Float?, cornerRadiusDp: Float?) -> Unit,
    onUpdateAppearance: (transparency: Float?, glowIntensity: Float?, bgHex: Long?, borderHex: Long?, glowHex: Long?, textSizeSp: Float?, iconStyle: IconStyle?) -> Unit,
    onUpdateAnimation: (speed: Float?, style: AnimationStyle?, haptic: Boolean?) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        // Pinned Live Preview Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A0F1A), VoidBlack)
                    )
                )
                .border(1.dp, GlassBorder.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customization Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${settings.pillWidthDp.toInt()}×${settings.pillHeightDp.toInt()}dp • r${settings.cornerRadiusDp.toInt()}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = ElectricCyan
                    )
                }
                Spacer(Modifier.height(12.dp))
                DynamicIslandPill(
                    settings = settings,
                    telemetry = uiState.telemetry,
                    isExpanded = uiState.isPreviewExpanded,
                    displayModeOverride = uiState.previewMode,
                    callbacks = IslandCallbacks(onToggleExpand = onTogglePreviewExpand)
                )
            }
        }

        // Scrollable Studio Controls
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Theme Presets
            item {
                GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                    SectionHeader(
                        title = "Theme Presets",
                        subtitle = "Instant curated visual styles saved to DataStore"
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IslandThemePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = settings.themePreset == preset,
                                onClick = { onApplyThemePreset(preset) },
                                label = { Text(preset.displayName) },
                                modifier = Modifier.testTag("theme_chip_${preset.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }
                }
            }

            // Dimensions & Screen Placement
            item {
                GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                    SectionHeader(
                        title = "Size, Position & Corner Radius",
                        subtitle = "Align perfectly around your camera punch-hole or notch"
                    )
                    Spacer(Modifier.height(8.dp))
                    StudioSlider(
                        label = "Island Width",
                        valueText = "${settings.pillWidthDp.toInt()} dp",
                        value = settings.pillWidthDp,
                        valueRange = 140f..320f,
                        testTag = "slider_width",
                        onValueChange = { onUpdateDimensions(it, null, null, null, null) }
                    )
                    StudioSlider(
                        label = "Island Height",
                        valueText = "${settings.pillHeightDp.toInt()} dp",
                        value = settings.pillHeightDp,
                        valueRange = 30f..60f,
                        testTag = "slider_height",
                        onValueChange = { onUpdateDimensions(null, it, null, null, null) }
                    )
                    StudioSlider(
                        label = "Vertical Top Offset",
                        valueText = "${settings.offsetYDp.toInt()} dp",
                        value = settings.offsetYDp,
                        valueRange = 0f..84f,
                        testTag = "slider_offset_y",
                        onValueChange = { onUpdateDimensions(null, null, null, it, null) }
                    )
                    StudioSlider(
                        label = "Horizontal Offset",
                        valueText = String.format(Locale.US, "%+.0f%%", settings.offsetXFraction * 100f),
                        value = settings.offsetXFraction,
                        valueRange = -0.30f..0.30f,
                        testTag = "slider_offset_x",
                        onValueChange = { onUpdateDimensions(null, null, it, null, null) }
                    )
                    StudioSlider(
                        label = "Corner Radius",
                        valueText = "${settings.cornerRadiusDp.toInt()} dp",
                        value = settings.cornerRadiusDp,
                        valueRange = 10f..34f,
                        testTag = "slider_corner_radius",
                        onValueChange = { onUpdateDimensions(null, null, null, null, it) }
                    )
                }
            }

            // Colors, Glow & Transparency
            item {
                GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                    SectionHeader(
                        title = "Colors, Glow & Glass Opacity",
                        subtitle = "Customize capsule surface, border, and neon aura"
                    )
                    Spacer(Modifier.height(8.dp))
                    StudioSlider(
                        label = "Transparency / Opacity",
                        valueText = "${(settings.transparency * 100).toInt()}%",
                        value = settings.transparency,
                        valueRange = 0.30f..1.0f,
                        testTag = "slider_transparency",
                        onValueChange = { onUpdateAppearance(it, null, null, null, null, null, null) }
                    )
                    StudioSlider(
                        label = "Glow Intensity",
                        valueText = "${(settings.glowIntensity * 100).toInt()}%",
                        value = settings.glowIntensity,
                        valueRange = 0f..1.0f,
                        testTag = "slider_glow_intensity",
                        onValueChange = { onUpdateAppearance(null, it, null, null, null, null, null) }
                    )

                    Spacer(Modifier.height(10.dp))
                    ColorSwatchRow(
                        label = "Background Color",
                        colors = SWATCH_BACKGROUND_COLORS,
                        selectedHex = settings.backgroundColorHex,
                        onSelect = { onUpdateAppearance(null, null, it, null, null, null, null) }
                    )
                    Spacer(Modifier.height(12.dp))
                    ColorSwatchRow(
                        label = "Border Color",
                        colors = SWATCH_ACCENT_COLORS,
                        selectedHex = settings.borderColorHex,
                        onSelect = { onUpdateAppearance(null, null, null, it, null, null, null) }
                    )
                    Spacer(Modifier.height(12.dp))
                    ColorSwatchRow(
                        label = "Glow Color",
                        colors = SWATCH_ACCENT_COLORS,
                        selectedHex = settings.glowColorHex,
                        onSelect = { onUpdateAppearance(null, null, null, null, it, null, null) }
                    )
                }
            }

            // Typography, Icon Style & Motion Engine
            item {
                GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                    SectionHeader(
                        title = "Motion, Text & Icon Style",
                        subtitle = "Spring physics, icon rendering, and typography scale"
                    )
                    Spacer(Modifier.height(8.dp))
                    StudioSlider(
                        label = "Text Size",
                        valueText = "${settings.textSizeSp.toInt()} sp",
                        value = settings.textSizeSp,
                        valueRange = 11f..17f,
                        testTag = "slider_text_size",
                        onValueChange = { onUpdateAppearance(null, null, null, null, null, it, null) }
                    )
                    StudioSlider(
                        label = "Animation Speed",
                        valueText = String.format(Locale.US, "%.1fx", settings.animationSpeed),
                        value = settings.animationSpeed,
                        valueRange = 0.5f..2.0f,
                        testTag = "slider_anim_speed",
                        onValueChange = { onUpdateAnimation(it, null, null) }
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Animation Physics Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimationStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.animationStyle == style,
                                onClick = { onUpdateAnimation(null, style, null) },
                                label = { Text(style.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = UltraViolet.copy(alpha = 0.25f),
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Icon Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconStyle.entries.forEach { iconStyle ->
                            FilterChip(
                                selected = settings.iconStyle == iconStyle,
                                onClick = {
                                    onUpdateAppearance(null, null, null, null, null, null, iconStyle)
                                },
                                label = { Text(iconStyle.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonEmerald.copy(alpha = 0.22f),
                                    selectedLabelColor = NeonEmerald
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onResetDefaults,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_reset_studio"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Reset Studio to Defaults", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(
                text = valueText,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                color = ElectricCyan
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = GlassBorder
            )
        )
    }
}

@Composable
private fun ColorSwatchRow(
    label: String,
    colors: List<Long>,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            colors.forEach { hex ->
                val color = Color(hex)
                val isSelected = hex == selectedHex
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) ElectricCyan else GlassBorder,
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        val checkTint = if (hex == 0xFFFFFFFFL) Color.Black else Color.White
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected color",
                            tint = checkTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
