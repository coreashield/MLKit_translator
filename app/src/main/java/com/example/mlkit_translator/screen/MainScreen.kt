package com.example.mlkit_translator.screen

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.mlkit_translator.firebase.Screens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, email: Any?) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel =
        viewModel(factory = MainScreenViewModelFactory(context.applicationContext as Application))
    val outputText by viewModel.outputText.observeAsState("")
    val translatedText by viewModel.translatedText.observeAsState("")
    val isLoading by viewModel.isLoading.observeAsState(false)
    val sourceLanguage by viewModel.sourceLanguage.observeAsState("ko")
    val targetLanguage by viewModel.targetLanguage.observeAsState("en")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "번역기요미")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LanguageSelectionRow(
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    onSourceLanguageChanged = { viewModel.setSourceLanguage(it) },
                    onTargetLanguageChanged = { viewModel.setTargetLanguage(it) }
                )
                SourceTextField(
                    value = outputText,
                    onValueChange = { viewModel.updateOutputText(it) }
                )
                TranslateButton(
                    onClick = { viewModel.translateText(outputText) }, // 번역 함수 호출
                    isLoading = isLoading
                )
                TranslatedTextField(
                    value = translatedText,
                    modifier = Modifier.weight(1f)
                )
            }
        } ,
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

                BottomNavigationItem(
                    icon = Icons.Filled.AccountCircle,
                    label = "Image",
                    onClick = { navController.navigate(Screens.ImageTranslate.route) }
                )
            }
        },
    )
}
@Composable
fun LanguageSelectionRow(
    sourceLanguage: String,
    targetLanguage: String,
    onSourceLanguageChanged: (String) -> Unit,
    onTargetLanguageChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LanguageDropdownMenu(
            label = sourceLanguage,
            onLanguageSelected = onSourceLanguageChanged
        )
        Spacer(modifier = Modifier.width(8.dp)) // 아이콘 대신 간격을 추가
        LanguageDropdownMenu(
            label = targetLanguage,
            onLanguageSelected = onTargetLanguageChanged
        )
    }
}

@Composable
fun LanguageDropdownMenu(label: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("ko", "en")
    Box(modifier = Modifier.wrapContentSize()) {
        Text(
            text = label,
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language) },  // text 파라미터에 Text를 직접 전달
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SourceTextField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("번역할 내용을 입력하세요") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(150.dp),
        maxLines = 5,
        singleLine = false
    )
}

@Composable
fun TranslatedTextField(value: String, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = {},
        placeholder = { Text("번역 결과") },
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(150.dp),
        readOnly = true,
        maxLines = 5,
        singleLine = false
    )
}

@Composable
fun TranslateButton(onClick: () -> Unit, isLoading: Boolean) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(16.dp)
            .height(50.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(text = "번역하기")
        }
    }
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

