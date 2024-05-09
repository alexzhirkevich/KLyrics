package io.github.alexzhirkevich.klyrics.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

//@Composable
//actual fun rememberAudioPlayer(duration : Int): AudioPlayer {
//    val context = LocalContext.current
//
//    return remember {
//        AndroidPlayer(context)
//    }.also {
//        DisposableEffect(it){
//            onDispose {
//                it.release()
//            }
//        }
//    }
//}

@Composable
actual fun rememberAudioPlayer(duration : Int): AudioPlayer {
    return remember {
        MockPlayer(0,duration)
    }
}