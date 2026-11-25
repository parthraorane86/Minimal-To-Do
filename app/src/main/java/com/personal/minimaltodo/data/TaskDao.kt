package com.personal.minimaltodo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, id DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, id DESC")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT COALESCE(MIN(sortOrder), 0) FROM tasks")
    suspend fun getMinSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Update
    suspend fun updateTasks(tasks: List<TaskEntity>)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM task_completions")
    fun observeAllCompletions(): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE dateEpochDay = :epochDay")
    fun observeCompletionsForDate(epochDay: Long): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    fun observeCompletionsInRange(
        startEpochDay: Long,
        endEpochDay: Long
    ): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE dateEpochDay = :epochDay")
    suspend fun getCompletionsForDateOnce(epochDay: Long): List<TaskCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: TaskCompletionEntity)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND dateEpochDay = :epochDay")
    suspend fun deleteCompletion(taskId: Long, epochDay: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observeSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsOnce(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettingsEntity)
}
