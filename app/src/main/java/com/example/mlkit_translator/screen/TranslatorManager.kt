package com.example.mlkit_translator.screen

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation

class TranslatorManager(private val context: Context) {

    private var translator: Translator? = null

    fun initializeTranslator(sourceLanguage: String?, targetLanguage: String?) {
        if (sourceLanguage == null || targetLanguage == null) {
            throw IllegalArgumentException("Source or target language is null")
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLanguage)!!)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLanguage)!!)
            .build()

        translator = Translation.getClient(options)
    }

    fun downloadModelIfNeeded(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                onSuccess()
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun translateText(text: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        translator?.translate(text)
            ?.addOnSuccessListener { translatedText ->
                onSuccess(translatedText)
            }
            ?.addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun deleteModel(languageTag: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val modelManager = RemoteModelManager.getInstance()
        val model = TranslateRemoteModel.Builder(TranslateLanguage.fromLanguageTag(languageTag)!!).build()
        modelManager.deleteDownloadedModel(model)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
