package io.github.alexzhirkevich.klyrics.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

class MockPlayer(
    initialValue: Int,
    private val duration : Int
): AudioPlayer {

    private val animatable = IntAnimatable(initialValue)

    override val playback: Flow<Int> = snapshotFlow {
        animatable.value
    }

    override val isPlaying: Boolean
        get() = animatable.isRunning

    override suspend fun init(track: ByteArray) {}

    override suspend fun init(uri: String) {
    }

    override suspend fun play() {
        animatable.animateTo(
            targetValue = duration,
            animationSpec = tween(
                duration - animatable.value,
                easing = LinearEasing
            )
        )
    }

    override suspend fun pause() {
        animatable.stop()
    }

    override suspend fun seek(time: Int) {
        val wasPLaying = isPlaying

        animatable.snapTo(time)

        if(wasPLaying){
            play()
        }
    }
}

private fun IntAnimatable(
    initialValue : Int,
) = Animatable(
    initialValue = initialValue,
    typeConverter = TwoWayConverter(
        convertToVector = {
            AnimationVector(it.toFloat())
        },
        convertFromVector = {
            it.value.toInt()
        }
    )
)