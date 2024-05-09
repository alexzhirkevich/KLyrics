package io.github.alexzhirkevich.klyrics.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAudioPlayer(duration : Int): AudioPlayer {
    return remember {
        IOSPlayer()
    }
}