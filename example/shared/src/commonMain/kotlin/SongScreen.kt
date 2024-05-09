import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.klyrics.Lyrics
import io.github.alexzhirkevich.klyrics.LyricsDefaults
import io.github.alexzhirkevich.klyrics.LyricsState
import io.github.alexzhirkevich.klyrics.player.AudioPlayer
import io.github.alexzhirkevich.klyrics.player.rememberAudioPlayer
import io.github.alexzhirkevich.klyrics.rememberLyricsState
import klyrics.example.shared.generated.resources.Res
import klyrics.example.shared.generated.resources.anti
import klyrics.example.shared.generated.resources.cmp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Immutable
data class Song(
    val lyrics: Lyrics,
    val url : String,
    val name : String,
    val artist: String
)

@Composable
fun SongScreen(
    song: Song
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {

        val player = rememberAudioPlayer(song.lyrics.duration)

        val scope = rememberCoroutineScope()

        val playback = player.playback.collectAsState(0)

        val lyricsState = rememberLyricsState(
            lyrics = song.lyrics,
            isPlaying = {
                player.isPlaying
            }
        ) {
            playback.value
        }

        val focus = remember { FocusRequester() }

        LaunchedEffect(0) {
            player.init(uri = song.url)
        }

        LaunchedEffect(0) {
            delay(500)
            focus.requestFocus()
        }

        Scaffold(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .focusRequester(focus)
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Spacebar) {
                        scope.launch {
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                        }
                        true
                    } else false
                },
            topBar = {
                LyricsTopBar(
                    cover = painterResource(Res.drawable.anti),
                    name = song.name,
                    artist = song.artist
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

            // the same as LocalContentColor.current.copy(alpha = .5f) but alpha blending is buggy on Android
            val unfocusedColor = lerp(LocalContentColor.current, MaterialTheme.colorScheme.background, .5f)

            val lastLaneStyle = MaterialTheme.typography.titleLarge

            BoxWithConstraints {
                Lyrics(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = lyricsState,
                    textStyle = {
                        when {
                            it == song.lyrics.lanes.lastIndex -> lastLaneStyle
                            song.lyrics.lanes[it].alignment == Alignment.End -> LyricsDefaults.TextStyleEndAligned
                            else -> LyricsDefaults.TextStyle
                        }
                    },
                    lineModifier = { idx ->
                        val line = song.lyrics.lanes[idx]

                        Modifier.appleMusicLane(
                            state = lyricsState,
                            idx = idx,
                            isAnnotation = idx == song.lyrics.lanes.lastIndex,
                            constraints = constraints,
                            singleArtist = song.lyrics.lanes.all { it.alignment == Alignment.Start },
                            onClick = {
                                scope.launch {
                                    player.seek(line.start)
                                    if (!player.isPlaying) {
                                        player.play()
                                    }
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
                        LyricsDefaults.IdleIndicator(
                            index = it,
                            state = lyricsState,
                            focusedColor = focusedColor,
                            unfocusedColor = unfocusedColor,
                            modifier = Modifier
                                .padding(
                                    horizontal = HorizontalPadding / 2 ,
                                    vertical = VerticalPadding
                                )
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(
                                    onClick = {
                                        scope.launch {
                                            player.seek(
                                                if (it == 0) 0 else lyricsState.lyrics.lanes[it-1].end + 1
                                            )
                                        }
                                    }
                                )
                                .padding(
                                    horizontal = HorizontalPadding /2,
                                    vertical = HorizontalPadding / 2
                                )
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = LocalContentColor.current.copy(alpha = .5f)
            )
        }

        Spacer(Modifier.weight(1f))

        val uriHandler = LocalUriHandler.current

        Icon(
            painter = painterResource(Res.drawable.cmp),
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    uriHandler.openUri("https://github.com/alexzhirkevich/klyrics")
                }
                .background(LocalContentColor.current.copy(alpha = .1f))
                .padding(VerticalPadding)
                .size(20.dp)
        )
    }
}

private val HorizontalPadding = 32.dp
private val VerticalPadding = 6.dp


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
            enter = fadeIn() + slideInVertically { it / 2 } + expandVertically(),
            exit = fadeOut() + slideOutVertically { it / 2 } + shrinkVertically(),
            modifier = Modifier.padding(
                horizontal = HorizontalPadding / 2
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                FilledIconButton(
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = LocalContentColor.current.copy(
                            alpha = .1f
                        ),
                        contentColor = LocalContentColor.current
                    ),
                    onClick = {
                        scope.launch {
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (player.isPlaying)
                            Icons.Rounded.Pause
                        else Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                }

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
}

private fun Modifier.appleMusicLane(
    state: LyricsState,
    idx : Int,
    isAnnotation : Boolean,
    singleArtist : Boolean,
    constraints : Constraints,
    onClick : () -> Unit
) = composed {
    val density = LocalDensity.current

    val blurRadius by animateDpAsState(
        when {
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
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )


    val alpha by animateFloatAsState(
        if (!isAnnotation || idx == state.currentLine) 1f else 0f
    )

    // FIXME: width crash
    widthIn(max = density.run { constraints.maxWidth.toDp() * if (singleArtist) 1f else 2 / 3f })
    padding(
        vertical = 8.dp,
        horizontal = HorizontalPadding / 2
    )
        .clip(RoundedCornerShape(HorizontalPadding / 2))
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
            enabled = !isAnnotation
        )
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .padding(
            vertical = VerticalPadding,
            horizontal = HorizontalPadding / 2
        )
        .blur(
            radius = blurRadius,
            edgeTreatment = BlurredEdgeTreatment.Unbounded
        )

}