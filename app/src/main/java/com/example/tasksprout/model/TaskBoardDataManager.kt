package com.example.tasksprout.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.OnFailureListener
import kotlin.collections.mapNotNull



object TaskBoardDataManager {

    private const val PREFS_NAME = "task_board_data"
    private const val KEY_BOARDS = "task_boards"
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private const val KEY_OPENED_BOARDS = "opened_boards"
    val taskBoards = mutableListOf<TaskBoard>()

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadBoards()
    }

    fun addBoard(board: TaskBoard) {
        taskBoards.add(board)
        saveBoards()
    }

    fun getAllBoards(): List<TaskBoard> {
        return taskBoards
    }

    fun clearBoards() {
        taskBoards.clear()
        saveBoards()
    }

    fun getBoardByName(name: String?): TaskBoard? {
        return taskBoards.find { it.name == name }
    }


    private fun saveBoards() {
        val json = gson.toJson(taskBoards)
        sharedPreferences.edit().putString(KEY_BOARDS, json).apply()
    }

    private fun loadBoards() {
        taskBoards.clear()
        val json = sharedPreferences.getString(KEY_BOARDS, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<TaskBoard>>() {}.type
            val loaded = gson.fromJson<List<TaskBoard>>(json, type)
            taskBoards.addAll(loaded)
        }
    }

    fun createBoardInFirestore(
        context: Context,
        taskBoard: TaskBoard,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

//        db.collection("users")
//            .whereIn("email", taskBoard.users)
//            .get()
//            .addOnSuccessListener { snapshot ->
//                val foundEmails = snapshot.documents.mapNotNull { it.getString("email") }
//                if (foundEmails.size != taskBoard.users.size) {
//                    onFailure("Some users are not registered.")
//                    return@addOnSuccessListener
//                }

                // Save the board
                db.collection("boards")
                    .add(taskBoard)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
//            }
//            .addOnFailureListener { e ->
//                onFailure(e.message ?: "Unknown error while validating users")
//            }
    }


    fun markBoardAsOpened(name: String) {
        val opened = sharedPreferences.getStringSet(KEY_OPENED_BOARDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        opened.add(name)
        sharedPreferences.edit().putStringSet(KEY_OPENED_BOARDS, opened).apply()
    }

    fun hasBoardBeenOpened(name: String): Boolean {
        val opened = sharedPreferences.getStringSet(KEY_OPENED_BOARDS, mutableSetOf()) ?: emptySet()
        return name in opened
    }

}
