package com.example.mlkit_translator

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
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
import com.example.mlkit_translator.ui.theme.MLKIT_translatorTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

class TextExtractActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MLKIT_translatorTheme {
                MainScreen2()
            }
        }
    }
}

@Composable
fun MainScreen2() {
    Column(modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally) {
        val context = LocalContext.current
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        var outputText by remember {
            mutableStateOf("")
        }
        var imageUri by remember { mutableStateOf<Uri?>(null)}
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                imageUri = uri
                if (uri != null) {
                    val image = InputImage.fromFilePath(context, uri)
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            outputText = visionText.text
                        }
                }
            }
        )

        Button(onClick = {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        ) {
            Text("추출 ")
        }
        
        Divider(Modifier
            .fillMaxWidth()
            .padding(20.dp),
            thickness = 2.dp)

        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = "",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                .clip(RectangleShape)
                .size(160.dp)
        )

        Divider(Modifier
            .fillMaxWidth()
            .padding(20.dp),
            thickness = 2.dp)
        Text(text = outputText, modifier = Modifier.padding(20.dp))
        Button(onClick = { /*TODO*/ }) {
            Text(text = "번역하기")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreen2Preview() {
    MLKIT_translatorTheme {
        MainScreen2()
    }
}
