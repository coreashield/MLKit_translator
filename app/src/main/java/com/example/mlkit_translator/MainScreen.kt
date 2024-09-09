package com.example.mlkit_translator

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel =
        viewModel(factory = MainScreenViewModelFactory(context.applicationContext as Application))
    val outputText by viewModel.outputText.observeAsState("")
    val imageUri by viewModel.imageUri.observeAsState(null)
    val translatedText by viewModel.translatedText.observeAsState("")
    val isCameraPreviewVisible by viewModel.isCameraPreviewVisible.observeAsState(false)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            viewModel.setImageUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "번역 귀요미")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavigationItem(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    onClick = { navController.navigate(Screens.TextExtract.route) }
                )
                BottomNavigationItem(
                    icon = Icons.Filled.Search,
                    label = "Log",
                    onClick = {
                        navController.navigate("${Screens.Logdata.route}/$email")
                    }
                )
                BottomNavigationItem(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    onClick = { }
                )
            }
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    if (isCameraPreviewVisible) {
                        CameraPreviewScreen(
                            onBack = { viewModel.setCameraPreviewVisible(false) },
                            onImageCaptured = { uri ->
                                viewModel.setImageUri(uri)
                                viewModel.setCameraPreviewVisible(false)
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TakePicture(onClick = { viewModel.setCameraPreviewVisible(true) })
                            ExtractButton(onClick = {
                                val intent = Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                )
                                photoPickerLauncher.launch(intent)
                            })
                            SaveResultButton(email, context, viewModel)
                        }
                        DividerWithPadding()
                        ImageDisplay(imageUri)
                        DividerWithPadding()
                        OutputText(outputText, isLoading)
                        Text(
                            text = translatedText,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(48.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun BottomNavigationItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun TakePicture(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("사진 찍기")
    }
}

@Composable
fun ExtractButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("사진 선택")
    }
}

@Composable
fun SaveResultButton(
    id: String,
    context: Context,
    viewModel: MainScreenViewModel
) {
    Button(onClick = {
        viewModel.saveResult(id, context)
    }) {
        Text(text = "저장 하기")
    }
}

@Composable
fun DividerWithPadding() {
    Divider(
        Modifier
            .fillMaxWidth()
            .padding(20.dp),
        thickness = 2.dp
    )
}

@Composable
fun ImageDisplay(imageUri: Uri?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RectangleShape)
            .size(320.dp)
    ) {
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "선택 된 이미지 없음",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun OutputText(outputText: String, isLoading: Boolean) {
    val displayText = if (isLoading) {
        "이미지를 분석중입니다"
    } else {
        if (outputText.isEmpty()) {
            "인식된 글자가 없습니다."
        } else {
            outputText
        }
    }

    Text(
        text = displayText,
        modifier = Modifier.padding(20.dp)
    )
}
