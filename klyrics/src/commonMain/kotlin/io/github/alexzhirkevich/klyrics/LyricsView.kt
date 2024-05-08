package io.github.alexzhirkevich.klyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
class LyricsState(
    val lazyListState: LazyListState
) {
    var currentLine : Int by mutableIntStateOf(0)
        internal set

    var autoscrollEnabled : Boolean by mutableStateOf(true)
        internal set
}

@Composable
fun rememberLyricsState(
    lazyListState: LazyListState = rememberLazyListState()
) : LyricsState {
    return remember(lazyListState) {
        LyricsState(lazyListState)
    }
}

@Composable
fun Lyrics(
    lyrics: Lyrics,
    time : () -> Int,
    textStyle : (Int) -> TextStyle,
    state : LyricsState = rememberLyricsState(),
    lineSpacing : Dp = 12.dp,
    fade: Dp = 24.dp,
    autoscrollDelay : Duration = 3.seconds,
    autoscrollOffset : Dp = 0.dp,
    focusedColor : Color,
    unfocusedColor : Color,
    modifier: Modifier = Modifier,
    lineModifier: (Int) -> Modifier = { Modifier },
) {

    val measurer = rememberTextMeasurer()

    val lines = lyrics.lines

    val autoscrollOffsetPx = LocalDensity.current.run {
        autoscrollOffset.roundToPx()
    }

    var isScrollingProgrammatically by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(state){
        snapshotFlow {
            state.lazyListState.isScrollInProgress
        }.collectLatest {
            if (it && !isScrollingProgrammatically){
                state.autoscrollEnabled = false
            } else {
                delay(autoscrollDelay)
                state.autoscrollEnabled = true
            }
        }
    }

    LaunchedEffect(state){
        snapshotFlow { time() }.collect { t ->
            val current = lines[state.currentLine]
            if (t !in current.start until current.end) {
                state.currentLine = lines.findLane(t) ?: return@collect
            }
        }
    }


    if(state.autoscrollEnabled) {
        LaunchedEffect(state.currentLine, state) {
            try {
                isScrollingProgrammatically = true
                state.lazyListState.animateScrollToItem(state.currentLine, -autoscrollOffsetPx)
            }finally {
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
        state = state.lazyListState
    ) {
        itemsIndexed(lines) { idx, line ->
            val lineModifierImpl = lineModifier(idx)

            when {
                !lyrics.isLaneFocused(
                    lineIndex = idx,
                    playbackIndex = state.currentLine
                ) -> {
                    UnfocusedLine(
                        line = line,
                        style = textStyle(idx),
                        color = unfocusedColor,
                        measurer = measurer,
                        modifier = lineModifierImpl
                    )
                }

                else -> FocusedLine(
                    line = line,
                    time = time,
                    style = textStyle(idx),
                    lyrics = lyrics,
                    fade = fade,
                    focusedSolidBrush = focusedSolidBrush,
                    unfocusedSolidBrush = unfocusedSolidBrush,
                    focusedColor = focusedColor,
                    unfocusedColor = unfocusedColor,
                    measurer = measurer,
                    modifier = lineModifierImpl
                )
            }

            if (idx < lines.lastIndex) {
                Spacer(Modifier.height(lineSpacing))
            }
        }
    }
}

private fun List<LyricsLine>.findLane(ms : Int) : Int? {

    return binarySearchClosest {
        when {
            it.end < ms -> -1
            it.start >= ms -> 1
            else -> 0
        }
    }
}

public fun <T> List<T>.binarySearchClosest(comparison: (T) -> Int): Int {

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
    line: LyricsLine,
    style : TextStyle,
    measurer : TextMeasurer,
    modifier: Modifier,
    draw : CacheDrawScope.(TextLayoutResult) -> DrawResult
) {
    Column(
        modifier = Modifier.fillParentMaxWidth(),
        horizontalAlignment = line.alignment
    ) {
        SubcomposeLayout(
            modifier = modifier
        ) { constraints ->

            println(constraints.maxWidth)
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
                        .drawWithCache { draw(measureResult) }
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
private fun LazyItemScope.UnfocusedLine(
    line: LyricsLine,
    style : TextStyle,
    color : Color,
    measurer : TextMeasurer,
    modifier: Modifier = Modifier,
) {
    Line(
        line = line,
        style = style,
        measurer = measurer,
        modifier = modifier
    ) {
        onDrawBehind {
            drawText(
                textLayoutResult = it,
                color = color
            )
        }
    }
}

@Composable
private fun LazyItemScope.FocusedLine(
    time : () -> Int,
    lyrics: Lyrics,
    line: LyricsLine,
    style : TextStyle,
    fade: Dp,
    focusedSolidBrush : Brush,
    unfocusedSolidBrush : Brush,
    focusedColor : Color,
    unfocusedColor : Color,
    measurer : TextMeasurer,
    modifier: Modifier = Modifier,
) {
    Line(
        line = line,
        style = style,
        measurer = measurer,
        modifier = modifier
    ){ measureResult ->

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
                brush = {
                    val timeMs = time()
                    val subLineStart = line.words[startIndex]
                    val subLineEnd = line.words[startIndex + wordsCount - 1]

                    val progress = lyrics.subLaneProgress(
                        firstWord = subLineStart,
                        lastWord = subLineEnd,
                        playback = timeMs,
                        line = line
                    )

                    when {
                        progress <= 0f -> unfocusedSolidBrush
                        progress >= 1f -> focusedSolidBrush
                        else -> {
                            Brush.horizontalGradient(
                                0f to focusedColor,
                                progress - fade.toPx()/size.width to focusedColor,
                                progress + fade.toPx()/size.width to unfocusedColor,
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
                    brush = l.brush(this)
                )
            }
        }
    }
}


@Immutable
private data class Subline(
    val layout : TextLayoutResult,
    val topLeft : Offset,
    val brush : DrawScope.() -> Brush,
)