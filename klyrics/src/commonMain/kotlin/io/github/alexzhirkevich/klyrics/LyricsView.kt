package io.github.alexzhirkevich.klyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlin.math.roundToInt

@Composable
fun Lyrics(
    lyrics: Lyrics,
    time : () -> Int,
    style : TextStyle,
    lineSpacing : Dp = 12.dp,
    focusedColor : Color,
    unfocusedColor : Color,
    state : LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    lineModifier: (LyricsLine) -> Modifier = { Modifier },
) {

    val style = style.copy(
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

    val measurer = rememberTextMeasurer()

    val lines = lyrics.lines

    val initialLine = remember {
        lines.findLane(time())
    }

    val currentLineIndex by produceState(initialLine) {
        snapshotFlow { time() }.collect { t ->
            val current = lines[value]
            if (t !in current.start until current.end) {
                value = lines.findLane(t)
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
        state = state
    ) {
        itemsIndexed(lines) { idx, line ->
            val lineModifierImpl = lineModifier(line)

            when {
                currentLineIndex < 0 || !lyrics.isLaneFocused(
                    lineIndex = idx,
                    playbackIndex = currentLineIndex
                ) -> {
                    UnfocusedLine(
                        line = line,
                        style = style,
                        color = unfocusedColor,
                        measurer = measurer,
                        modifier = lineModifierImpl
                    )
                }

                else -> FocusedLine(
                    line = line,
                    time = time,
                    style = style,
                    lyrics = lyrics,
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

private fun List<LyricsLine>.findLane(ms : Int) : Int {
    return binarySearch {
        when {
            it.end <= ms -> -1
            it.start > ms -> 1
            else -> 0
        }
    }.coerceIn(indices)
}

@Composable
private fun LazyItemScope.Line(
    line: LyricsLine,
    style : TextStyle,
    measurer : TextMeasurer,
    modifier: Modifier,
    draw : CacheDrawScope.(TextLayoutResult) -> DrawResult
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
                    .drawWithCache { draw(measureResult) }
            )
        }.first()


        val placeable = content.measure(
            Constraints.fixed(width.roundToInt(), height.roundToPx())
        )

        val x = line.alignment.align(placeable.width, constraints.maxWidth, layoutDirection)

        layout(placeable.width, placeable.height) {
            placeable.place(x,0)
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
    Line(line, style, measurer, modifier) {
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
                layout = measurer.measure(content, style),
                topLeft = Offset(0f, style.lineHeight.toPx() * it),
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
                            Brush.linearGradient(
                                0f to focusedColor,
                                progress to focusedColor,
                                progress to unfocusedColor,
                                1f to unfocusedColor
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
                    brush = l.brush()
                )
            }
        }
    }
}


@Immutable
private data class Subline(
    val layout : TextLayoutResult,
    val topLeft : Offset,
    val brush : () -> Brush,
)