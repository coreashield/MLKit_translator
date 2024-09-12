import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mlkit_translator.screen.TranslatorManager

class ImageTranslateViewModel(application: Application) : AndroidViewModel(application) {
    private val translatorManager = TranslatorManager(application) // TranslatorManager 초기화

    private val _outputText = MutableLiveData<String>()
    val outputText: LiveData<String> = _outputText

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> = _imageUri

    private val _translatedText = MutableLiveData<String>()
    val translatedText: LiveData<String> = _translatedText

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 추가된 부분: sourceLanguage와 targetLanguage
    private val _sourceLanguage = MutableLiveData("ko")
    val sourceLanguage: LiveData<String> = _sourceLanguage

    private val _targetLanguage = MutableLiveData("en")
    val targetLanguage: LiveData<String> = _targetLanguage

    // 이미지 처리 및 번역 관련 함수들 (processImage, translateText 등) 여기에 포함
}

class ImageTranslateViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageTranslateViewModel::class.java)) {
            return ImageTranslateViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}