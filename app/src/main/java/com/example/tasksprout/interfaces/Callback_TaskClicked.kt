package com.example.tasksprout.interfaces

import com.example.tasksprout.model.Task

interface Callback_TaskClicked {
    fun onClick(task: Task)
}
