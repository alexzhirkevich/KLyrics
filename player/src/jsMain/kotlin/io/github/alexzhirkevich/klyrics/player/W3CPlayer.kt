package io.github.alexzhirkevich.klyrics.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import org.w3c.dom.Audio
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class W3CPlayer actual constructor(): AudioPlayer {

    private val audio = Audio().apply {
        onplay = {
            this@W3CPlayer._isPlaying = true
            null
        }
        onpause = {
            this@W3CPlayer._isPlaying = false
            null
        }
    }


    override val playback: Flow<Int> = flow {
        while (true) {
            emit((audio.currentTime * 1000).toInt())
            delay(16)
        }
    }.cancellable()

    private var _isPlaying: Boolean by mutableStateOf(!audio.paused)

    override val isPlaying: Boolean
        get() = _isPlaying

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun init(track: ByteArray) {
        audio.src = "data:audio/wav;base64," + Base64.encode(track)
    }

    override suspend fun init(url: String) {
        audio.src = url
    }

    override suspend fun play() {
        if (!isPlaying) {
            audio.play().await()
        }
    }

    override suspend fun pause() {
        audio.pause()
    }

    override suspend fun seek(time: Int) {
        audio.fastSeek(time.toDouble()/1000)
    }
}