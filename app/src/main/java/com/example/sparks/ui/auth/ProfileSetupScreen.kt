package com.example.sparks.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sparks.viewmodel.AuthViewModel

@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
    onProfileCompleted: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Profiles are visible to people you message, contacts, and groups.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            fontSize = 14.sp
        )

        // First Name (Required)
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name (Required)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Last Name (Optional)
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            supportingText = { Text("People can find you by this handle.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            prefix = { Text("@") }
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (firstName.isNotBlank() && username.isNotBlank()) {
                    viewModel.createProfile(firstName, lastName, username) {
                        onProfileCompleted()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && firstName.isNotBlank() && username.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Complete Setup")
            }
        }
    }
}