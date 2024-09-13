import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.mlkit_translator.camera.CameraPreviewScreen
import com.example.mlkit_translator.screen.TranslateButton

@Composable
fun ImageTranslateScreen() {
    val context = LocalContext.current
    val viewModel: ImageTranslateViewModel = viewModel(
        factory = ImageTranslateViewModelFactory(context.applicationContext as Application)
    )

    val imageUri by viewModel.imageUri.observeAsState(null)
    val outputText by viewModel.outputText.observeAsState("")
    val isLoading by viewModel.isLoading.observeAsState(false)
    // 이미지 선택을 위한 Launcher 설정
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setImageUri(it) }
    }
    var showCamera by remember { mutableStateOf(false) }

    if (showCamera) {
        CameraPreviewScreen(
            onBack = { showCamera = false },
            onImageCaptured = { uri ->
                viewModel.setImageUri(uri)
                showCamera = false
            }
        )
    }else{
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 이미지 디스플레이
            ImageDisplay(imageUri = imageUri)
            DividerWithPadding()
            Button(onClick = { showCamera = true}) {
                Text("사진 찍기")
            }
            Button(onClick = {
                galleryLauncher.launch("image/*") // 갤러리에서 이미지를 선택하도록 호출
            }) {
                Text("사진 선택")
            }

            DividerWithPadding()
            // 결과 저장 버튼
            SaveResultButton(id = "sample_id", context = context, viewModel = viewModel)
            DividerWithPadding()

            // 번역 버튼
//        TranslateButton(
//            onClick = { viewModel.translateText(outputText) },
//            isLoading = isLoading
//        )

            // 번역 결과 텍스트 출력
            OutputText(outputText = outputText, isLoading = isLoading)
        }
    }

}

@Composable
fun SaveResultButton(
    id: String,
    context: Context,
    viewModel: ImageTranslateViewModel
) {
    Button(onClick = {
//        viewModel.saveResult(id, context)
    }) {
        Text(text = "저장 하기")
    }
}

@Composable
fun DividerWithPadding() {
    HorizontalDivider(
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
    val displayText = when {
        isLoading -> "이미지를 분석중입니다"
        outputText.isEmpty() -> "인식된 글자가 없습니다."
        else -> outputText
    }

    Text(
        text = displayText,
        modifier = Modifier.padding(20.dp)
    )
}
