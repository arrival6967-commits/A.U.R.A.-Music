package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.EqualizerState
import com.example.data.local.EqualizerPresetEntity
import com.example.ui.components.EqualizerCurveCanvas
import java.util.Locale

private val BandLabels = listOf(
    "60 Гц" to "Суб-бас",
    "230 Гц" to "Мид-бас",
    "910 Гц" to "Средние",
    "3.6 кГц" to "Присутствие",
    "14 кГц" to "Воздух / ВЧ"
)

@Composable
fun EqualizerScreen(
    equalizerState: EqualizerState,
    presets: List<EqualizerPresetEntity>,
    onToggleEnabled: (Boolean) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Float) -> Unit,
    onApplyPreset: (EqualizerPresetEntity) -> Unit,
    onResetFlat: () -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (EqualizerPresetEntity) -> Unit,
    onBackToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToLibrary)

    var showSavePresetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("equalizer_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Master Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DSP Эквалайзер и Эффекты",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Активный профиль: ${equalizerState.activePresetName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Switch(
                        checked = equalizerState.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }
        }

        // Real-time Spline Frequency Response Curve
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "АМПЛИТУДНО-ЧАСТОТНАЯ ХАРАКТЕРИСТИКА (АЧХ)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-15 dB .. +15 dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    EqualizerCurveCanvas(
                        bandGainsDb = equalizerState.bandGainsDb,
                        isEnabled = equalizerState.isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BandLabels.forEach { (freq, _) ->
                            Text(
                                text = freq,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Presets Bar + Save & Reset Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Звуковые пресеты",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onResetFlat,
                            modifier = Modifier.testTag("reset_eq_flat_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("0 dB")
                        }

                        FilledTonalButton(
                            onClick = { showSavePresetDialog = true },
                            modifier = Modifier.testTag("save_eq_preset_button")
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сохранить")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = equalizerState.activePresetId == preset.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onApplyPreset(preset) },
                            label = { Text(preset.name) },
                            trailingIcon = if (preset.isCustom) {
                                {
                                    IconButton(
                                        onClick = { onDeleteCustomPreset(preset) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Удалить пресет",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier.testTag("eq_preset_chip_${preset.id}")
                        )
                    }
                }
            }
        }

        // 5-Band Parametric Sliders
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "5-полосный эквалайзер",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    BandLabels.forEachIndexed { index, (freqLabel, roleLabel) ->
                        val gain = equalizerState.bandGainsDb.getOrElse(index) { 0f }
                        val formattedGain = String.format(
                            Locale.US,
                            "%s%.1f dB",
                            if (gain > 0f) "+" else "",
                            gain
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = freqLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = roleLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = formattedGain,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (gain != 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Slider(
                                value = gain,
                                onValueChange = { onBandChange(index, it) },
                                valueRange = -15f..15f,
                                enabled = equalizerState.isEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("eq_band_slider_$index")
                            )
                        }
                    }
                }
            }
        }

        // Dedicated DSP AudioFX Controls: Bass Boost, 3D Virtualizer, Loudness Enhancer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Пространственные и динамические эффекты",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Bass Boost
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speaker,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Усиление баса (Bass Boost)", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = "${equalizerState.bassBoostStrength / 10}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = equalizerState.bassBoostStrength.toFloat(),
                            onValueChange = { onBassBoostChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = equalizerState.isEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bass_boost_slider")
                        )
                    }

                    // 3D Virtualizer
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SurroundSound,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("3D Виртуализатор сцены", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = "${equalizerState.virtualizerStrength / 10}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = equalizerState.virtualizerStrength.toFloat(),
                            onValueChange = { onVirtualizerChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = equalizerState.isEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("virtualizer_slider")
                        )
                    }

                    // Loudness Enhancer
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Усилитель громкости (Preamp)", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = String.format(Locale.US, "+%.1f dB", equalizerState.loudnessGainDb),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Slider(
                            value = equalizerState.loudnessGainDb,
                            onValueChange = onLoudnessChange,
                            valueRange = 0f..10f,
                            enabled = equalizerState.isEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loudness_slider")
                        )
                    }
                }
            }
        }
    }

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Сохранить пресет эквалайзера") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Название пресета") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_preset_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSaveCustomPreset(presetName)
                            showSavePresetDialog = false
                        }
                    },
                    enabled = presetName.isNotBlank(),
                    modifier = Modifier.testTag("confirm_save_preset_button")
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
