@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mlkit_translator

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

class TextExtractActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MLKIT_translatorTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    // 한국어 텍스트 인식기 초기화
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    var outputText by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var translatedText by remember { mutableStateOf("") }
    var isCameraPreviewVisible by remember { mutableStateOf(false) }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCameraPreviewVisible) {
                    CameraPreviewScreen(
                        onBack = { isCameraPreviewVisible = false },
                        onImageCaptured = { uri ->
                            imageUri = uri
                            val image = InputImage.fromFilePath(context, uri)
                            recognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    // 인식된 텍스트를 outputText에 저장
                                    outputText = visionText.text
                                }
                        }
                    )
                } else {
                    ExtractButton(onClick = { isCameraPreviewVisible = true })
                }
                DividerWithPadding()
                // 선택된 이미지 표시
                ImageDisplay(imageUri)
                DividerWithPadding()
                // 인식된 텍스트 출력
                OutputText(outputText)
                TranslateButton(
                    textToTranslate = outputText, // 번역할 텍스트를 전달
                    onTranslationCompleted = { translation -> // 콜백
                        translatedText = translation // translatedText에 저장
                    }
                )
                Text(text = translatedText, modifier = Modifier.padding(20.dp))
            }
        }
    )
}

@Composable
fun ExtractButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("사진 선택 ")
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
    // 선택된 이미지를 화면에 표시하는 부분입니다.
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

@Composable
fun OutputText(outputText: String) {
    // 인식된 텍스트를 화면에 출력하는 부분입니다.
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
