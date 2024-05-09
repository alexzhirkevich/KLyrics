package io.github.alexzhirkevich.klyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.createAnimation
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Stable
class LyricsState(
    val lyrics: Lyrics,
    val lazyListState: LazyListState,
    internal val isPlaying: () -> Boolean,
    internal val playbackTime: () -> Int,
) {
    var currentLine : Int by mutableIntStateOf(-1)
        internal set

    var isAutoScrolling : Boolean by mutableStateOf(isPlaying())
        internal set
}

@Composable
fun rememberLyricsState(
    lyrics: Lyrics,
    isPlaying : () -> Boolean,
    lazyListState: LazyListState = rememberLazyListState(),
    playbackTime : () -> Int,
) : LyricsState {

    val updatedTime by rememberUpdatedState {
        playbackTime()
    }

    val updatedIsPlaying by rememberUpdatedState {
        isPlaying()
    }

    return remember(lyrics, lazyListState) {
        LyricsState(
            lyrics = lyrics,
            lazyListState = lazyListState,
            isPlaying = {
                updatedIsPlaying()
            }
        ){
            updatedTime()
        }
    }
}

enum class AutoscrollMode {
    Disabled,
    Docked,
    Continuous
}



@Composable
fun Lyrics(
    state : LyricsState,
    focusedColor : Color,
    unfocusedColor : Color,
    textStyle : (Int) -> TextStyle = {
        if (state.lyrics.lanes[it].alignment == Alignment.End)
            LyricsDefaults.TextStyleEndAligned
        else LyricsDefaults.TextStyleEndAligned
    },
    fade: Dp = LyricsDefaults.Fade,
    autoscrollMode: AutoscrollMode = LyricsDefaults.AutoScrollMode,
    autoscrollDelay : Duration = LyricsDefaults.AutoscrollDelay,
    autoscrollAnimationSpec : FiniteAnimationSpec<Float> = LyricsDefaults.AutoScrollAnimation,
    focusColorAnimationSpec : FiniteAnimationSpec<Color>? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    lineModifier: (Int) -> Modifier = { Modifier },
    idleIndicator : @Composable (Int) -> Unit = {
        DefaultLyricsIdleIndicator(
            index = it,
            state = state,
            focusedColor = focusedColor,
            unfocusedColor = unfocusedColor,
        )
    }
) {

    val measurer = rememberTextMeasurer(
        cacheSize = 30
    )

    val lines = state.lyrics.lanes

    var isScrollingProgrammatically by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(state) {
        snapshotFlow {
            state.lazyListState.isScrollInProgress &&
                    !isScrollingProgrammatically ||
                    !state.isPlaying()
        }.collectLatest {
            if (it) {
                state.isAutoScrolling = false
            } else {
                delay(autoscrollDelay)
                state.isAutoScrolling = true
            }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.playbackTime() }.collect { t ->

            if (state.currentLine in lines.indices) {
                val current = lines[state.currentLine]
                if (t !in current.start until current.end) {
                    state.currentLine = lines.findLane(t) ?: -1
                }
            } else {
                state.currentLine = lines.findLane(t) ?: -1
            }
        }
    }

    if (autoscrollMode != AutoscrollMode.Disabled && state.isAutoScrolling) {

        var lastIndex by remember {
            mutableStateOf(state.currentLine)
        }

        LaunchedEffect(state.currentLine, state) {
            try {
                isScrollingProgrammatically = true

                val lastItem = state.lazyListState.layoutInfo.visibleItemsInfo.find {
                    it.index == lastIndex
                }

                val scrollToItem =  state.lazyListState.layoutInfo.visibleItemsInfo.find {
                    it.index == state.currentLine
                }

                if (lastItem != null && scrollToItem != null && scrollToItem.offset >= 0){
                    val diff = if (autoscrollMode == AutoscrollMode.Continuous)
                            (scrollToItem.offset - lastItem.offset).toFloat()
                    else scrollToItem.offset.toFloat()

                    state.lazyListState.animateScrollBy(
                        value = diff,
                        animationSpec = autoscrollAnimationSpec
                    )
                } else {
                    state.lazyListState.animateScrollToItem(state.currentLine)
                }
                lastIndex = state.currentLine
            } finally {
                isScrollingProgrammatically = false
            }
        }
    }

    val focusedSolidBrush = remember(focusedColor) {
        SolidColor(focusedColor)
    }

    val unfocusedSolidBrush = remember(unfocusedColor) {
        SolidColor(unfocusedColor)
    }

    LazyColumn(
        modifier = modifier,
        state = state.lazyListState,
        contentPadding = contentPadding
    ) {
        itemsIndexed(lines) { idx, line ->
            idleIndicator(idx)

            LyricsLaneView(
                state = state,
                line = line,
                index = idx,
                style = textStyle(idx),
                fade = fade,
                focusAnimation = focusColorAnimationSpec,
                focusedSolidBrush = focusedSolidBrush,
                unfocusedSolidBrush = unfocusedSolidBrush,
                focusedColor = focusedColor,
                unfocusedColor = unfocusedColor,
                measurer = measurer,
                modifier = lineModifier(idx)
            )
        }
    }
}

