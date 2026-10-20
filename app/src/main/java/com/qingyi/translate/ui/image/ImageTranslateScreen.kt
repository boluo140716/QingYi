package com.qingyi.translate.ui.image

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyi.translate.ui.AppUiState
import com.qingyi.translate.ui.AppViewModel
import com.qingyi.translate.ui.ImageUi
import com.qingyi.translate.ui.Screen
import com.qingyi.translate.ui.theme.QingAccent
import com.qingyi.translate.ui.theme.QingBg
import com.qingyi.translate.ui.theme.QingCard
import com.qingyi.translate.ui.theme.QingMuted
import com.qingyi.translate.ui.theme.QingOnSurface
import com.qingyi.translate.ui.uriOrNull
import com.qingyi.translate.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class DragMode {
    None,
    Move,
    Top,
    Bottom,
    Left,
    Right,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

@Composable
fun ImageTranslateScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onPickImage: () -> Unit,
) {
    val uri = state.image.uriOrNull
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(uri) { mutableStateOf(false) }
    var selection by remember(uri) { mutableStateOf(Rect(0f, 0f, 1f, 1f)) }

    LaunchedEffect(uri) {
        if (uri == null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) { BitmapUtils.load(context, uri) }
        bitmap = loaded
        loadFailed = loaded == null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QingBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.go(Screen.TRANSLATE) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = QingOnSurface,
                )
            }
            Text(
                text = "图片翻译",
                color = QingOnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPickImage) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = "换一张",
                    tint = QingOnSurface,
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
        ) {
            val bmp = bitmap
            if (bmp == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (loadFailed) {
                        Text(
                            text = "图片读取失败，换一张试试",
                            color = Color.White,
                            fontSize = 14.sp,
                        )
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                return@BoxWithConstraints
            }

            val density = LocalDensity.current
            val containerW = with(density) { maxWidth.toPx() }
            val containerH = with(density) { maxHeight.toPx() }
            val scale = min(containerW / bmp.width, containerH / bmp.height)
            val drawW = bmp.width * scale
            val drawH = bmp.height * scale
            val drawLeft = (containerW - drawW) / 2f
            val drawTop = (containerH - drawH) / 2f
            val displayRect = Rect(drawLeft, drawTop, drawLeft + drawW, drawTop + drawH)

            val locked = state.image is ImageUi.Success
            val selectionRect = selection.toDisplay(displayRect)

            val haptic = LocalHapticFeedback.current

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(displayRect, locked) {
                        if (locked) return@pointerInput

                        val cornerSlop = 52.dp.toPx()
                        val edgeSlop = 44.dp.toPx()

                        fun normX(x: Float) = ((x - displayRect.left) / displayRect.width).coerceIn(0f, 1f)
                        fun normY(y: Float) = ((y - displayRect.top) / displayRect.height).coerceIn(0f, 1f)

                        fun near(pos: Offset, x: Float, y: Float) =
                            (pos - Offset(x, y)).getDistance() < cornerSlop

                        // No touch slop: the handle reacts from the first pixel, which
                        // is what makes native crop UIs feel immediate.
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val start = down.position
                            val r = selection.toDisplay(displayRect)

                            val mode = when {
                                // Corners win over edges so the 8 handles never fight.
                                near(start, r.left, r.top) -> DragMode.TopLeft
                                near(start, r.right, r.top) -> DragMode.TopRight
                                near(start, r.left, r.bottom) -> DragMode.BottomLeft
                                near(start, r.right, r.bottom) -> DragMode.BottomRight

                                abs(start.y - r.top) < edgeSlop &&
                                    start.x >= r.left - edgeSlop &&
                                    start.x <= r.right + edgeSlop -> DragMode.Top

                                abs(start.y - r.bottom) < edgeSlop &&
                                    start.x >= r.left - edgeSlop &&
                                    start.x <= r.right + edgeSlop -> DragMode.Bottom

                                abs(start.x - r.left) < edgeSlop &&
                                    start.y >= r.top - edgeSlop &&
                                    start.y <= r.bottom + edgeSlop -> DragMode.Left

                                abs(start.x - r.right) < edgeSlop &&
                                    start.y >= r.top - edgeSlop &&
                                    start.y <= r.bottom + edgeSlop -> DragMode.Right

                                // Only the middle of the box moves it, never an edge.
                                r.contains(start) -> DragMode.Move
                                else -> DragMode.None
                            }

                            if (mode == DragMode.None) return@awaitEachGesture
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break

                                val pos = change.position
                                val delta = change.positionChange()
                                val minSize = 0.06f

                                selection = when (mode) {
                                    DragMode.Move -> {
                                        val w = selection.width
                                        val h = selection.height
                                        val nl = (selection.left + delta.x / displayRect.width)
                                            .coerceIn(0f, 1f - w)
                                        val nt = (selection.top + delta.y / displayRect.height)
                                            .coerceIn(0f, 1f - h)
                                        Rect(nl, nt, nl + w, nt + h)
                                    }

                                    DragMode.Top -> Rect(
                                        selection.left,
                                        normY(pos.y).coerceAtMost(selection.bottom - minSize),
                                        selection.right,
                                        selection.bottom,
                                    )

                                    DragMode.Bottom -> Rect(
                                        selection.left,
                                        selection.top,
                                        selection.right,
                                        normY(pos.y).coerceAtLeast(selection.top + minSize),
                                    )

                                    DragMode.Left -> Rect(
                                        normX(pos.x).coerceAtMost(selection.right - minSize),
                                        selection.top,
                                        selection.right,
                                        selection.bottom,
                                    )

                                    DragMode.Right -> Rect(
                                        selection.left,
                                        selection.top,
                                        normX(pos.x).coerceAtLeast(selection.left + minSize),
                                        selection.bottom,
                                    )

                                    DragMode.TopLeft -> Rect(
                                        normX(pos.x).coerceAtMost(selection.right - minSize),
                                        normY(pos.y).coerceAtMost(selection.bottom - minSize),
                                        selection.right,
                                        selection.bottom,
                                    )

                                    DragMode.TopRight -> Rect(
                                        selection.left,
                                        normY(pos.y).coerceAtMost(selection.bottom - minSize),
                                        normX(pos.x).coerceAtLeast(selection.left + minSize),
                                        selection.bottom,
                                    )

                                    DragMode.BottomLeft -> Rect(
                                        normX(pos.x).coerceAtMost(selection.right - minSize),
                                        selection.top,
                                        selection.right,
                                        normY(pos.y).coerceAtLeast(selection.top + minSize),
                                    )

                                    DragMode.BottomRight -> Rect(
                                        selection.left,
                                        selection.top,
                                        normX(pos.x).coerceAtLeast(selection.left + minSize),
                                        normY(pos.y).coerceAtLeast(selection.top + minSize),
                                    )

                                    DragMode.None -> selection
                                }

                                change.consume()
                            }
                        }
                    },
            ) {
                drawImage(
                    image = bmp.asImageBitmap(),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bmp.width, bmp.height),
                    dstOffset = IntOffset(drawLeft.roundToInt(), drawTop.roundToInt()),
                    dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )

                if (!locked) {
                    val dim = Color.Black.copy(alpha = 0.45f)
                    drawRect(dim, Offset.Zero, Size(size.width, selectionRect.top))
                    drawRect(
                        dim,
                        Offset(0f, selectionRect.bottom),
                        Size(size.width, size.height - selectionRect.bottom),
                    )
                    drawRect(
                        dim,
                        Offset(0f, selectionRect.top),
                        Size(selectionRect.left, selectionRect.height),
                    )
                    drawRect(
                        dim,
                        Offset(selectionRect.right, selectionRect.top),
                        Size(size.width - selectionRect.right, selectionRect.height),
                    )

                    val l = selectionRect.left
                    val t = selectionRect.top
                    val r = selectionRect.right
                    val b = selectionRect.bottom

                    drawRect(
                        color = Color.White.copy(alpha = 0.7f),
                        topLeft = Offset(l, t),
                        size = Size(selectionRect.width, selectionRect.height),
                        style = Stroke(width = 1.dp.toPx()),
                    )

                    val thick = 4.dp.toPx()
                    val cap = StrokeCap.Round
                    val cornerLenX = min(26.dp.toPx(), selectionRect.width / 3f)
                    val cornerLenY = min(26.dp.toPx(), selectionRect.height / 3f)

                    // L-shaped corner brackets, like a native crop UI.
                    drawLine(Color.White, Offset(l, t), Offset(l + cornerLenX, t), thick, cap)
                    drawLine(Color.White, Offset(l, t), Offset(l, t + cornerLenY), thick, cap)
                    drawLine(Color.White, Offset(r, t), Offset(r - cornerLenX, t), thick, cap)
                    drawLine(Color.White, Offset(r, t), Offset(r, t + cornerLenY), thick, cap)
                    drawLine(Color.White, Offset(l, b), Offset(l + cornerLenX, b), thick, cap)
                    drawLine(Color.White, Offset(l, b), Offset(l, b - cornerLenY), thick, cap)
                    drawLine(Color.White, Offset(r, b), Offset(r - cornerLenX, b), thick, cap)
                    drawLine(Color.White, Offset(r, b), Offset(r, b - cornerLenY), thick, cap)

                    // Thick grips in the middle of each edge.
                    val gripX = min(20.dp.toPx(), selectionRect.width / 4f)
                    val gripY = min(20.dp.toPx(), selectionRect.height / 4f)
                    val cx = (l + r) / 2f
                    val cy = (t + b) / 2f
                    drawLine(Color.White, Offset(cx - gripX, t), Offset(cx + gripX, t), thick, cap)
                    drawLine(Color.White, Offset(cx - gripX, b), Offset(cx + gripX, b), thick, cap)
                    drawLine(Color.White, Offset(l, cy - gripY), Offset(l, cy + gripY), thick, cap)
                    drawLine(Color.White, Offset(r, cy - gripY), Offset(r, cy + gripY), thick, cap)
                }
            }

            val success = state.image as? ImageUi.Success
            when {
                success != null -> {
                    TranslatedOverlay(
                        text = success.translated,
                        rect = selectionRect,
                    )
                }

                state.image is ImageUi.Translating -> {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    selectionRect.left.roundToInt(),
                                    selectionRect.top.roundToInt(),
                                )
                            }
                            .size(
                                with(density) { selectionRect.width.toDp() },
                                with(density) { selectionRect.height.toDp() },
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    }
                }
            }

            if (!locked) {
                Text(
                    text = "拖动或拉伸方框，选择要翻译的区域",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        val success = state.image as? ImageUi.Success
        val loading = state.image is ImageUi.Translating

        if (state.image is ImageUi.NeedKey) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "配置 DeepSeek API Key 后即可翻译",
                    color = QingMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { viewModel.go(Screen.SETTINGS) }) {
                    Text("去设置", color = QingAccent)
                }
            }
        }

        (state.image as? ImageUi.Failure)?.let { failure ->
            Text(
                text = failure.message,
                color = QingMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (success != null) {
                OutlinedButton(
                    onClick = viewModel::dismissImageResult,
                    shape = RoundedCornerShape(50),
                ) {
                    Text("重新框选")
                }

                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(success.translated))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "复制译文",
                        tint = QingOnSurface,
                    )
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { viewModel.openAssistant() },
                    shape = RoundedCornerShape(50),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("问助手")
                }
            } else {
                OutlinedButton(
                    onClick = { selection = Rect(0f, 0f, 1f, 1f) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("全图")
                }

                Button(
                    onClick = {
                        bitmap?.let { full ->
                            viewModel.translateCrop(full, BitmapUtils.crop(full, selection))
                        }
                    },
                    enabled = bitmap != null && !loading,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1.6f),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("翻译选中区域")
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslatedOverlay(text: String, rect: Rect) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .size(
                with(density) { rect.width.toDp() },
                with(density) { rect.height.toDp() },
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = with(density) { (rect.height * 0.8f).toDp() })
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xE61A1A1A))
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

private fun Rect.toDisplay(display: Rect): Rect = Rect(
    display.left + left * display.width,
    display.top + top * display.height,
    display.left + right * display.width,
    display.top + bottom * display.height,
)
