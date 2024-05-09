package io.github.alexzhirkevich.klyrics.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile

@OptIn(ExperimentalForeignApi::class)
class IOSPlayer(): AudioPlayer {

    private var avPlayer: AVAudioPlayer? = null

    private val session = AVAudioSession.sharedInstance()

    override val playback: Flow<Int> = flow {
        while (currentCoroutineContext().isActive){
            kotlinx.coroutines.delay(16)
            emit(avPlayer?.currentTime?.times(1000)?.toInt() ?: continue)
        }
    }.flowOn(Dispatchers.IO)


    private var _playing by mutableStateOf(false)

    override val isPlaying: Boolean
        get() = _playing

    override suspend fun init(track: ByteArray) {
        avPlayer = AVAudioPlayer(track.toNSData(), null).apply {
            initPLayer(this)
        }
    }

    override suspend fun init(url: String) {
        avPlayer = AVAudioPlayer(NSURL.URLWithString(url)!!, null).apply {
            initPLayer(this)
        }
    }

    private fun initPLayer(player: AVAudioPlayer){
        player.numberOfLoops = 1000
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun play() {
        withContext(Dispatchers.IO) {
            session
                .setCategory(
                    category = AVAudioSessionCategoryPlayback,
                    error = null
                )
            session.setActive(
                active = true,
                withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                error = null
            )

            _playing = requireNotNull(avPlayer) {
                "Player is not initialized"
            }.play()
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.IO) {
            session.setActive(
                active = false,
                withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                error = null
            )
            _playing = false
        }
    }

    override suspend fun seek(time: Int) {
        requireNotNull(avPlayer) {
            "Player is not initialized"
        }.setCurrentTime(time.toDouble()/1000)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData() : NSData = memScoped {
    NSData.create(
        bytes = allocArrayOf(this@toNSData),
        length = size.toULong()
    )
}
