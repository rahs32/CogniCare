package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ReportsTab(viewModel: MainViewModel) {
    val completions by viewModel.routineCompletions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Routine Completion Trend",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (completions.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No data available yet.", style = MaterialTheme.typography.bodyLarge)
            }
            return
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large
        ) {
            Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxPoints = 7
                    val recentCompletions = completions.takeLast(maxPoints)
                    
                    val barWidth = size.width / (recentCompletions.size * 2 + 1)
                    var xOffset = barWidth
                    
                    recentCompletions.forEach { completion ->
                        val rate = if (completion.totalTasks > 0) completion.completedTasks.toFloat() / completion.totalTasks else 0f
                        val barHeight = size.height * rate
                        
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(xOffset, size.height - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                        
                        xOffset += barWidth * 2
                    }
                    
                    // Draw axis line
                    drawLine(
                        color = onSurfaceColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Showing up to last 7 days of activity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
