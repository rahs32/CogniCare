package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onComplete: (String, String, String, String, String) -> Unit) {
    var currentStep by remember { mutableIntStateOf(1) }
    
    var sensorySensitivity by remember { mutableStateOf("1") }
    var commFamily by remember { mutableStateOf("Easily") }
    var commFriends by remember { mutableStateOf("Easily") }
    var commStrangers by remember { mutableStateOf("Easily") }
    var caregiverPhone by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CogniCare",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Step $currentStep of 3", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(32.dp))

            when (currentStep) {
                1 -> {
                    Text("How strongly are you affected by bright lights and loud sounds?", 
                        style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text("Your answer helps CogniCare personalize environmental alerts.", 
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val options = listOf(
                        "1" to "🟢 Not very affected - I usually feel comfortable.",
                        "2" to "🟡 Slightly affected - Sometimes bright lights or loud sounds bother me.",
                        "3" to "🟠 Moderately affected - I often become uncomfortable.",
                        "4" to "🔴 Highly affected - Bright lights or loud sounds can quickly become overwhelming."
                    )
                    options.forEach { (value, label) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (sensorySensitivity == value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            onClick = { sensorySensitivity = value }
                        ) {
                            Text(label, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                2 -> {
                    Text("How comfortable are you communicating with other people?", 
                        style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text("This helps CogniCare personalize AAC suggestions.", 
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        item { CommQuestion("Family", "How easily can you communicate your needs with your family?", commFamily) { commFamily = it } }
                        item { CommQuestion("Friends", "How easily can you communicate with your friends?", commFriends) { commFriends = it } }
                        item { CommQuestion("Strangers", "How easily can you communicate with unfamiliar people?", commStrangers) { commStrangers = it } }
                    }
                }
                3 -> {
                    Text("What would you like CogniCare to help you with most?", 
                        style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val supportOptions = listOf(
                        "🗣️ Communicating my needs",
                        "🔊 Managing sound sensitivity",
                        "💡 Managing light sensitivity",
                        "🧭 Following routines",
                        "🆘 Getting help during emergencies"
                    )
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(supportOptions.size) { index ->
                            var checked by remember { mutableStateOf(false) }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Checkbox(checked = checked, onCheckedChange = { checked = it })
                                Text(supportOptions[index], modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Emergency Contact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Enter a caregiver's phone number for automated SMS alerts.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = caregiverPhone,
                                onValueChange = { caregiverPhone = it },
                                label = { Text("Caregiver Phone Number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (currentStep < 3) {
                        currentStep++
                    } else {
                        onComplete(sensorySensitivity, commFamily, commFriends, commStrangers, caregiverPhone)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (currentStep == 3) "Finish Setup" else "Continue", fontSize = 18.sp)
                if (currentStep < 3) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun CommQuestion(title: String, desc: String, selected: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(desc, style = MaterialTheme.typography.bodySmall)
        val opts = listOf("Easily", "Sometimes difficult", "Often difficult", "I mostly use alternative communication")
        opts.forEach { opt ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == opt, onClick = { onSelect(opt) })
                Text(opt, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
