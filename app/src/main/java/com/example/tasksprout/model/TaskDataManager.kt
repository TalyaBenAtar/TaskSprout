package com.example.tasksprout.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TaskDataManager {

    private const val PREFS_NAME = "task_data"
    private const val KEY_TASKS = "tasks"
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private const val KEY_OPENED_TASKS = "opened_tasks"

    val tasks = mutableListOf<Task>()

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadTasks()
    }

    fun addTask(task: Task) {
        tasks.add(task)
        saveTasks()
    }

    fun getAllTasks(): List<Task> {
        return tasks
    }

    fun clearTasks() {
        tasks.clear()
        saveTasks()
    }

    private fun saveTasks() {
        val json = gson.toJson(tasks)
        sharedPreferences.edit().putString(KEY_TASKS, json).apply()
    }

    private fun loadTasks() {
        tasks.clear()
        val json = sharedPreferences.getString(KEY_TASKS, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<Task>>() {}.type
            val loaded = gson.fromJson<List<Task>>(json, type)
            tasks.addAll(loaded)
        }
    }

    fun markTaskAsOpened(name: String) {
        val opened = TaskDataManager.sharedPreferences.getStringSet(KEY_OPENED_TASKS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        opened.add(name)
        TaskDataManager.sharedPreferences.edit().putStringSet(KEY_OPENED_TASKS, opened).apply()
    }

    fun hasTaskBeenOpened(name: String): Boolean {
        val opened = TaskDataManager.sharedPreferences.getStringSet(KEY_OPENED_TASKS, mutableSetOf()) ?: emptySet()
        return name in opened
    }

}
