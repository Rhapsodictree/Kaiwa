package com.example.kaiwa

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaiwa.ui.theme.KaiwaTheme
import com.example.kaiwa.view.VideoActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KaiwaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginPage()
                }
            }
        }
    }
}

@Composable
fun LoginPage() {
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // App Title
        Text(
            text = "KAIWA",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "VIDEO CONFERENCING APP",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(50.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            InputFields(
                onJoinClick = { isLoading = true },
                onJoinComplete = { isLoading = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputFields(
    onJoinClick: () -> Unit,
    onJoinComplete: () -> Unit
) {
    val context = LocalContext.current
    var channelName by remember { mutableStateOf("") }
    var channelNameError by remember { mutableStateOf<String?>(null) }
    val userRoleOptions = listOf("Broadcaster", "Audience")
    var selectedRole by remember { mutableStateOf(userRoleOptions[0]) }

    Column(
        modifier = Modifier.padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // Channel Name Input
        TextField(
            value = channelName,
            onValueChange = {
                channelName = it
                channelNameError = null
            },
            label = { Text("Channel Name") },
            placeholder = { Text("Enter channel name") },
            isError = channelNameError != null,
            supportingText = channelNameError?.let {
                { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (channelNameError != null) 0.dp else 16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Role Selection
        Text(
            text = "Select Role:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        userRoleOptions.forEach { role ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (role == selectedRole),
                        onClick = { selectedRole = role }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (role == selectedRole),
                    onClick = { selectedRole = role }
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Join Button
        Button(
            onClick = {
                if (validateInput(channelName)) {
                    onJoinClick()
                    try {
                        val intent = Intent(context, VideoActivity::class.java).apply {
                            putExtra("ChannelName", channelName.trim())
                            putExtra("UserRole", selectedRole)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        onJoinComplete()
                    }
                } else {
                    channelNameError = "Please enter a valid channel name"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = "Join",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(
                text = "Join Meeting",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun validateInput(channelName: String): Boolean {
    val trimmedChannelName = channelName.trim()
    return trimmedChannelName.isNotEmpty() &&
            trimmedChannelName.length >= 3 &&
            trimmedChannelName.length <= 64 &&
            trimmedChannelName.matches(Regex("^[a-zA-Z0-9_-]+$"))
}