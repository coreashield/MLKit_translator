
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

interface FlaskApiService {
    @Multipart
    @POST("/upload")
    fun uploadImage(@Part file: MultipartBody.Part): Call<ResponseBody>
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.45.115:5000/"

    val instance: FlaskApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(FlaskApiService::class.java)
    }
}

fun uploadImageToFlask(context: Context, imageUri: Uri) {
    val file = File(getRealPathFromURI(context, imageUri))
    val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
    val body = MultipartBody.Part.createFormData("file", file.name, requestBody)

    val call = RetrofitClient.instance.uploadImage(body)
    call.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                Toast.makeText(context, "Upload Success", Toast.LENGTH_SHORT).show()
                Log.d("Upload", "Success")
            } else {
                Toast.makeText(context, "Upload Failed: " + response.message(), Toast.LENGTH_SHORT).show()
                Log.d("Upload", "Failed: " + response.message())
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            Toast.makeText(context, "Upload Error: " + t.message, Toast.LENGTH_SHORT).show()
            Log.d("Upload", "Error: " + t.message)
        }
    })
}

fun getRealPathFromURI(context: Context, uri: Uri): String {
    var result: String? = null
    if (DocumentsContract.isDocumentUri(context, uri)) {
        val documentId = DocumentsContract.getDocumentId(uri)
        if (uri.authority == "com.android.providers.media.documents") {
            val id = documentId.split(":")[1]
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selection = "_id=?"
            val selectionArgs = arrayOf(id)
            result = getDataColumn(context, contentUri, selection, selectionArgs)
        } else if (uri.authority == "com.android.providers.downloads.documents") {
            val contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), documentId.toLong())
            result = getDataColumn(context, contentUri, null, null)
        } else if (uri.authority == "com.android.externalstorage.documents") {
            val split = documentId.split(":")
            val type = split[0]
            if (type.equals("primary", true)) {
                result = "${context.getExternalFilesDir(null)?.path}/${split[1]}"
            }
        }
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        result = getDataColumn(context, uri, null, null)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        result = uri.path
    }
    return result ?: ""
}

fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(column)

    try {
        cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(index)
        }
    } finally {
        cursor?.close()
    }
    return null
}
