package com.qingyi.translate.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyi.translate.ui.AppUiState
import com.qingyi.translate.ui.AppViewModel
import com.qingyi.translate.ui.AssistantRole
import com.qingyi.translate.ui.AssistantTurn
import com.qingyi.translate.ui.ImageUi
import com.qingyi.translate.ui.TranslationUi
import com.qingyi.translate.ui.theme.QingAccent
import com.qingyi.translate.ui.theme.QingBg
import com.qingyi.translate.ui.theme.QingCard
import com.qingyi.translate.ui.theme.QingMuted
import com.qingyi.translate.ui.theme.QingOnSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSheet(state: AppUiState, viewModel: AppViewModel) {
    if (!state.assistant.open) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasImageContext = state.image is ImageUi.Success

    ModalBottomSheet(
        onDismissRequest = viewModel::closeAssistant,
        sheetState = sheetState,
        containerColor = QingBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 380.dp, max = 620.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "翻译助手",
                color = QingOnSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (hasImageContext) "基于选中的图片和译文回答" else "基于原文和译文回答",
                color = QingMuted,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.assistant.turns.isEmpty() && !state.assistant.loading) {
                    item {
                        Text(
                            text = "可以问我：这句话为什么这么翻、有没有更口语的说法、某个词在语境里的意思……",
                            color = QingMuted,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

                items(state.assistant.turns) { turn ->
                    TurnBubble(turn)
                }

                if (state.assistant.loading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = QingAccent,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = "正在思考…", color = QingMuted, fontSize = 13.sp)
                        }
                    }
                }
            }

            state.assistant.error?.let { message ->
                Spacer(Modifier.height(6.dp))
                Text(text = message, color = com.qingyi.translate.ui.theme.QingError, fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { viewModel.openAssistant("解释一下这段翻译，重点说语气和使用场景") },
                    label = { Text("解释一下") },
                )
                AssistChip(
                    onClick = { viewModel.openAssistant("有没有更口语、更自然的说法？") },
                    label = { Text("更口语") },
                )
                AssistChip(
                    onClick = { viewModel.openAssistant("帮我润色一下译文，保持原意") },
                    label = { Text("润色") },
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.assistant.input,
                    onValueChange = viewModel::onAssistantInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("继续问点什么…", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { viewModel.sendAssistantQuestion() }
                    ),
                )
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = viewModel::sendAssistantQuestion,
                    enabled = state.assistant.input.isNotBlank() && !state.assistant.loading,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (state.assistant.input.isNotBlank()) {
                            QingAccent
                        } else {
                            QingMuted
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TurnBubble(turn: AssistantTurn) {
    val isUser = turn.role == AssistantRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.82f else 0.92f)
                .background(
                    color = if (isUser) QingAccent else QingCard,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = turn.text,
                color = if (isUser) androidx.compose.ui.graphics.Color.White else QingOnSurface,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
        }
    }
}
