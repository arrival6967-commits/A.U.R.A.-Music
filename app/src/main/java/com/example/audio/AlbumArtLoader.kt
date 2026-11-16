package com.example.audio

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

data class CoverArtResult(
    val bitmap: Bitmap,
    val isEmbedded: Boolean,
    val dominantColorArgb: Int,
    val secondaryColorArgb: Int
)

object AlbumArtLoader {

    private val memoryCache = object : LruCache<String, CoverArtResult>(24) {}

    private val FallbackGradientPairs = listOf(
        0xFF00B4DB.toInt() to 0xFF0083B0.toInt(),
        0xFFFF416C.toInt() to 0xFF8A2387.toInt(),
        0xFFF7971E.toInt() to 0xFFFFD200.toInt(),
        0xFF11998E.toInt() to 0xFF38EF7D.toInt(),
        0xFF8E2DE2.toInt() to 0xFF4A00E0.toInt(),
        0xFFEC008C.toInt() to 0xFFFC6767.toInt(),
        0xFF1A2980.toInt() to 0xFF26D0CE.toInt(),
        0xFFEB3349.toInt() to 0xFFF45C43.toInt()
    )

    fun getFallbackColors(paletteIndex: Int): Pair<Int, Int> {
        return FallbackGradientPairs[paletteIndex.mod(FallbackGradientPairs.size)]
    }

    suspend fun loadCoverArt(context: Context, track: TrackEntity, targetSizePx: Int = 512): CoverArtResult =
        withContext(Dispatchers.IO) {
            val cacheKey = "${track.contentUri}_${targetSizePx}"
            memoryCache.get(cacheKey)?.let { return@withContext it }

            var embeddedBitmap: Bitmap? = null

            // 1. Try MediaMetadataRetriever embedded picture
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(track.contentUri))
                val bytes = retriever.embeddedPicture
                if (bytes != null && bytes.isNotEmpty()) {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    options.inSampleSize = calculateInSampleSize(options, targetSizePx, targetSizePx)
                    options.inJustDecodeBounds = false
                    embeddedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }

            // 2. Fallback to MediaStore album art thumbnail if albumId is present
            if (embeddedBitmap == null && track.albumId != null && track.albumId > 0) {
                try {
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        track.albumId
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        embeddedBitmap = context.contentResolver.loadThumbnail(
                            albumArtUri,
                            Size(targetSizePx, targetSizePx),
                            null
                        )
                    } else {
                        context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                            embeddedBitmap = BitmapFactory.decodeStream(stream)
                        }
                    }
                } catch (_: Exception) {
                }
            }

            val result = if (embeddedBitmap != null) {
                val (dom, sec) = extractDominantAndSecondaryColors(embeddedBitmap!!, track.coverPaletteIndex)
                CoverArtResult(
                    bitmap = embeddedBitmap!!,
                    isEmbedded = true,
                    dominantColorArgb = dom,
                    secondaryColorArgb = sec
                )
            } else {
                val (dom, sec) = getFallbackColors(track.coverPaletteIndex)
                val generated = generateStylizedCoverBitmap(
                    title = track.title,
                    artist = track.artist,
                    primaryArgb = dom,
                    secondaryArgb = sec,
                    sizePx = targetSizePx
                )
                CoverArtResult(
                    bitmap = generated,
                    isEmbedded = false,
                    dominantColorArgb = dom,
                    secondaryColorArgb = sec
                )
            }

            memoryCache.put(cacheKey, result)
            result
        }

    fun extractDominantAndSecondaryColors(bitmap: Bitmap, fallbackIndex: Int): Pair<Int, Int> {
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
            var rSum1 = 0L
            var gSum1 = 0L
            var bSum1 = 0L
            var count1 = 0

            var rSum2 = 0L
            var gSum2 = 0L
            var bSum2 = 0L
            var count2 = 0

            for (y in 0 until scaled.height) {
                for (x in 0 until scaled.width) {
                    val pixel = scaled.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val maxC = max(r, max(g, b))
                    val minC = min(r, min(g, b))
                    val saturation = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC.toFloat()
                    val weight = if (saturation > 0.25f && maxC in 35..235) 3 else 1

                    if (x + y < scaled.width) {
                        rSum1 += r * weight
                        gSum1 += g * weight
                        bSum1 += b * weight
                        count1 += weight
                    } else {
                        rSum2 += r * weight
                        gSum2 += g * weight
                        bSum2 += b * weight
                        count2 += weight
                    }
                }
            }

            if (count1 == 0 || count2 == 0) {
                return getFallbackColors(fallbackIndex)
            }

            val dom = (0xFF shl 24) or
                (((rSum1 / count1).toInt().coerceIn(24, 230)) shl 16) or
                (((gSum1 / count1).toInt().coerceIn(24, 230)) shl 8) or
                ((bSum1 / count1).toInt().coerceIn(24, 230))

            val sec = (0xFF shl 24) or
                (((rSum2 / count2).toInt().coerceIn(20, 220)) shl 16) or
                (((gSum2 / count2).toInt().coerceIn(20, 220)) shl 8) or
                ((bSum2 / count2).toInt().coerceIn(20, 220))

            dom to sec
        } catch (_: Exception) {
            getFallbackColors(fallbackIndex)
        }
    }

    private fun generateStylizedCoverBitmap(
        title: String,
        artist: String,
        primaryArgb: Int,
        secondaryArgb: Int,
        sizePx: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                sizePx.toFloat(),
                sizePx.toFloat(),
                intArrayOf(primaryArgb, secondaryArgb, 0xFF101420.toInt()),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

        // Subtle modern geometric wave rings on cover
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x28FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.012f
        }
        canvas.drawCircle(sizePx * 0.8f, sizePx * 0.2f, sizePx * 0.36f, wavePaint)
        canvas.drawCircle(sizePx * 0.2f, sizePx * 0.85f, sizePx * 0.42f, wavePaint)

        // Frosted inner badge with track monogram
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33000000
            style = Paint.Style.FILL
        }
        val pad = sizePx * 0.18f
        val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)
        canvas.drawRoundRect(rect, sizePx * 0.12f, sizePx * 0.12f, cardPaint)

        val monogram = title.trim().take(2).uppercase().ifEmpty { "AU" }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = sizePx * 0.26f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val textY = sizePx * 0.5f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(monogram, sizePx * 0.5f, textY, textPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            textSize = sizePx * 0.055f
            textAlign = Paint.Align.CENTER
        }
        val artistShort = artist.take(22)
        canvas.drawText(artistShort, sizePx * 0.5f, sizePx * 0.76f, subPaint)

        return bmp
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
