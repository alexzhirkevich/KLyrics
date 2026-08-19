package io.github.alexzhirkevich.klyrics.player

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayer as Player
import io.github.kdroidfilter.composemediaplayer.audio.isPlaying

private class AudioPlayerImpl : AudioPlayer {

    private val player = Player()

    override val playback: Flow<Int> = flow {
        while (true){
            withFrameMillis {}
            emit(player.currentPosition()?.toInt() ?: 0)
            isPlaying = player.isPlaying()
        }
    }

    override var isPlaying: Boolean by mutableStateOf(false)
        private set

    override suspend fun init(track: ByteArray) {
        TODO("Not yet implemented")
    }

    override suspend fun init(uri: String) {
        player.play(uri)
        player.stop()
    }

    override suspend fun play() {
        isPlaying = true
        player.play()
    }

    override suspend fun pause() {
        isPlaying = false
        player.pause()
    }

    override suspend fun seek(time: Int) {
        player.seekTo(time.toLong())
    }
}


@Composable
fun rememberAudioPlayer2(duration : Int) : AudioPlayer {
    return remember { AudioPlayerImpl() }
}