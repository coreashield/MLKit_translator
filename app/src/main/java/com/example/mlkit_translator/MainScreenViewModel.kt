package com.example.mlkit_translator

import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
