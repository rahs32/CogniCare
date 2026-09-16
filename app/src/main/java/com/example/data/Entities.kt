package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_tasks")
data class RoutineTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String,
    val time: String, // HH:mm format for simplicity
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(tableName = "aac_usage")
data class AacUsage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phrase: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "routine_completions")
data class RoutineCompletion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD format
    val totalTasks: Int,
    val completedTasks: Int
)
