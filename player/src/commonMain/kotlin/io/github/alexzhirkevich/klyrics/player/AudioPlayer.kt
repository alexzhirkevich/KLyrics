package io.github.alexzhirkevich.klyrics.player

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

interface AudioPlayer {

    val playback : Flow<Int>

    val isPlaying : Boolean

    suspend fun init(track: ByteArray)

    suspend fun init(url: String)

    suspend fun play()

    suspend fun pause()

    suspend fun seek(time : Int)
}

@Composable
expect fun rememberAudioPlayer(duration : Int) : AudioPlayer