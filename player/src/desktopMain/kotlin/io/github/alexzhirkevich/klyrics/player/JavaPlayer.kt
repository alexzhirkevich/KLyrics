package io.github.alexzhirkevich.klyrics.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javazoom.jl.player.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

class JavaPlayer(): AudioPlayer {

    private var player: Player? = null

    override suspend fun init(track: ByteArray) {
        player = Player(ByteArrayInputStream(track))
    }

    override var playback = MutableSharedFlow<Int>()

    override var isPlaying: Boolean by mutableStateOf(false)
        private set

    override suspend fun play() = coroutineScope {
        val p = requireNotNull(player) {
            "Player is not initialized"
        }
        isPlaying = true
        launch(Dispatchers.IO) {
            p.play()
        }.invokeOnCompletion {
            isPlaying = true
        }

        while (isActive && isPlaying){
            playback.emit(p.position)
            delay(16)
        }
    }
    override suspend fun pause() {
        withContext(Dispatchers.IO) {
            player?.close()
            isPlaying = false
        }
    }

    override suspend fun seek(time: Int) {
        withContext(Dispatchers.IO) {
            player?.play(time)
        }
    }
}

