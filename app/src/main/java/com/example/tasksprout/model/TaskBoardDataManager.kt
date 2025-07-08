package com.example.tasksprout.model

import android.content.Context
import android.content.SharedPreferences
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.example.tasksprout.CurrentActivityProvider
import com.example.tasksprout.utilities.SignalManager
import com.google.firebase.firestore.FirebaseFirestore


object TaskBoardDataManager {

    private const val PREFS_NAME = "task_board_data"
    private lateinit var sharedPreferences: SharedPreferences
    private const val KEY_OPENED_BOARDS = "opened_boards"


    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }


    fun createBoardInFirestore(
        context: Context,
        taskBoard: TaskBoard,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("boards")
            .add(taskBoard)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
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

     fun deleteBoardFromFirestore(board: TaskBoard) {
        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", board.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val docId = snapshot.documents.firstOrNull()?.id ?: return@addOnSuccessListener
                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(docId)
                    .delete()
                    .addOnSuccessListener {
                        SignalManager.getInstance().toast("Board deleted")
                    }
            }
    }

     fun removeUserFromBoard(board: TaskBoard, email: String) {
        val updatedUsers = board.users.filter { it.email != email }
        val updatedBoard = board.copy(users = updatedUsers)

        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", board.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val docId = snapshot.documents.firstOrNull()?.id ?: return@addOnSuccessListener
                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(docId)
                    .set(updatedBoard)
                    .addOnSuccessListener {
                        SignalManager.getInstance().toast("Removed $email from board")
                    }
            }
    }

     fun buildUpdatedBoardWithNewUser(board: TaskBoard, newEmail: String): TaskBoard {
        val newUser = BoardUser(email = newEmail, name = "Anonymous", role = Role.MEMBER)

        return TaskBoard.Builder()
            .name(board.name)
            .description(board.description)
            .users(board.users + newUser)
            .tasks(board.tasks)
            .releaseDate(board.releaseDate)
            .xpClaim(board.xpClaim)
            .xpTodoToInProgress(board.xpTodoToInProgress)
            .xpToDone(board.xpToDone)
            .xpToNeglected(board.xpToNeglected)
            .xpNeglectedRecovered(board.xpNeglectedRecovered)

            .build()
    }

     fun updateBoardInFirestore(
        boardName: String,
        updatedBoard: TaskBoard,
        dialog: AlertDialog
    ) {
        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", boardName)
            .get()
            .addOnSuccessListener { snapshot ->
                val docId = snapshot.documents.firstOrNull()?.id
                if (docId != null) {
                    FirebaseFirestore.getInstance()
                        .collection("boards")
                        .document(docId)
                        .set(updatedBoard)
                        .addOnSuccessListener {
                            SignalManager.getInstance().toast("User added to board")
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            SignalManager.getInstance().toast("Failed to update board")
                        }
                } else {
                    SignalManager.getInstance().toast("Board not found")
                }
            }
            .addOnFailureListener {
                SignalManager.getInstance().toast("Error accessing Firestore")
            }
    }


    fun checkAndAwardBoardJoinAchievements(
        context: Context,
        userEmail: String,
        currentBoards: List<TaskBoard>
    ) {
        val activity = CurrentActivityProvider.getActivity() ?: return
        val rootLayout = activity.findViewById<ViewGroup>(android.R.id.content)

        currentBoards.forEach { board ->
            if (!hasBoardBeenOpened(board.name)) {
                listOf("joined_board", "joined_5_boards", "joined_10_boards").forEach { achievementId ->
                    AchievementManager.incrementAchievementProgress(userEmail, achievementId) {
                        AchievementManager.showAchievementPopup(context, rootLayout, it)
                    }
                }
            }
        }
    }



}
