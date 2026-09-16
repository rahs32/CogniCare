package com.example.ui

import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.roundToInt

@Composable
fun GaugeBar(progress: Float, color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
        drawRoundRect(
            color = Color.LightGray.copy(alpha = 0.3f),
            size = size,
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        drawRoundRect(
            color = color,
            size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MonitorTab(viewModel: MainViewModel) {
    val tasks by viewModel.routineTasks.collectAsStateWithLifecycle()
    val soundLevel by viewModel.currentSoundLevel.collectAsStateWithLifecycle()
    val lightLevel by viewModel.currentLightLevel.collectAsStateWithLifecycle()
    val aiSuggestion by viewModel.aiSuggestion.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val locationPermissionState = com.google.accompanist.permissions.rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    var isLiveMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.allPermissionsGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(audioPermissionState.status, isLiveMode) {
        if (isLiveMode) {
            if (audioPermissionState.status.isGranted) {
                viewModel.startSensing()
            } else {
                audioPermissionState.launchPermissionRequest()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Sensory Environment", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FilledTonalIconToggleButton(
                    checked = isLiveMode,
                    onCheckedChange = { isLiveMode = it }
                ) {
                    Text(if (isLiveMode) "Live" else "Demo", modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = if (soundLevel > 100) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("🔊 Sound", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${soundLevel.roundToInt()} dB", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GaugeBar(progress = soundLevel / 120f, color = if (soundLevel > 100) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (soundLevel > 100) "🔴 Potentially overwhelming" else "🟢 Comfortable", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = if (lightLevel > 500) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("💡 Light", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${lightLevel.roundToInt()} lux", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GaugeBar(progress = lightLevel / 1000f, color = if (lightLevel > 500) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (lightLevel > 500) "🔴 Potentially overwhelming" else "🟢 Comfortable", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        
        if (!isLiveMode) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = { viewModel.simulateEnvironment(45f, 200f); viewModel.aiSuggestion.value = "" }) { Text("Normal") }
                    OutlinedButton(onClick = { 
                        viewModel.simulateEnvironment(105f, 200f)
                        viewModel.getAiSuggestion("The environment is too loud.")
                    }) { Text("Loud") }
                    OutlinedButton(onClick = { 
                        viewModel.simulateEnvironment(45f, 800f)
                        viewModel.getAiSuggestion("The environment is too bright.")
                    }) { Text("Bright") }
                }
            }
        } else if (!audioPermissionState.status.isGranted) {
            item {
                Text("Microphone permission required for live sound monitoring.", color = MaterialTheme.colorScheme.error)
            }
        }
        
        if (aiSuggestion.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("AI Sensory Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(aiSuggestion, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, lineHeight = 24.sp)
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.notifyCaregiver("Environment is overwhelming.") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("💬", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Text Caregiver for Support", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Today's Routine", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                var showAiDialog by remember { mutableStateOf(false) }
                FilledTonalButton(onClick = { showAiDialog = true }) {
                    Text("AI Routine ✨")
                }
                
                if (showAiDialog) {
                    var aiPrompt by remember { mutableStateOf("") }
                    var isGenerating by remember { mutableStateOf(false) }
                    
                    AlertDialog(
                        onDismissRequest = { if (!isGenerating) showAiDialog = false },
                        title = { Text("Generate AI Routine") },
                        text = {
                            Column {
                                Text("What kind of day is it? (e.g. 'A relaxed weekend at home' or 'A busy school day with therapy')")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = aiPrompt,
                                    onValueChange = { aiPrompt = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isGenerating
                                )
                                if (isGenerating) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    isGenerating = true
                                    viewModel.generateRoutineWithAi(aiPrompt) {
                                        isGenerating = false
                                        showAiDialog = false
                                    }
                                },
                                enabled = aiPrompt.isNotEmpty() && !isGenerating
                            ) { Text("Generate") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAiDialog = false }, enabled = !isGenerating) { Text("Cancel") }
                        }
                    )
                }
                
                val pendingRoutine by viewModel.pendingAiRoutine.collectAsStateWithLifecycle()
                if (pendingRoutine != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.rejectAiRoutine() },
                        title = { Text("Proposed Routine") },
                        text = {
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                itemsIndexed(pendingRoutine!!) { index, task ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(task.icon, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(task.name, fontWeight = FontWeight.Bold)
                                            Text(task.time, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (index < pendingRoutine!!.lastIndex) {
                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.approveAiRoutine() }) {
                                Text("Approve & Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.rejectAiRoutine() }) {
                                Text("Discard")
                            }
                        }
                    )
                }
            }
            val completedCount = tasks.count { it.isCompleted }
            val rawProgress = if (tasks.isNotEmpty()) completedCount.toFloat() / tasks.size else 0f
            val progress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = rawProgress,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Large circular progress indicator
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(120.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$completedCount / ${tasks.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Tasks", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        
        itemsIndexed(tasks) { index, task ->
            val isNextTask = !task.isCompleted && (index == 0 || tasks[index-1].isCompleted)
            var showSparkle by remember(task.id) { mutableStateOf(false) }
            
            LaunchedEffect(showSparkle) {
                if (showSparkle) {
                    kotlinx.coroutines.delay(800)
                    showSparkle = false
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Timeline Graphics
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                    if (index > 0) {
                        Box(modifier = Modifier.width(2.dp).weight(1f).background(if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    task.isCompleted -> MaterialTheme.colorScheme.primary
                                    isNextTask -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        } else if (isNextTask) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSecondary))
                        }
                    }
                    
                    if (index < tasks.lastIndex) {
                        Box(modifier = Modifier.width(2.dp).weight(1f).background(if (tasks[index+1].isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Task Card
                Card(
                    onClick = { 
                        if (!task.isCompleted) {
                            showSparkle = true
                            try {
                                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                                val r = android.media.RingtoneManager.getRingtone(context, uri)
                                r.play()
                            } catch (e: Exception) {}
                        }
                        viewModel.toggleTaskCompletion(task) 
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            isNextTask -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(task.icon, fontSize = 28.sp)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showSparkle,
                                enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                            ) {
                                Text("✨", fontSize = 36.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                task.name, 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = if (isNextTask) FontWeight.Bold else FontWeight.SemiBold, 
                                color = if(task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            Text(task.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
