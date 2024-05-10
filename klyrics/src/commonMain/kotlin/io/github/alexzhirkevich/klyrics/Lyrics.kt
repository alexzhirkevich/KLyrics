package io.github.alexzhirkevich.klyrics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment


@Immutable
class Lyrics(
    val duration: Int,
    val lines: List<LyricsLine>,
)

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

    @Stable
    fun isFocused(playback: Int): Boolean

    @Stable
    fun progress(
        word: Int,
        playback: Int
    ): Float

    @Immutable
    data class WordSynced(
        override val start: Int = 0,
        override val end: Int = 0,
        override val alignment: Alignment.Horizontal = Alignment.Start,
        override val words: List<LyricsWord>,
    ) : LyricsLine {

        override fun isFocused(playback: Int): Boolean {
            return playback in start..end
        }

        override fun progress(word: Int, playback: Int): Float {
            val w = words[word]

            return when {
                playback !in start..end -> 0f
                w.start > playback -> 0f
                w.end < playback -> 1f
                else -> ((playback - w.start) / (w.end - playback).toFloat())
                    .coerceIn(0f, 1f)
            }
        }

        override val content: String = words.joinToString(separator = " ") {
            it.content
        }
    }

    @Immutable
    data class Default(
        override val start: Int = 0,
        override val end: Int = 0,
        override val alignment: Alignment.Horizontal = Alignment.Start,
        override val content: String
    ) : LyricsLine {

        override val words: List<LyricsWord>

        init {
            var sum = 0

            words = content.split(" ").map {
                LyricsWord(content = it, firstCharIndexInLine = sum).also {
                    sum += it.content.length + 1
                }
            }
        }

        override fun isFocused(playback: Int): Boolean {
            return playback in start..end
        }

        override fun progress(word: Int, playback: Int): Float = 1f
    }

    @Immutable
    data class Unsynced(
        override val start: Int = 0,
        override val end: Int = 0,
        override val alignment: Alignment.Horizontal = Alignment.Start,
        override val content: String
    ) : LyricsLine {

        override val words: List<LyricsWord>

        init {
            var sum = 0

            words = content.split(" ").map {
                LyricsWord(content = it, firstCharIndexInLine = sum).also {
                    sum += it.content.length + 1
                }
            }
        }

        override fun isFocused(playback: Int): Boolean {
            return true
        }
        override fun progress(word: Int, playback: Int): Float = 1f
    }
}

/**
 * Lyrics word is a line part between two spaces, including special symbols, commas, etc.
 * */
@Immutable
data class LyricsWord(
    override val start : Int = 0,
    override val end : Int = 0,
    val content : String = "",
    val firstCharIndexInLine : Int,
    val isBackground : Boolean = false
) : LyricsEntry

