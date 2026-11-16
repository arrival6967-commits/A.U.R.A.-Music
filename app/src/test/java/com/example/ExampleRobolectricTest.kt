package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AlbumArtLoader
import com.example.audio.AudioPlaybackController
import com.example.data.local.TrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("A.U.R.A. Music", appName)
    }

    @Test
    fun `cover art loader generates cover and extracts dominant colors and equalizer updates state`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sampleTrack = TrackEntity(
            id = 1L,
            title = "Local Track",
            artist = "Device Artist",
            album = "Device Album",
            genre = "Local",
            durationMs = 180000L,
            contentUri = "content://media/external/audio/media/1"
        )
        val coverResult = AlbumArtLoader.loadCoverArt(context, sampleTrack, targetSizePx = 128)
        assertNotNull(coverResult.bitmap)

        val controller = AudioPlaybackController.getInstance(context)
        controller.updateBandGain(0, 6.5f)
        assertEquals(6.5f, controller.equalizerState.value.bandGainsDb[0], 0.01f)
    }
}