@Stable
object LyricsDefaults {

    val TextStyle = TextStyle(
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold
    ).copy(
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

    val TextStyleEndAligned = TextStyle.copy(
        textAlign = TextAlign.End
    )

    val Fade = 32.dp

    val AutoscrollDelay = 3.seconds
    val AutoScrollMode = AutoscrollMode.Docked
    val AutoScrollAnimation = spring<Float>(stiffness = Spring.StiffnessMediumLow)

    @Composable
    fun IdleIndicator(
        index: Int,
        state: LyricsState,
        focusedColor: Color,
        unfocusedColor: Color,
        radius : Dp = 8.dp,
        spacing : Dp = radius,
        enter: EnterTransition = DefaultIdleEnterTransition,
        exit: ExitTransition = DefaultIdleExitTransition,
        modifier: Modifier = Modifier
    ) {
        DefaultLyricsIdleIndicator(
            index = index,
            state = state,
            focusedColor = focusedColor,
            unfocusedColor = unfocusedColor,
            radius = radius,
            spacing = spacing,
            enter = enter,
            exit = exit,
            modifier = modifier
        )
    }
}


@Composable
private fun DefaultLyricsIdleIndicator(
    index: Int,
    state: LyricsState,
    focusedColor: Color,
    unfocusedColor: Color,
    radius : Dp = 8.dp,
    spacing : Dp = radius,
    enter: EnterTransition = DefaultIdleEnterTransition,
    exit: ExitTransition = DefaultIdleExitTransition,
    modifier: Modifier = Modifier
) {
    val currentLineIdx = index

    val startTime = if (currentLineIdx == 0) 0 else state.lyrics.lanes[currentLineIdx - 1].end
    val endTime = state.lyrics.lanes[currentLineIdx].start

    val visible by remember(state) {
        derivedStateOf {
            val t = state.playbackTime()
            t < endTime &&
                    state.currentLine == index &&
                    endTime - startTime > LyricsIdleIndicatorMinDuration ||
                    index == 0 && t < state.lyrics.lanes[0].start
        }
    }
    val circleDuration = (endTime - startTime) / 3

    val totalDuration = (endTime - startTime)
    val durationWithoutEnterExit = (totalDuration - IdleIn - IdleOut)

    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit
    ) {

        val scaleKeyframes = remember(totalDuration, durationWithoutEnterExit) {
            keyframes {
                durationMillis = totalDuration
                1f at 0
                1f at IdleIn
                IdleScaleMax at IdleIn + durationWithoutEnterExit * 1 / 3
                IdleScaleMin at IdleIn + durationWithoutEnterExit * 2 / 3
//                        1f at circlesEnd
                IdleScaleMax at IdleIn + durationWithoutEnterExit

                repeat(EasingSteps) {
                    val f = it.toFloat() / EasingSteps
                    1.15f * (1f - Easing.transform(f)) at ((IdleIn + durationWithoutEnterExit) +
                            IdleOut / EasingSteps * it)
                }

                0f at totalDuration
            }.vectorize(Float.VectorConverter).createAnimation(
                initialValue = AnimationVector1D(1f),
                targetValue = AnimationVector1D(1f),
                initialVelocity = AnimationVector1D(0f)
            )
        }

        val alphaKeyframes = remember(totalDuration) {
            keyframes {
                durationMillis = totalDuration
                1f at 0
                1f at totalDuration - IdleOut
                0f at totalDuration
            }.vectorize(Float.VectorConverter).createAnimation(
                initialValue = AnimationVector1D(0f),
                targetValue = AnimationVector1D(0f),
                initialVelocity = AnimationVector1D(0f)
            )
        }

        Spacer(
            modifier
                .width(radius * 6 + spacing * 2)
                .height(radius * 2)
                .drawWithCache {

                    val spacingPx = spacing.toPx()

                    val radiusPx = radius.toPx()
                    val diameter = radiusPx * 2

                    val centers = List(3) {
                        Offset(
                            x = radiusPx + spacingPx * it + diameter * it,
                            y = radiusPx
                        )
                    }

//                    val circlesEnd = ((IdleScaleMax - IdleScaleMin / (durationWithoutEnterExit/3)) * (IdleScaleMax - 1)).toInt()

                    onDrawBehind {
                        val time = state.playbackTime()

                        val nanoTimeElapsed = (time - startTime).milliseconds.inWholeNanoseconds

                        val scale = scaleKeyframes.getValueFromNanos(nanoTimeElapsed).value
                        val alpha = alphaKeyframes.getValueFromNanos(nanoTimeElapsed).value

                        scale(scale) {
                            repeat(3) {
                                val circleProgress = if (time < startTime + circleDuration * it) {
                                    0f
                                } else {
                                    ((time.toFloat() - startTime - circleDuration * it) / circleDuration)
                                        .coerceIn(0f, 1f)
                                }

                                drawCircle(
                                    color = lerp(
                                        unfocusedColor, focusedColor, circleProgress
                                    ),
                                    center = centers[it],
                                    radius = radiusPx,
                                    alpha = alpha
                                )
                            }
                        }
                    }
                }
        )
    }
}

