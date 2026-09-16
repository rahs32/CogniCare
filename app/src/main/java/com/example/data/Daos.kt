package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineTaskDao {
    @Query("SELECT * FROM routine_tasks ORDER BY orderIndex ASC")
    fun getAllTasks(): Flow<List<RoutineTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: RoutineTask)

    @Update
    suspend fun updateTask(task: RoutineTask)

    @Query("DELETE FROM routine_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    @Query("DELETE FROM routine_tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface AacUsageDao {
    @Query("SELECT phrase, COUNT(*) as count FROM aac_usage GROUP BY phrase ORDER BY count DESC LIMIT 5")
    fun getTopPhrases(): Flow<List<PhraseCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: AacUsage)
}

data class PhraseCount(val phrase: String, val count: Int)

@Dao
interface RoutineCompletionDao {
    @Query("SELECT * FROM routine_completions ORDER BY date ASC")
    fun getAllCompletions(): Flow<List<RoutineCompletion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: RoutineCompletion)
    
    @Query("SELECT * FROM routine_completions WHERE date = :date")
    suspend fun getCompletionByDate(date: String): RoutineCompletion?
}
