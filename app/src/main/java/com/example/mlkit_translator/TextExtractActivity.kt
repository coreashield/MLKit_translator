package com.example.mlkit_translator

import android.app.Activity
import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val _outputText = MutableLiveData<String>()
    val outputText: LiveData<String> = _outputText

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> = _imageUri

    private val _translatedText = MutableLiveData<String>()
    val translatedText: LiveData<String> = _translatedText

    private val _isCameraPreviewVisible = MutableLiveData<Boolean>()
    val isCameraPreviewVisible: LiveData<Boolean> = _isCameraPreviewVisible

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val recognizer =
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
        if (uri != null) {
            processImage(uri)
        }
    }

    fun setCameraPreviewVisible(visible: Boolean) {
        _isCameraPreviewVisible.value = visible
    }

    fun saveResult(id: String, context: Context) {
        val db = Firebase.firestore
        val storage = Firebase.storage
        val storageRef = storage.reference
        val uri = _imageUri.value
        if (uri != null) {
            val riversRef = storageRef.child("${id}/${uri.lastPathSegment}")
            val uploadTask = riversRef.putFile(uri)

            uploadTask.addOnFailureListener { exception ->
                Log.w(TAG, "Error uploading file", exception)
                Toast.makeText(context, "파일 업로드 실패", Toast.LENGTH_SHORT).show()
            }.addOnSuccessListener { taskSnapshot ->
                riversRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    val data = hashMapOf(
                        "convertData" to _outputText.value,
                        "searchData" to "hi",
                        "imageUrl" to imageUrl,
                        "timestamp" to FieldValue.serverTimestamp(),
                    )
                    db.collection(id)
                        .add(data)
                        .addOnSuccessListener {
                            Log.d(TAG, "DocumentSnapshot successfully written!")
                            Toast.makeText(context, "업로드 및 저장 성공", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error writing document", e)
                            Toast.makeText(context, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        } else {
            val data = hashMapOf(
                "convertData" to _outputText.value,
                "searchData" to "hi",
                "imageUrl" to "",
                "timestamp" to FieldValue.serverTimestamp(),
            )
            db.collection("logID")
                .add(data)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    Toast.makeText(context, "저장 성공", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    Toast.makeText(context, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun processImage(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val image = InputImage.fromFilePath(getApplication(), uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    _outputText.value = visionText.text
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _isLoading.value = false
                }
        }
    }
}

class MainScreenViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainScreenViewModel::class.java)) {
            return MainScreenViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

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

//저장
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