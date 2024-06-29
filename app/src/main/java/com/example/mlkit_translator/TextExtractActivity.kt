package com.example.mlkit_translator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    var outputText by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) } // 이미지 URI 상태 변수
    val translatedText by remember { mutableStateOf("") }
    var isCameraPreviewVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // 로딩 상태 변수 추가

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                imageUri = uri
                isLoading = true
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        outputText = visionText.text
                        isLoading = false // 로딩 상태 종료
                    }
                    .addOnFailureListener {
                        isLoading = false // 실패 시에도 로딩 상태 종료
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "ML Kit Translator")
                    }
                }
            )
        },
        bottomBar = {
            // Custom bottom navigation using Row and IconButton
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
                    onClick = { /* Handle home navigation */ }
                )
                BottomNavigationItem(
                    icon = Icons.Filled.Search,
                    label = "Log",
                    onClick = { /* Handle translate navigation */ }
                )
                BottomNavigationItem(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    onClick = { /* Handle settings navigation */ }
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
                            onBack = { isCameraPreviewVisible = false },
                            onImageCaptured = { uri ->
                                imageUri = uri // 이미지 URI 업데이트
                                isCameraPreviewVisible = false
                                isLoading = true // 로딩 상태 시작
                                val image = InputImage.fromFilePath(context, uri)
                                recognizer.process(image)
                                    .addOnSuccessListener { visionText ->
                                        outputText = visionText.text
                                        isLoading = false // 로딩 상태 종료
                                    }
                                    .addOnFailureListener {
                                        isLoading = false // 실패 시에도 로딩 상태 종료
                                    }
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
//                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TakePicture(onClick = { isCameraPreviewVisible = true })
                            ExtractButton(onClick = {
                                val intent = Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                )
                                photoPickerLauncher.launch(intent)
                            })
                        }
                        SaveResultButton()
                        DividerWithPadding()
                        ImageDisplay(imageUri) // 이미지 URI를 ImageDisplay에 전달
                        DividerWithPadding()
                        OutputText(outputText,isLoading)
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
                            ) // 로딩 인디케이터 표시
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
fun SaveResultButton() {
    Button(onClick = { /*TODO*/ }) {
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
    // 인식된 텍스트를 화면에 출력하는 부분.
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