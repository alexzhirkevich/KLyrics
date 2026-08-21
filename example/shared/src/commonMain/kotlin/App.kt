
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.klyrics.Lyrics
import io.github.alexzhirkevich.klyrics.LyricsLine
import klyrics.example.shared.generated.resources.Res
import klyrics.example.shared.generated.resources.anti
import klyrics.example.shared.generated.resources.mmlp2
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import kotlin.collections.plus


@OptIn(ExperimentalResourceApi::class)

private const val song = "monster"

@Composable
fun App() {

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {

            val resourceLyrics: Lyrics? by produceState<Lyrics?>(null) {
                value = loadLyrics("files/$song/lyrics.json").let {
                    it.copy(
                        lines = it.lines + LyricsLine.LineSynced(
                            start = it.lines.last().words.last().end,
                            end = it.duration,
                            content = " Alex Zhirkevich\n KLyrics\n Compose Multiplatform"
                        )
                    )
                }
            }

            val lyrics = resourceLyrics

            Crossfade(lyrics) {
                if (it == null) {
                    CircularProgressIndicator(
                        color = LocalContentColor.current.copy(alpha = .5f)
                    )
                } else {
                    val cover = painterResource(Res.drawable.mmlp2)
                    SongScreen(
                        song = remember(it, cover) {
                            Song(
                                lyrics = it,
                                url = Res.getUri("files/$song/audio.mp3"),
                                cover = cover,
                                name = "Monster (feat. Rihanna)",
                                artist = "Eminem"
                            )
                        }
                    )
                }
            }
        }
    }
}
