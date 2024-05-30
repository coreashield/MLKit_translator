package com.example.mlkit_translator

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mlkit_translator.ui.theme.MLKIT_translatorTheme
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MLKIT_translatorTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Text(
                text = "언어 감지",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(30.dp)
            )
            Text(
                text = "<->",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(30.dp)
            )
            Text(
                text = "한국어",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(30.dp)
            )
        }

        Divider(thickness = 1.dp, color = Color.Gray)

        var textState by remember {
            mutableStateOf("")
        }
        var outputText by remember {
            mutableStateOf("")
        }
        var outputText2 by remember {
            mutableStateOf("")
        }

        TextField(value = textState,
            onValueChange = { textState = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            label = { Text("번역할 내용을 입력하세요.") },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 200.dp)
                        .size(40.dp), // 아이콘 크기 조정
                )
            },

            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 200.dp)
                        .size(40.dp), // 아이콘 크기 조정
                )
            }
        )

        var isDownloaded by remember {
            mutableStateOf(false)
        }

        val koEnTranslator = remember {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.KOREAN)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
            Translation.getClient(options)
        }

        downloadModel(koEnTranslator, onSuccess = {
            isDownloaded = true
        })

        var translated by remember {
            mutableStateOf("")
        }

        Button(
            onClick = {
                outputText = textState.length.toString()
                outputText2 = textState.replace(" ", "")
                outputText2 = outputText2.length.toString()

                koEnTranslator.translate(textState)
                    .addOnSuccessListener { translatedText ->
                        translated = translatedText
                    }
                    .addOnFailureListener { exception ->
                        // Error.
                        // ...
                    }
            },
            enabled = isDownloaded
        ) {
            Text(text = "번역")
        }

        Text(text = "공백 포함 : $outputText")
        Text(text = "공백 미포함 : $outputText2")
        Text(text = "번역 결과 : $translated")
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MLKIT_translatorTheme {
        MainScreen()
    }
}


@Composable
fun downloadModel(
    koEnTranslator: Translator,
    onSuccess: () -> Unit,
) {
    LaunchedEffect(key1 = koEnTranslator) {
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        koEnTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                onSuccess()
            }
    }
}

