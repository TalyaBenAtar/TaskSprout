package com.example.tasksprout.model

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import com.example.tasksprout.R
import com.example.tasksprout.ui.TaskFragment
import com.example.tasksprout.ui.TaskUpperMenuFragment
import com.example.tasksprout.utilities.SignalManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TaskDataManager {

    private const val PREFS_NAME = "task_data"
    private lateinit var sharedPreferences: SharedPreferences
    private const val KEY_OPENED_TASKS = "opened_tasks"
    val tasks = mutableListOf<Task>()

//    private const val KEY_TASKS = "tasks"
//    private val gson = Gson()


    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//        loadTasks()
    }


    fun getAllTasks(): List<Task> {
        return tasks
    }

    fun markTaskAsOpened(name: String) {
        val opened = sharedPreferences.getStringSet(KEY_OPENED_TASKS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        opened.add(name)
        sharedPreferences.edit().putStringSet(KEY_OPENED_TASKS, opened).apply()
    }

    fun hasTaskBeenOpened(name: String): Boolean {
        val opened = sharedPreferences.getStringSet(KEY_OPENED_TASKS, mutableSetOf()) ?: emptySet()
        return name in opened
    }

     fun addTaskToBoard(task: Task, boardIntent: TaskBoard, onSuccess: () -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", boardIntent.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                val fullBoard = doc?.toObject(TaskBoard::class.java)
                if (fullBoard == null || doc == null) return@addOnSuccessListener

                val updatedTasks = fullBoard.tasks.toMutableList()
                updatedTasks.add(task)

                val updatedBoard = TaskBoard.Builder()
                    .name(fullBoard.name)
                    .description(fullBoard.description)
                    .users(fullBoard.users)
                    .tasks(updatedTasks)
                    .releaseDate(fullBoard.releaseDate)
                    .xpClaim(fullBoard.xpClaim)
                    .xpTodoToInProgress(fullBoard.xpTodoToInProgress)
                    .xpToDone(fullBoard.xpToDone)
                    .xpToNeglected(fullBoard.xpToNeglected)
                    .xpNeglectedRecovered(fullBoard.xpNeglectedRecovered)
                    .build()

                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(doc.id)
                    .set(updatedBoard)
                    .addOnSuccessListener {
                        SignalManager.getInstance().toast("Task added")
                        onSuccess()
                    }
                    .addOnFailureListener {
                        SignalManager.getInstance().toast("Failed to add task")
                    }
            }
    }

    fun deleteTaskFromFirestore(task: Task, board: TaskBoard, onSuccess: () -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", board.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                val boardDoc = doc?.toObject(TaskBoard::class.java) ?: return@addOnSuccessListener

                val updatedTasks = boardDoc.tasks.filterNot {
                    it.name == task.name &&
                            it.description == task.description
                }

                val updatedBoard = TaskBoard.Builder()
                    .name(boardDoc.name)
                    .description(boardDoc.description)
                    .users(boardDoc.users)
                    .tasks(updatedTasks)
                    .releaseDate(boardDoc.releaseDate)
                    .xpClaim(boardDoc.xpClaim)
                    .xpTodoToInProgress(boardDoc.xpTodoToInProgress)
                    .xpToDone(boardDoc.xpToDone)
                    .xpToNeglected(boardDoc.xpToNeglected)
                    .xpNeglectedRecovered(boardDoc.xpNeglectedRecovered)
                    .build()

                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(doc.id)
                    .set(updatedBoard)
                    .addOnSuccessListener {
                        SignalManager.getInstance().toast("Task deleted")
                        onSuccess()
                    }
            }
    }


    fun updateTaskInFirestore(
        updatedTask: Task,
        oldTask: Task? = null,
        board: TaskBoard,
        onSuccess: () -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", board.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                val boardDoc = doc?.toObject(TaskBoard::class.java) ?: return@addOnSuccessListener


                //  use oldTask if given, fallback to name+desc match
                val matchTask = oldTask ?: boardDoc.tasks.find {
                    it.name == updatedTask.name && it.description == updatedTask.description
                }


                if (matchTask != null && matchTask.status != updatedTask.status) {
                    UserDataManager.checkAndApplyXPChange(
                        oldStatus = matchTask.status,
                        newStatus = updatedTask.status,
                        assignedTo = updatedTask.assignedTo,
                        boardName = board.name
                    )
                    SignalManager.getInstance().toast("XP checked")
                }

                val updatedTasks = boardDoc.tasks.map {
                    val isMatch = if (oldTask != null) {
                        it.name == oldTask.name && it.description == oldTask.description
                    } else {
                        it.name == updatedTask.name && it.description == updatedTask.description
                    }
//delete? later
                    if (isMatch) {
                        if (it.assignedTo == null && updatedTask.assignedTo != null) {
                            UserDataManager.handleXPChange("CLAIM", boardDoc.name)
                        }
                        updatedTask
                    } else it
                }


                val updatedBoard = TaskBoard.Builder()
                    .name(boardDoc.name)
                    .description(boardDoc.description)
                    .users(boardDoc.users)
                    .tasks(updatedTasks)
                    .releaseDate(boardDoc.releaseDate)
                    .xpClaim(boardDoc.xpClaim)
                    .xpTodoToInProgress(boardDoc.xpTodoToInProgress)
                    .xpToDone(boardDoc.xpToDone)
                    .xpToNeglected(boardDoc.xpToNeglected)
                    .xpNeglectedRecovered(boardDoc.xpNeglectedRecovered)
                    .build()

                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(doc.id)
                    .set(updatedBoard)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                }
            }


}
