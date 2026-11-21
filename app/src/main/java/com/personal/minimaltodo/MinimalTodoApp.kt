package com.personal.minimaltodo

import android.app.Application
import com.personal.minimaltodo.data.AppDatabase
import com.personal.minimaltodo.data.TodoRepository
import com.personal.minimaltodo.notifications.TaskReminderWorker

class MinimalTodoApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: TodoRepository by lazy {
        TodoRepository(database.taskDao(), database.settingsDao())
    }

    override fun onCreate() {
        super.onCreate()
        TaskReminderWorker.ensureChannel(this)
    }
}