private val Easing = FastOutLinearInEasing
private val EasingSteps = 10
private const val IdleIn = 1000
private const val IdleOut = 300
private const val IdleScaleMax = 1.15f
private const val IdleScaleMin = 0.8f
private const val LyricsIdleIndicatorMinDuration = 7000

private val DefaultSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)
private val DefaultSpringSize = spring<IntSize>(stiffness = Spring.StiffnessMediumLow)

private val DefaultIdleEnterTransition = expandVertically(
    animationSpec = DefaultSpringSize
) + fadeIn(DefaultSpring)


private val DefaultIdleExitTransition = shrinkVertically(
    animationSpec =  spring(stiffness = Spring.StiffnessMediumLow)
)

private fun List<LyricsLane>.findLane(ms : Int) : Int {

    return binarySearchClosest {
        when {
            it.end < ms -> -1
            it.start >= ms -> 1
            else -> 0
        }
    }
}

private fun <T> List<T>.binarySearchClosest(comparison: (T) -> Int): Int {

    var low = 0
    var high = lastIndex

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = comparison(midVal)

        if (cmp < 0)
            low = mid + 1
        else if (cmp > 0)
            high = mid - 1
        else
            return mid // key found
    }
    return low
}

@Composable
private fun LazyItemScope.Line(
    line: LyricsLane,
    style : TextStyle,
    measurer : TextMeasurer,
    modifier: Modifier,
    draw : CacheDrawScope.(Constraints, TextLayoutResult) -> DrawResult
) {
    Column(
        modifier = Modifier.fillParentMaxWidth(),
        horizontalAlignment = line.alignment
    ) {
        SubcomposeLayout(
            modifier = modifier
        ) { constraints ->

            val measureResult = measurer.measure(
                text = line.content,
                style = style,
                constraints = Constraints(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth
                )
            )

            val height = (style.lineHeight * measureResult.lineCount)

            val width = (0 until measureResult.lineCount).maxOf {
                measureResult.getBoundingBox(
                    measureResult.getLineEnd(it, visibleEnd = true) - 1
                ).right
            }

            val content = subcompose(null) {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .drawWithCache { draw(constraints, measureResult) }
                )
            }.first()


            val placeable = content.measure(
                Constraints.fixed(width.roundToInt(), height.roundToPx())
            )

            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}

@Composable
private fun LazyItemScope.LyricsLaneView(
    state: LyricsState,
    line: LyricsLane,
    index : Int,
    style : TextStyle,
    fade: Dp,
    focusedSolidBrush : Brush,
    unfocusedSolidBrush : Brush,
    focusedColor : Color,
    unfocusedColor : Color,
    focusAnimation : FiniteAnimationSpec<Color>?,
    measurer : TextMeasurer,
    modifier: Modifier = Modifier,
) {

    val unfocused by remember {
        derivedStateOf {
            index !in state.lyrics.lanes.indices || !line.isFocused(index, state.currentLine)
        }
    }


    val animatedFocusColor = if (focusAnimation != null) {
        animateColorAsState(
            targetValue = if (unfocused) unfocusedColor else focusedColor,
            animationSpec = focusAnimation
        ).value
    } else {
        unfocusedColor
    }

    Line(
        line = line,
        style = style,
        measurer = measurer,
        modifier = modifier
    ){parentConstraints, measureResult ->

        if (unfocused){
            return@Line  onDrawBehind {
                drawText(
                    textLayoutResult = measureResult,
                    color = animatedFocusColor
                )
            }
        }

        var wordsPlaced = 0

        val lines = List(measureResult.lineCount) {

            val startIndex = wordsPlaced

            val lineStart = measureResult.getLineStart(it)
            val lineEnd = measureResult.getLineEnd(it)

            val content = line.content.substring(lineStart, lineEnd).trim()

            val wordsCount = content.count { it == ' ' } + 1

            Subline(
                layout = measurer.measure(
                    text = content,
                    style = style,
                    constraints = measureResult.layoutInput.constraints
                ),
                topLeft = Offset(measureResult.getLineLeft(it), measureResult.getLineTop(it)),
                brush = { w ->

                    val endIndex = startIndex + wordsCount - 1

                    if (startIndex !in line.words.indices || endIndex !in line.words.indices){
                        return@Subline unfocusedSolidBrush
                    }

                    val timeMs = state.playbackTime()
                    val subLineStart = line.words[startIndex]
                    val subLineEnd = line.words[endIndex]

                    val progress = line.progress(
                        from = subLineStart,
                        to = subLineEnd,
                        playback = timeMs,
                    )

                    when {
                        progress <= 0f -> unfocusedSolidBrush
                        progress >= 1f -> focusedSolidBrush
                        else -> {
                            Brush.horizontalGradient(
                                0f to focusedColor,
                                progress - fade.toPx()/w to focusedColor,
                                progress + fade.toPx()/w to unfocusedColor,
                                1f  to unfocusedColor
                            )
                        }
                    }
                }
            ).also {
                wordsPlaced += wordsCount
            }
        }

        onDrawBehind {
            lines.fastForEach { l ->
                drawText(
                    textLayoutResult = l.layout,
                    topLeft = l.topLeft,
                    brush = l.brush(parentConstraints.maxWidth)
                )
            }
        }
    }
}



@Immutable
private data class Subline(
    val layout : TextLayoutResult,
    val topLeft : Offset,
    val brush : (width : Int) -> Brush,
)