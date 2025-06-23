package com.example.tasksprout.interfaces

import com.example.tasksprout.model.Task

interface TaskInteractionHandler {
    fun onEditTask(task: Task)
    fun onDeleteTask(task: Task)
    fun onClaimTask(task: Task)
}