package io.github.alexzhirkevich.klyrics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment


@Immutable
interface Lyrics {

    val duration: Int

    val lines: List<LyricsLine>

    @Stable
    fun isLaneFocused(lineIndex: Int, playbackIndex: Int): Boolean

    @Stable
    fun subLaneProgress(
        line : LyricsLine,
        firstWord: LyricsWord,
        lastWord: LyricsWord,
        playback: Int
    ): Float


    @Immutable
    class NotSynced(
        override val duration: Int,
        override val lines: List<LyricsLine>
    ) : Lyrics {
        override fun isLaneFocused(lineIndex: Int, playbackIndex: Int): Boolean = true

        override fun subLaneProgress(
            line : LyricsLine,
            firstWord: LyricsWord,
            lastWord: LyricsWord,
            playback: Int
        ): Float = 1f
    }

    @Immutable
    class LineSynced(
        override val duration: Int,
        override val lines: List<LyricsLine>
    ) : Lyrics {
        override fun isLaneFocused(lineIndex: Int, playbackIndex: Int): Boolean =
            lineIndex == playbackIndex

        override fun subLaneProgress(
            line : LyricsLine,
            firstWord: LyricsWord,
            lastWord: LyricsWord,
            playback: Int
        ): Float = 1f
    }

    @Immutable
    class WordSynced(
        override val duration: Int,
        override val lines: List<LyricsLine>
    ) : Lyrics {

        override fun isLaneFocused(lineIndex: Int, playbackIndex: Int): Boolean =
            lineIndex == playbackIndex

        override fun subLaneProgress(
            line : LyricsLine,
            firstWord: LyricsWord,
            lastWord: LyricsWord,
            playback: Int,
        ): Float {

            return when {
                firstWord.start > playback || playback !in line.start until line.end  -> 0f
                lastWord.end < playback -> 1f
                else -> ((playback - firstWord.start) / (lastWord.end - playback).toFloat())
                    .coerceIn(0f, 1f)
            }
        }
    }
}


@Immutable
interface LyricsEntry {
    val start : Int
    val end : Int
}

@Immutable
sealed interface LyricsLine : LyricsEntry {

    val content: String

    val words: List<LyricsWord>

    val alignment: Alignment.Horizontal

    @Immutable
    data class WordSynced(
        override val start: Int = 0,
        override val end: Int = 0,
        override val alignment: Alignment.Horizontal = Alignment.Start,
        override val words: List<LyricsWord>
    ) : LyricsLine {

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
    ) : LyricsLine {

        override val words: List<LyricsWord> = content.split(" ").map {
            LyricsWord(content = it)
        }
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

