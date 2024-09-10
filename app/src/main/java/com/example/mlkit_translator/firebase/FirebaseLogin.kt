package com.example.mlkit_translator.firebase

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.mlkit_translator.data.FetchTranslatedData
import com.example.mlkit_translator.screen.MainScreen
import com.example.mlkit_translator.ui.theme.MLKIT_translatorTheme
import com.google.firebase.auth.FirebaseAuth


class FirebaseLogin : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            MLKIT_translatorTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        permissions.entries.forEach {
                            if (!it.value) {
                                // 권한이 거부되었을 때 처리
                                Toast.makeText(context, "${it.key} 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    val permissions = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    val allPermissionsGranted = permissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (!allPermissionsGranted) {
                        permissionLauncher.launch(permissions)
                    }
                }
                NavHost(navController = navController, startDestination = Screens.Login.route) {
                    composable(Screens.Login.route) {
                        LoginScreen(
                            onLoginClick = { email, password ->
                                signInWithEmailAndPassword(email, password, navController)
                            },
                            onSignUpClick = {
                                navController.navigate(Screens.SignUp.route)
                            }
                        )
                    }
                    composable(Screens.SignUp.route) {
                        SignUpScreen(
                            onSignUpClick = { email, password ->
                                signUpWithEmailAndPassword(email, password, navController)
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(Screens.TextExtract.route) {
                        MainScreen(navController)
                    }

                    composable(Screens.Logdata.route + "/{email}") { backStackEntry ->
                        val userID = backStackEntry.arguments?.getString("email") ?: ""
                        FetchTranslatedData(userID)
                    }
                }
            }
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String, navController: NavHostController) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navController.navigate(Screens.TextExtract.route)
                } else {
                    showErrorMessage("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun signUpWithEmailAndPassword(email: String, password: String, navController: NavHostController) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navController.popBackStack(Screens.Login.route, inclusive = false)
                } else {
                    showErrorMessage("Sign up failed: ${task.exception?.message}")
                }
            }
    }

    private fun showErrorMessage(message: String?) {
        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
    }
}

sealed class Screens(val route: String) {
    object Login : Screens("login_screen")
    object SignUp : Screens("sign_up_screen")
    object TextExtract : Screens("text_extract_screen")
    object Logdata : Screens("Logdata_screen")
}

@Composable
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit,
    onSignUpClick: () -> Unit
) {
    var email by remember { mutableStateOf("coreashield1@gmail.com") }
    var password by remember { mutableStateOf("rlgns123") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onLoginClick(email, password) }) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onSignUpClick() }) {
            Text("Sign Up")
        }
    }
}