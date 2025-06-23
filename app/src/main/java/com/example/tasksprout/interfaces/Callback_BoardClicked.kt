package com.example.tasksprout.interfaces

import com.example.tasksprout.model.TaskBoard

interface Callback_BoardClicked {
    fun onBoardClicked(board: TaskBoard)
    fun onBoardLongClicked(board: TaskBoard)
}
