package com.example.mlkit_translator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.mlkit_translator.composables.CameraPreviewScreen
import com.example.mlkit_translator.ui.theme.MLKIT_translatorTheme
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import uploadImageToFlask

class TextExtractActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    setContent {
                        MLKIT_translatorTheme {
                            MainScreen()
                        }
                    }
                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                }
            }

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setContent {
                MLKIT_translatorTheme {
                    MainScreen()
                }
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    var outputText by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) } // 이미지 URI 상태 변수
    var translatedText by remember { mutableStateOf("") }
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
                        Row {
                            TakePicture(onClick = { isCameraPreviewVisible = true })
                            ExtractButton(onClick = {
                                val intent = Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                )
                                photoPickerLauncher.launch(intent)
                            })
                        }
                        DividerWithPadding()
                        ImageDisplay(imageUri) // 이미지 URI를 ImageDisplay에 전달
                        DividerWithPadding()
                        OutputText(outputText)
                        TranslateButton(
                            textToTranslate = outputText,
                            onTranslationCompleted = { translation ->
                                translatedText = translation
                            }
                        )
                        Text(text = translatedText, modifier = Modifier.padding(20.dp))
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.padding(20.dp)) // 로딩 인디케이터 표시
                        }
                        // 전송 버튼 추가
                        imageUri?.let {
                            Button(onClick = { uploadImageToFlask(context, it) }) {
                                Text("전송")
                            }
                        }
                    }
                }
            }
        }
    )
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
    // 선택된 이미지를 화면에 표시하는 부분.
    if (imageUri != null) {
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .clip(RectangleShape)
                .size(160.dp)
        )
    }
}

@Composable
fun OutputText(outputText: String) {
    // 인식된 텍스트를 화면에 출력하는 부분.
    Text(text = outputText, modifier = Modifier.padding(20.dp))
}

@Composable
fun TranslateButton(textToTranslate: String, onTranslationCompleted: (String) -> Unit) {
    // 번역 버튼
    Button(onClick = {
        // 번역기 옵션 설정 (영어로 번역)
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.KOREAN)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        val translator = Translation.getClient(options)

        // 번역 작업 시작
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(textToTranslate)
                    .addOnSuccessListener { translatedText ->
                        onTranslationCompleted(translatedText)
                    }
                    .addOnFailureListener { exception ->
                        // 번역 실패 시 예외 처리
                        exception.printStackTrace()
                    }
            }
            .addOnFailureListener { exception ->
                // 모델 다운로드 실패 시 예외 처리
                exception.printStackTrace()
            }
    }) {
        Text(text = "번역하기")
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MLKIT_translatorTheme {
        MainScreen()
    }
}
