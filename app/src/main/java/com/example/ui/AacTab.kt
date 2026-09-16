package com.example.ui

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

data class AacItem(val icon: String, val label: String, val message: String, val category: String)

@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun AacTab(viewModel: MainViewModel) {
    var constructedMessage by remember { mutableStateOf("") }
    
    val detectedLocation by viewModel.currentLocationContext.collectAsStateWithLifecycle()
    var selectedLocation by remember { mutableStateOf(detectedLocation) }
    
    val groupedOptions by viewModel.aacOptions.collectAsStateWithLifecycle()
    val optionsGroupedByCategory = groupedOptions.groupBy { it.category }
    val isBoyVoice by viewModel.isBoyVoice.collectAsStateWithLifecycle()
    
    // Auto-update selected location if detected location changes, but allow manual override
    LaunchedEffect(detectedLocation) {
        selectedLocation = detectedLocation
        viewModel.updateAacOptionsForLocation(selectedLocation)
    }
    
    LaunchedEffect(selectedLocation) {
        viewModel.updateAacOptionsForLocation(selectedLocation)
    }
    
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            viewModel.startLocationMonitoring()
        }
    }
    
    val locations = listOf("Home", "School", "Public", "Transit")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        // Sticky Confirmation Area at top
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = constructedMessage.ifEmpty { "Tap cards to speak..." },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (constructedMessage.isEmpty()) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onPrimaryContainer,
                    minLines = 2,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.speak(constructedMessage) },
                        enabled = constructedMessage.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Speak")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Speak")
                    }
                    FilledTonalIconToggleButton(
                        checked = isBoyVoice,
                        onCheckedChange = { viewModel.toggleVoice() }
                    ) {
                        Text(if (isBoyVoice) "👦" else "👧")
                    }
                    FilledTonalButton(
                        onClick = { constructedMessage = "" },
                        enabled = constructedMessage.isNotEmpty(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!locationPermissions.allPermissionsGranted) {
            Card(
                onClick = { locationPermissions.launchMultiplePermissionRequest() },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = "Enable Auto-Location")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tap to enable auto-location switching", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(locations) { loc ->
                FilterChip(
                    selected = selectedLocation == loc,
                    onClick = { selectedLocation = loc },
                    label = { Text(loc) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        val categoryOrder = listOf("Context: $selectedLocation", "Needs", "Sensory", "Feelings")
        val sortedCategories = optionsGroupedByCategory.keys.sortedBy { key -> 
            categoryOrder.indexOf(key).takeIf { it >= 0 } ?: 99 
        }
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            sortedCategories.forEach { category ->
                val items = optionsGroupedByCategory[category] ?: emptyList()
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(items) { item ->
                    Card(
                        onClick = { 
                            constructedMessage = if (constructedMessage.isEmpty()) item.message else "$constructedMessage ${item.message}"
                            viewModel.recordAacUsage(item.label)
                        },
                        modifier = Modifier.height(110.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = when(item.category) {
                                "Feelings" -> MaterialTheme.colorScheme.secondaryContainer
                                "Needs" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(item.icon, fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(item.label, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
