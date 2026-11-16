package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.audio.AlbumArtLoader
import com.example.data.local.TrackEntity
import java.util.Locale

private val CoverPalettes = listOf(
    listOf(Color(0xFF00B4DB), Color(0xFF0083B0), Color(0xFF121A2A)),
    listOf(Color(0xFFFF416C), Color(0xFF8A2387), Color(0xFF1B102E)),
    listOf(Color(0xFFF7971E), Color(0xFFFF5252), Color(0xFF2B1216)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFF0B292E)),
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0), Color(0xFF141836)),
    listOf(Color(0xFFEC008C), Color(0xFFFC6767), Color(0xFF190E2E)),
    listOf(Color(0xFF1A2980), Color(0xFF26D0CE), Color(0xFF102826)),
    listOf(Color(0xFFEB3349), Color(0xFFF45C43), Color(0xFF1C1C2E))
)

fun getPaletteColors(index: Int): List<Color> {
    return CoverPalettes[index.mod(CoverPalettes.size)]
}

/**
 * Displays the track's album cover artwork (either embedded from the audio file
 * or a stylized cover generated for the track), without any spinning vinyl record.
 */
@Composable
fun TrackCoverArtwork(
    track: TrackEntity?,
    preloadedBitmap: Bitmap? = null,
    paletteIndex: Int = track?.coverPaletteIndex ?: 0,
    size: Dp = 56.dp,
    cornerRadius: Dp = size * 0.22f,
    elevation: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loadedBitmap by remember(track?.contentUri, preloadedBitmap) {
        mutableStateOf(preloadedBitmap)
    }

    LaunchedEffect(track?.contentUri, preloadedBitmap) {
        if (preloadedBitmap != null) {
            loadedBitmap = preloadedBitmap
        } else if (track != null) {
            val result = AlbumArtLoader.loadCoverArt(context, track, targetSizePx = 384)
            loadedBitmap = result.bitmap
        }
    }

    val shape = RoundedCornerShape(cornerRadius)
    val fallbackColors = remember(track?.dominantColorArgb, track?.secondaryColorArgb, paletteIndex) {
        if (track?.dominantColorArgb != null && track.secondaryColorArgb != null) {
            listOf(Color(track.dominantColorArgb), Color(track.secondaryColorArgb))
        } else {
            getPaletteColors(paletteIndex)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = fallbackColors,
                    start = Offset.Zero,
                    end = Offset(400f, 400f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = loadedBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = track?.title ?: "Обложка трека",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val icon = when (paletteIndex.mod(4)) {
                0 -> Icons.Default.GraphicEq
                1 -> Icons.Default.MusicNote
                2 -> Icons.Default.Waves
                else -> Icons.Default.Album
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(size * 0.44f)
            )
        }
    }
}

@Composable
fun SpectrumAnalyzerView(
    bars: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEachIndexed { index, value ->
            val heightFraction = if (isPlaying) value.coerceIn(0.12f, 1f) else 0.12f
            val barColor = if (index < bars.size / 2) accentColor else secondaryColor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.35f))
                        )
                    )
            )
        }
    }
}

@Composable
fun EqualizerCurveCanvas(
    bandGainsDb: List<Float>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        listOf(0.15f, 0.5f, 0.85f).forEach { frac ->
            drawLine(
                color = gridColor,
                start = Offset(0f, h * frac),
                end = Offset(w, h * frac),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (bandGainsDb.isEmpty()) return@Canvas

        val points = bandGainsDb.mapIndexed { idx, gain ->
            val x = (w * (idx + 0.5f)) / bandGainsDb.size
            val effectiveGain = if (isEnabled) gain.coerceIn(-15f, 15f) else 0f
            val normalizedY = midY - (effectiveGain / 15f) * (h * 0.38f)
            Offset(x, normalizedY)
        }

        val strokePath = Path().apply {
            moveTo(0f, points.first().y)
            lineTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlX1 = (p0.x + p1.x) / 2f
                val controlY1 = p0.y
                val controlX2 = (p0.x + p1.x) / 2f
                val controlY2 = p1.y
                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }
            lineTo(w, points.last().y)
        }

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        val activeGradient = if (isEnabled) {
            listOf(primaryColor.copy(alpha = 0.38f), secondaryColor.copy(alpha = 0.08f), Color.Transparent)
        } else {
            listOf(gridColor.copy(alpha = 0.2f), Color.Transparent)
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(activeGradient)
        )

        drawPath(
            path = strokePath,
            brush = Brush.horizontalGradient(
                colors = if (isEnabled) listOf(primaryColor, secondaryColor) else listOf(gridColor, gridColor)
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEach { pt ->
            drawCircle(
                color = if (isEnabled) primaryColor else gridColor,
                radius = 6.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = surfaceColor,
                radius = 3.dp.toPx(),
                center = pt
            )
        }
    }
}

fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
