package com.example.mlkit_translator.data

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
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
    var searchText by rememberSaveable { mutableStateOf("") }
    val filteredDataList = dataList.filter {
        it.convertData.contains(searchText, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBarSample(searchText) { newText -> searchText = newText }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(filteredDataList) { data ->
                TranslatedDataItem(data)
                HorizontalDivider()
            }
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatTimestampToKST(data.timestamp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = data.convertData,
            style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarSample(searchText: String, onSearchTextChange: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f }
                .fillMaxWidth(0.8f), // 크기를 80%로 조절
                shape = RectangleShape,
            colors = SearchBarDefaults.colors(Color.Black),
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchText,
                    onQueryChange = onSearchTextChange,
                    onSearch = { expanded = false },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("검색어를 입력하세요") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },

                )
            },
            expanded = false,
            onExpandedChange = { expanded = it },
        ) {
//            Column(Modifier.verticalScroll(rememberScrollState())) {
//                // 검색어를 기반으로 추천 검색어 등을 표시할 수 있습니다.
//                // 현재는 예제 데이터로 구성되어 있습니다.
//                repeat(4) { idx ->
//                    val resultText = "Suggestion $idx"
//                    ListItem(
//                        headlineContent = { Text(resultText) },
//                        supportingContent = { Text("Additional info") },
//                        leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
//                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
//                        modifier =
//                        Modifier
//                            .clickable {
//                                onSearchTextChange(resultText)
//                                expanded = false
//                            }
//                            .fillMaxWidth()
//                            .padding(horizontal = 16.dp, vertical = 4.dp)
//                    )
//                }
//            }
        }
    }
}
