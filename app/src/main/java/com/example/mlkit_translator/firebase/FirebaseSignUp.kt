package com.example.mlkit_translator.firebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mlkit_translator.ui.theme.MLKIT_translatorTheme
import com.google.firebase.auth.FirebaseAuth

class FirebaseSignUp : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            MLKIT_translatorTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Screens.SignUp.route) {
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
                }
            }
        }
    }

    private fun signUpWithEmailAndPassword(email: String, password: String, navController: NavController) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navController.popBackStack(Screens.Login.route, inclusive = false)
                } else {
                    //가입 실패
                }
            }
    }
}

@Composable
fun SignUpScreen(
    onSignUpClick: (email: String, password: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
        Button(onClick = { onSignUpClick(email, password) }) {
            Text("Sign Up")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigateBack() }) {
            Text("Back to Login")
        }
    }
}
