package io.github.alexzhirkevich.klyrics.player

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioTrack
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.FileDescriptor

class AndroidPlayer(
    private val context: Context
) : AudioPlayer {

    override var isPlaying: Boolean by mutableStateOf(false)
        private set

    private val mediaPlayer = MediaPlayer().apply {
        setOnCompletionListener {
            this@AndroidPlayer.isPlaying = it.isPlaying
        }
    }

    override val playback: Flow<Int> = flow {
        while (true){
            if (mediaPlayer.isPlaying) {
                emit(mediaPlayer.currentPosition)
            }
            kotlinx.coroutines.delay(16)
        }
    }.flowOn(Dispatchers.IO).cancellable()


    override suspend fun init(track: ByteArray) {
        TODO("Not yet implemented")
    }

    override suspend fun init(url: String) {
        withContext(Dispatchers.IO) {
            mediaPlayer.setDataSource(context, Uri.parse(url))
            mediaPlayer.prepareAsync()
        }
    }

    override suspend fun play() {
        withContext(Dispatchers.IO) {
            mediaPlayer.start()
            isPlaying = true
        }
    }

    override suspend fun pause() {
        mediaPlayer.pause()
        isPlaying = false
    }

    override suspend fun seek(time: Int) {
        mediaPlayer.seekTo(time)
    }

    internal fun release() {
        kotlin.runCatching {
            mediaPlayer.stop()
        }
        kotlin.runCatching {
            mediaPlayer.release()
        }
    }
}