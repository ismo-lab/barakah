package dev.barakah.app.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AdhanSoundManager {

    private var mediaPlayer: MediaPlayer? = null
    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState

    private val _playingResId = MutableStateFlow<Int?>(null)
    val playingResId: StateFlow<Int?> = _playingResId

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val stopRunnable = Runnable { stop() }

    @Synchronized
    fun play(context: Context, resId: Int, isShort: Boolean) {
        // Stop any currently playing audio first
        stop()

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val mp = MediaPlayer.create(context.applicationContext, resId, audioAttributes, 0)
            if (mp != null) {
                mediaPlayer = mp
                _isPlayingState.value = true
                _playingResId.value = resId
                mp.start()

                mp.setOnCompletionListener {
                    stop()
                }

                if (isShort) {
                    handler.postDelayed(stopRunnable, 20000)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlayingState.value = false
            _playingResId.value = null
        }
    }

    @Synchronized
    fun stop() {
        handler.removeCallbacks(stopRunnable)
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _isPlayingState.value = false
            _playingResId.value = null
        }
    }
}
