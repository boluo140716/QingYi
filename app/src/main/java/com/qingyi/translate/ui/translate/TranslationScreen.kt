package com.qingyi.translate.ui.translate

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyi.translate.data.Language
import com.qingyi.translate.ui.AppUiState
import com.qingyi.translate.ui.AppViewModel
import com.qingyi.translate.ui.Screen
import com.qingyi.translate.ui.TranslationUi
import com.qingyi.translate.ui.theme.QingAccent
import com.qingyi.translate.ui.theme.QingBg
import com.qingyi.translate.ui.theme.QingCard
import com.qingyi.translate.ui.theme.QingMuted
import com.qingyi.translate.ui.theme.QingOnSurface
import com.qingyi.translate.ui.theme.QingPill

@Composable
fun TranslationScreen(
    state: AppUiState,
    viewModel: AppViewModel,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QingBg),
    ) {
        TopBar(onSettings = { viewModel.go(Screen.SETTINGS) })

        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize()) {
                InputCard(
                    text = state.inputText,
                    onTextChange = viewModel::onInputChange,
                    onClear = viewModel::clearInput,
                    onTranslate = viewModel::translateNow,
                    isTranslating = state.translation == TranslationUi.Loading,
                    modifier = Modifier.weight(1f),
                )

                if (state.translation != TranslationUi.Idle) {
                    Spacer(Modifier.height(2.dp))
                    ResultCard(
                        target = state.target,
                        state = state.translation,
                        onCopy = { text ->
                            clipboard.setText(AnnotatedString(text))
                            Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                        },
                        onRetry = viewModel::retryTextTranslation,
                        onOpenSettings = { viewModel.go(Screen.SETTINGS) },
                        onAsk = viewModel::openAssistant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        LanguageSelector(
            source = state.source,
            target = state.target,
            onSourceClick = { viewModel.openLanguagePicker(true) },
            onTargetClick = { viewModel.openLanguagePicker(false) },
            onSwapClick = viewModel::swapLanguages,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TopBar(onSettings: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "收藏",
            tint = QingOnSurface,
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    Toast.makeText(context, "收藏功能暂未实现", Toast.LENGTH_SHORT).show()
                },
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "轻译",
                color = QingOnSurface,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "翻译",
                color = QingMuted,
                fontSize = 21.sp,
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(QingAccent)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "译",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun InputCard(
    text: String,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onTranslate: () -> Unit,
    isTranslating: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = QingCard,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 22.dp, end = 24.dp, bottom = 76.dp),
                textStyle = TextStyle(
                    color = QingOnSurface,
                    fontSize = 30.sp,
                    lineHeight = 38.sp,
                ),
                cursorBrush = SolidColor(QingAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        focusManager.clearFocus()
                        onTranslate()
                    }
                ),
            )

            if (text.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 24.dp, top = 22.dp),
                ) {
                    Text(
                        text = "翻译文字",
                        color = QingOnSurface.copy(alpha = 0.72f),
                        fontSize = 32.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                    PasteChip(
                        onClick = {
                            val pasted = clipboard.getText()?.text.orEmpty()
                            if (pasted.isBlank()) {
                                Toast.makeText(context, "剪贴板是空的", Toast.LENGTH_SHORT).show()
                            } else {
                                onTextChange(pasted)
                            }
                        }
                    )
                }
            }

            if (text.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "清空",
                        tint = QingMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Button(
                    onClick = onTranslate,
                    enabled = !isTranslating,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 18.dp),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 26.dp, vertical = 13.dp),
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "翻译",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(QingOnSurface.copy(alpha = 0.22f)),
            )
        }
    }
}

@Composable
private fun PasteChip(onClick: () -> Unit) {
    Surface(
        color = QingPill,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.ContentPaste,
                contentDescription = null,
                tint = QingOnSurface,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "粘贴",
                color = QingOnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ResultCard(
    target: Language,
    state: TranslationUi,
    onCopy: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onAsk: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = QingCard,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(
                text = target.displayName,
                color = QingMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (state) {
                    TranslationUi.Idle -> Unit

                    TranslationUi.Loading -> Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            color = QingAccent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(text = "正在翻译…", color = QingMuted, fontSize = 15.sp)
                    }

                    is TranslationUi.Success -> Text(
                        text = state.text,
                        color = QingOnSurface,
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                    )

                    is TranslationUi.Failure -> Column {
                        Text(
                            text = state.message,
                            color = QingMuted,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        )
                        TextButton(onClick = onRetry) {
                            Text(text = "重试", color = QingAccent)
                        }
                    }

                    TranslationUi.NeedKey -> Column {
                        Text(
                            text = "还没有配置 DeepSeek API Key",
                            color = QingOnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "密钥只保存在本机，配置一次即可。",
                            color = QingMuted,
                            fontSize = 14.sp,
                        )
                        TextButton(onClick = onOpenSettings) {
                            Text(text = "去设置", color = QingAccent)
                        }
                    }
                }
            }

            if (state is TranslationUi.Success) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = { onAsk("解释一下这段翻译，重点说语气和使用场景") },
                        label = { Text("解释", fontSize = 12.sp) },
                    )
                    AssistChip(
                        onClick = { onAsk("有没有更口语、更自然的说法？") },
                        label = { Text("更口语", fontSize = 12.sp) },
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { onCopy(state.text) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "复制译文",
                            tint = QingOnSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = { onAsk(null) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forum,
                            contentDescription = "问助手",
                            tint = QingOnSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    source: Language?,
    target: Language,
    onSourceClick: () -> Unit,
    onTargetClick: () -> Unit,
    onSwapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LanguagePill(
            text = source?.displayName ?: "检测语言",
            onClick = onSourceClick,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSwapClick) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = "交换语言",
                tint = QingMuted,
                modifier = Modifier.size(24.dp),
            )
        }
        LanguagePill(
            text = target.displayName,
            onClick = onTargetClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LanguagePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = QingPill,
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = QingOnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
