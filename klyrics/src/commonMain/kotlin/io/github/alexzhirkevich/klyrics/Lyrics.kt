package io.github.alexzhirkevich.klyrics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment


@Immutable
class Lyrics(
    val duration: Int,
    val lanes: List<LyricsLane>,
)

@Immutable
interface LyricsEntry {
    val start : Int
    val end : Int
}

@Immutable
sealed interface LyricsLane : LyricsEntry {

    val content: String

    val words: List<LyricsWord>

    val alignment: Alignment.Horizontal

    @Stable
    fun isFocused(lineIndex: Int, playbackIndex: Int): Boolean

    @Stable
    fun progress(
        from: LyricsWord,
        to: LyricsWord,
        playback: Int
    ): Float

    @Immutable
    data class WordSynced(
        override val start: Int = 0,
        override val end: Int = 0,
        override val alignment: Alignment.Horizontal = Alignment.Start,
        override val words: List<LyricsWord>
    ) : LyricsLane {

        override fun isFocused(index: Int, playbackIndex: Int): Boolean =
            index == playbackIndex
        override fun progress(
            from: LyricsWord,
            to: LyricsWord,
            playback: Int
        ): Float {
            return when {
                from.start > playback || playback !in start until end  -> 0f
                to.end < playback -> 1f
                else -> ((playback - from.start) / (to.end - playback).toFloat())
                    .coerceIn(0f, 1f)
            }
        }

        override val content: String = words.joinToString(separator = " ") {
            it.content.replace(" ","⠀")
        }
    }

    @Immutable
    data class Default(
        override val start: Int = 0,
        override val end: Int = 0,
        override val alignment: Alignment.Horizontal = Alignment.Start,
        override val content: String
    ) : LyricsLane {

        override val words: List<LyricsWord> = content.split(" ").map {
            LyricsWord(content = it)
        }
        override fun isFocused(lineIndex: Int, playbackIndex: Int): Boolean {
           return  lineIndex == playbackIndex
        }

        override fun progress(
            from: LyricsWord,
            to: LyricsWord,
            playback: Int
        ): Float = 1f


    }

    @Immutable
    data class Unsynced(
        override val start: Int = 0,
        override val end: Int = 0,
        override val alignment: Alignment.Horizontal = Alignment.Start,
        override val content: String
    ) : LyricsLane {
        override val words: List<LyricsWord> = content.split(" ").map {
            LyricsWord(content = it)
        }
        override fun isFocused(lineIndex: Int, playbackIndex: Int): Boolean {
            return  true
        }

        override fun progress(
            from: LyricsWord,
            to: LyricsWord,
            playback: Int
        ): Float = 1f


    }
}

/**
 * Lyrics word is a line part between two spaces, including special symbols, commas, etc.
 * */
@Immutable
data class LyricsWord(
    override val start : Int = 0,
    override val end : Int = 0,
    val content : String = ""
) : LyricsEntry

