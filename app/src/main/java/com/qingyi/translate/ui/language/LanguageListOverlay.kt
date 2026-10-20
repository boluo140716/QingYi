package com.qingyi.translate.ui.language

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingyi.translate.data.Language
import com.qingyi.translate.ui.theme.QingBg
import com.qingyi.translate.ui.theme.QingMuted
import com.qingyi.translate.ui.theme.QingOnSurface

@Composable
fun LanguageListOverlay(
    forSource: Boolean,
    selected: Language?,
    onSelect: (Language?) -> Unit,
    onDismiss: () -> Unit,
) {
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
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = QingOnSurface,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (forSource) "翻译自" else "翻译成",
                color = QingOnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (forSource) {
                item {
                    LanguageRow(
                        label = "检测语言",
                        isSelected = selected == null,
                        onClick = { onSelect(null) },
                    )
                }
            }

            items(Language.entries.toList()) { language ->
                LanguageRow(
                    label = language.displayName,
                    isSelected = selected == language,
                    onClick = { onSelect(language) },
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = if (isSelected) QingOnSurface else QingMuted,
            fontSize = 17.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "已选择",
                tint = QingOnSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
