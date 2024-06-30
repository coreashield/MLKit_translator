package com.example.mlkit_translator

import android.util.Log
import android.widget.Space
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class TranslatedData(
    val convertData: String,
    val searchData: String,
    val timestamp: Date?,
    val imageUrl: String
)

@Composable
fun FetchTranslatedData(userID: String, viewModel: TranslatedDataViewModel = viewModel()) {
    val dataList by viewModel.dataList.observeAsState(emptyList())

    LaunchedEffect(userID) {
        viewModel.fetchData(userID)
    }

    TranslatedDataList(dataList)
}

class TranslatedDataViewModel : ViewModel() {
    private val _dataList = MutableLiveData<List<TranslatedData>>()
    val dataList: LiveData<List<TranslatedData>> = _dataList

    fun fetchData(userID: String) {
        val db = Firebase.firestore
        db.collection(userID)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                val data = result.map { document ->
                    TranslatedData(
                        convertData = document.getString("convertData") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        searchData = document.getString("searchData") ?: "",
                        timestamp = document.getTimestamp("timestamp")?.toDate()
                    )
                }
                _dataList.value = data
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }
}

@Composable
fun TranslatedDataList(dataList: List<TranslatedData>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(dataList) { data ->
            TranslatedDataItem(data)
            Divider()
        }
    }
}

@Composable
fun TranslatedDataItem(data: TranslatedData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RectangleShape)
            .clickable { }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "추출 내용")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = data.convertData, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
//        Text(text = "Search Data: ${data.searchData}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Timestamp: ${formatTimestampToKST(data.timestamp)}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (data.imageUrl.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(data.imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
    }
}

fun formatTimestampToKST(timestamp: Date?): String {
    return if (timestamp != null) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        sdf.format(timestamp)
    } else {
        "N/A"
    }
}
