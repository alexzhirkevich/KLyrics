import androidx.compose.ui.Alignment
import androidx.compose.ui.util.fastSumBy
import io.github.alexzhirkevich.klyrics.Lyrics
import io.github.alexzhirkevich.klyrics.LyricsLine
import io.github.alexzhirkevich.klyrics.LyricsWord
import klyrics.example.shared.generated.resources.Res
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
suspend fun loadLyrics(
    file : String
) : Lyrics {
    val json = Res.readBytes(file).decodeToString()

    val l = Json.decodeFromString<JsonLyrics>(json)

    return Lyrics(
        duration = l.duration,
        lines = l.lines.map {
            LyricsLine.WordSynced(
                start = it.start,
                end = it.end,//words.last().end,
                alignment = if (it.singer == 1) Alignment.Start else Alignment.End,
                words = it.words.mapIndexed { i, w ->
                    LyricsWord(
                        start = w.start,
                        end = w.end,
                        content = w.content,
                        firstCharIndexInLine = it.words.take(i).fastSumBy { it.content.length } + i,
                        isBackground = w.bg
                    )
                }
            )
        } + LyricsLine.Default(
            start = l.lines.last().words.last().end,
            end = l.duration,
            content = "Alex Zhirkevich\n KLyrics\n Compose Multiplatform"
        )
    )
}

@Serializable
private class JsonLyrics(
    val duration : Int,
    val lines: List<JsonLyricsLine>
)



@Serializable
private class JsonLyricsLine(
    val start : Int,
    val end : Int,
    val singer : Int,
    val words : List<JsonLyricsWord>
)

@Serializable
private class JsonLyricsWord(
    val start : Int,
    val end : Int,
    val content : String,
    val bg : Boolean = false
)