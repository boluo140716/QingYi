package com.qingyi.translate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingyi.translate.ui.AppViewModel
import com.qingyi.translate.ui.ImageUi
import com.qingyi.translate.ui.Screen
import com.qingyi.translate.ui.assistant.AssistantSheet
import com.qingyi.translate.ui.camera.CameraScreen
import com.qingyi.translate.ui.image.ImageTranslateScreen
import com.qingyi.translate.ui.language.LanguageListOverlay
import com.qingyi.translate.ui.settings.SettingsScreen
import com.qingyi.translate.ui.theme.QingAccent
import com.qingyi.translate.ui.theme.QingAccentSoft
import com.qingyi.translate.ui.theme.QingBg
import com.qingyi.translate.ui.theme.QingMuted
import com.qingyi.translate.ui.theme.QingOnSurface
import com.qingyi.translate.ui.theme.QingYiTheme
import com.qingyi.translate.ui.translate.TranslationScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QingYiTheme {
                val appViewModel: AppViewModel = viewModel()
                AppRoot(appViewModel)
            }
        }
    }
}

@Composable
private fun AppRoot(appViewModel: AppViewModel) {
    val state = appViewModel.state

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(appViewModel::onImagePicked) }

    val pickImage: () -> Unit = {
        imagePicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    BackHandler(
        enabled = state.languagePicker != null || state.screen != Screen.TRANSLATE
    ) {
        when {
            state.languagePicker != null -> appViewModel.closeLanguagePicker()
            state.screen == Screen.SETTINGS -> {
                val target = if (state.image != ImageUi.None) Screen.IMAGE else Screen.TRANSLATE
                appViewModel.go(target)
            }
            state.screen == Screen.CAMERA -> {
                val target = if (state.image != ImageUi.None) Screen.IMAGE else Screen.TRANSLATE
                appViewModel.go(target)
            }
            else -> appViewModel.go(Screen.TRANSLATE)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(QingBg)) {
        Scaffold(
            containerColor = QingBg,
            bottomBar = {
                if (state.screen == Screen.TRANSLATE) {
                    BottomBar(
                        onGallery = pickImage,
                        onCamera = { appViewModel.go(Screen.CAMERA) },
                        onSettings = { appViewModel.go(Screen.SETTINGS) },
                    )
                }
            },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding).fillMaxSize()) {
                when (state.screen) {
                    Screen.TRANSLATE -> TranslationScreen(
                        state = state,
                        viewModel = appViewModel,
                    )

                    Screen.CAMERA -> CameraScreen(
                        onClose = {
                            val target = if (state.image != ImageUi.None) Screen.IMAGE else Screen.TRANSLATE
                            appViewModel.go(target)
                        },
                        onCaptured = appViewModel::onImagePicked,
                        onPickImage = pickImage,
                    )

                    Screen.IMAGE -> ImageTranslateScreen(
                        state = state,
                        viewModel = appViewModel,
                        onPickImage = pickImage,
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        state = state,
                        viewModel = appViewModel,
                    )
                }
            }
        }

        state.languagePicker?.let { picker ->
            LanguageListOverlay(
                forSource = picker.forSource,
                selected = if (picker.forSource) state.source else state.target,
                onSelect = appViewModel::selectLanguage,
                onDismiss = appViewModel::closeLanguagePicker,
            )
        }

        AssistantSheet(state = state, viewModel = appViewModel)
    }
}

@Composable
private fun BottomBar(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QingBg)
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        BottomAction(
            icon = Icons.Outlined.PhotoLibrary,
            label = "图片",
            onClick = onGallery,
        )
        CameraAction(onClick = onCamera)
        BottomAction(
            icon = Icons.Filled.Settings,
            label = "设置",
            onClick = onSettings,
        )
    }
}

@Composable
private fun BottomAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(QingAccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = QingOnSurface,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = QingMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CameraAction(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(QingAccent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "相机",
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "相机",
            color = QingMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
