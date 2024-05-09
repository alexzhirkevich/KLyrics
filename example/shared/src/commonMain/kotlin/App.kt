
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.klyrics.DefaultLyricsIdleIndicator
import io.github.alexzhirkevich.klyrics.Lyrics
import io.github.alexzhirkevich.klyrics.LyricsState
import io.github.alexzhirkevich.klyrics.player.AudioPlayer
import io.github.alexzhirkevich.klyrics.player.MockPlayer
import io.github.alexzhirkevich.klyrics.player.rememberAudioPlayer
import io.github.alexzhirkevich.klyrics.rememberLyricsState
import klyrics.example.shared.generated.resources.Res
import klyrics.example.shared.generated.resources.anti
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration

private val HorizontalPadding = 32.dp

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)


@Composable
fun App() {

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {

        val resourceLyrics: Lyrics? by produceState<Lyrics?>(null) {
            value = loadLyrics("files/desperado/lyrics.json")
        }

        val lyrics = resourceLyrics ?: return@MaterialTheme

        val player = rememberAudioPlayer(lyrics.duration)

//        val player = remember {
//            MockPlayer(0, lyrics.duration)
//        }

        LaunchedEffect(0){
            player.init(
                track = Res.readBytes("files/desperado/audio.mp3"),
            )

//            player.init(
//                url = Res.getUri("files/desperado/audio.mp3"),
//            )
            player.play()
        }

        val scope = rememberCoroutineScope()


        val playback = player.playback.collectAsState(0)

        val lyricsState = rememberLyricsState(lyrics) {
            playback.value
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                LyricsTopBar(
                    cover = painterResource(Res.drawable.anti),
                    name = "Desperado",
                    artist = "Rihanna"
                )
            },
            bottomBar = {
                LyricsBottomBar(
                    player = player,
                    lyricsState = lyricsState,
                    playback = playback.value
                )
            }
        ) {



            val focusedColor = LocalContentColor.current
            val unfocusedColor = focusedColor.copy(alpha = .5f)

            BoxWithConstraints {
                Lyrics(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = lyricsState,
                    lineModifier = { idx ->
                        val line = lyrics.lanes[idx]

                        Modifier.appleMusicLane(
                            state = lyricsState,
                            idx = idx,
                            constraints = constraints,
                            singleArtist = lyrics.lanes.all { it.alignment == Alignment.Start },
                            onClick = {
                                scope.launch {
                                    player.seek(line.start)
                                }
                            }
                        )
                    },
                    focusedColor = focusedColor,
                    unfocusedColor = unfocusedColor,
                    contentPadding = PaddingValues(
                        top = 42.dp + it.calculateTopPadding(),
                        bottom = 20.dp + it.calculateBottomPadding()
                    ),
                    idleIndicator = {
                        DefaultLyricsIdleIndicator(
                            index = it,
                            state = lyricsState,
                            focusedColor = focusedColor,
                            unfocusedColor = unfocusedColor,
                            modifier = Modifier
                                .padding(
                                    horizontal = HorizontalPadding,
                                    vertical = HorizontalPadding/2
                                )
                        )
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsTopBar(
    cover : Painter,
    name : String,
    artist : String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to MaterialTheme.colorScheme.background,
                    .9f to MaterialTheme.colorScheme.background,
                    1f to Color.Transparent
                )
            )
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .padding(horizontal = HorizontalPadding)
            .padding(bottom = 28.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = cover,
            contentDescription = "Cover",
            modifier = Modifier
                .size(72.dp)
                .shadow(elevation = 12.dp)
                .clip(MaterialTheme.shapes.small)
        )

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = LocalContentColor.current.copy(alpha = .5f)
            )
        }
    }
}

@Composable
private fun LyricsBottomBar(
    player: AudioPlayer,
    lyricsState : LyricsState,
    playback : Int,
) {

    val bg = MaterialTheme.colorScheme.background

    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxWidth()
            .drawWithCache {
                val brush = if (lyricsState.isAutoScrolling) {
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        .5f to bg,
                        1f to bg
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to bg
                    )
                }
                onDrawBehind {
                    drawRect(brush)
                }
            }
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    .5f to MaterialTheme.colorScheme.background,
                    1f to MaterialTheme.colorScheme.background
                )
            )
            .navigationBarsPadding()

    ) {
        AnimatedVisibility(
            visible = !lyricsState.isAutoScrolling,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Slider(
                modifier = Modifier.padding(
                    horizontal = HorizontalPadding / 2,
                    vertical = HorizontalPadding / 2
                ),
                value = playback.toFloat(),
                valueRange = 0f..lyricsState.lyrics.duration.toFloat(),
                onValueChange = {
                    scope.launch {
                        player.seek(it.toInt())
                    }
                },

                colors = SliderDefaults.colors(
                    thumbColor = LocalContentColor.current,
                    activeTrackColor = LocalContentColor.current,
                    inactiveTrackColor = LocalContentColor.current.copy(alpha = .5f)
                ),
            )
        }
    }
}

private fun Modifier.appleMusicLane(
    state: LyricsState,
    idx : Int,
    singleArtist : Boolean,
    constraints : Constraints,
    onClick : () -> Unit
) = composed {
    val density = LocalDensity.current

    val blurRadius by animateDpAsState(when {
        !state.isAutoScrolling -> 0.dp
        state.currentLine < idx -> 1.5.dp * (idx - state.currentLine).coerceAtMost(4)
        else -> 3.dp * (state.currentLine - idx).coerceAtMost(4)
    }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    val interactionSource = remember {
        MutableInteractionSource()
    }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        when {
            pressed -> .975f
            state.currentLine == idx -> 1.025f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    // FIXME: width crash

//    widthIn(max = density.run { constraints.maxWidth.toDp() *  3/4})
        padding(
            vertical = 8.dp,
            horizontal = HorizontalPadding/2
        )
        .clip(RoundedCornerShape(HorizontalPadding/2))
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
        .padding(vertical = 6.dp, horizontal = HorizontalPadding/2)
        .blur(
            radius = blurRadius,
            edgeTreatment = BlurredEdgeTreatment.Unbounded
        )
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
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