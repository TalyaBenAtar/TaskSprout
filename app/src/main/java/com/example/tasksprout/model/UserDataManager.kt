package com.example.tasksprout.model

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.example.tasksprout.CurrentActivityProvider
import com.example.tasksprout.utilities.SignalManager
import com.example.tasksprout.utilities.SingleSoundPlayer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.tasksprout.R
import com.google.firebase.firestore.FieldValue


object UserDataManager {
    var currentUser: User? = null
    private val firestore = FirebaseFirestore.getInstance()
    private const val USERS_COLLECTION = "users"



    // Initializes the currentUser by loading from Firestore or creating a new user doc if it doesn't exist.
    fun init(context: Context, onComplete: (Boolean) -> Unit) {
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email == null) {
            Log.e("UserDataManager", "User not authenticated")
            onComplete(false)
            return
        }

        firestore.collection(USERS_COLLECTION)
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    Log.d("UserDataManager", "Loaded user: $currentUser")
                    onComplete(true)
                } else {
                    val newUser = User(
                        email = email,
                        name = email.substringBefore("@") // default name
                    )
                    firestore.collection(USERS_COLLECTION)
                        .document(email)
                        .set(newUser)
                        .addOnSuccessListener {
                            currentUser = newUser
                            Log.d("UserDataManager", "Created new user: $newUser")
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            Log.e("UserDataManager", "Failed to create user", it)
                            onComplete(false)
                        }
                }
            }
            .addOnFailureListener {
                Log.e("UserDataManager", "Failed to load user", it)
                onComplete(false)
            }
    }

    fun handleXPChange(event: String, boardId: String, context: Context) {
        var ssp = SingleSoundPlayer(context)
        val db = FirebaseFirestore.getInstance()
        Log.d("XP_DEBUG", "handleXPChange CALLED with event=$event boardId=$boardId")

        db.collection("boards")
            .whereEqualTo("name", boardId)
            .get()
            .addOnSuccessListener { snapshot ->
                val board = snapshot.documents.firstOrNull()?.toObject(TaskBoard::class.java)
                    ?: return@addOnSuccessListener

                val xp = when (event) {
                    "CLAIM" -> board.xpClaim
                    "TODO_TO_IN_PROGRESS" -> board.xpTodoToInProgress
                    "TO_DONE" -> board.xpToDone
                    "TO_NEGLECTED" -> board.xpToNeglected
                    "NEGLECTED_RECOVERED" -> board.xpNeglectedRecovered
                    else -> 0
                }
                Log.d("XP_DEBUG", "XP resolved to $xp for event=$event")

                SignalManager.getInstance().vibrate()
                if (xp < 0) {
                    ssp.playSound(R.raw.lose_xp)
                } else if (xp > 0) {
                    ssp.playSound(R.raw.gain_xp)
                }

                if (xp != 0) {
                    addXP(xp, boardId)

                    val achievementIds = achievementEventTranslation(event)
                    achievementIds.forEach { achievementId ->
                        handleTaskAchievements(achievementId)
                    }
                    if (xp>0) {
                        handleTaskAchievements("reach_100_xp", xp)
                        handleTaskAchievements("reach_250_xp", xp)
                        handleTaskAchievements("reach_500_xp", xp)
                    }
                }
            }
    }

    //helper for achievement tracking
    fun achievementEventTranslation(event: String): List<String> {
        return when (event) {
            "CLAIM" -> listOf("first_task_claimed")
            "TO_DONE" -> listOf("first_task_done", "done_10_tasks")
            "TO_NEGLECTED" -> listOf("neglect_5_tasks")
            else -> emptyList()
        }
    }

    fun handleTaskAchievements(achievement: String? =null, incrementBy: Int?=1){
        val user = currentUser ?: return
        val email = user.email
        val activity = CurrentActivityProvider.getActivity()
        val rootLayout = activity?.findViewById<ViewGroup>(android.R.id.content)

        if (achievement != null && activity != null && rootLayout != null) {
            AchievementManager.incrementAchievementProgress(email, achievement, incrementBy) {
                AchievementManager.showAchievementPopup(activity, rootLayout, it)
            }
        }
    }

    fun checkAndApplyXPChange(
        oldStatus: Task.Status,
        newStatus: Task.Status,
        assignedTo: String?,
        boardName: String,
        context: Context
    ) {
        if (assignedTo == null || assignedTo != FirebaseAuth.getInstance().currentUser?.email)
            return

        val transition = when {
            oldStatus == Task.Status.TODO && newStatus == Task.Status.IN_PROGRESS -> "TODO_TO_IN_PROGRESS"
            newStatus == Task.Status.DONE -> "TO_DONE"
            newStatus == Task.Status.NEGLECTED -> "TO_NEGLECTED"
            oldStatus == Task.Status.NEGLECTED &&
                    (newStatus == Task.Status.TODO || newStatus == Task.Status.IN_PROGRESS) -> "NEGLECTED_RECOVERED"
            else -> null
        }
        transition?.let {
            handleXPChange(it, boardName, context)
        }
    }

    fun addXP(amount: Int, boardId: String? = null) {
        val user = currentUser ?: return
        val email = user.email
        val db = FirebaseFirestore.getInstance()

        // Update global user XP
        val userRef = db.collection("users").document(email)
        userRef.update("xp", FieldValue.increment(amount.toLong()))
            .addOnSuccessListener {
                currentUser?.xp = (currentUser?.xp ?: 0) + amount
            }

        // Update in the board if provided
        if (boardId != null) {
            db.collection("boards")
                .whereEqualTo("name", boardId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.documents.firstOrNull() ?: return@addOnSuccessListener
                    val board = doc.toObject(TaskBoard::class.java) ?: return@addOnSuccessListener

                    val updatedUsers = board.users.map {
                        if (it.email == email) it.copy(xp = it.xp + amount) else it
                    }

                    val updatedBoard = TaskBoard.Builder()
                        .name(board.name)
                        .description(board.description)
                        .users(updatedUsers)
                        .tasks(board.tasks)
                        .releaseDate(board.releaseDate)
                        .xpClaim(board.xpClaim)
                        .xpTodoToInProgress(board.xpTodoToInProgress)
                        .xpToDone(board.xpToDone)
                        .xpToNeglected(board.xpToNeglected)
                        .xpNeglectedRecovered(board.xpNeglectedRecovered)
                        .build()

                    db.collection("boards").document(doc.id).set(updatedBoard)
                }
        }



    }

    fun updateUserName(newName: String, onComplete: (Boolean) -> Unit) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return onComplete(false)
        val userRef = FirebaseFirestore.getInstance().collection("users").document(email)

        // Update name inside user document
        userRef.update("name", newName)
            .addOnSuccessListener {
                currentUser?.name = newName

                // Now update all board documents where this email appears
                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val batch = FirebaseFirestore.getInstance().batch()

                        for (doc in snapshot.documents) {
                            val board = doc.toObject(TaskBoard::class.java) ?: continue

                            val userExists = board.users.any { it.email == email }
                            if (!userExists) continue

                            val updatedUsers = board.users.map {
                                if (it.email == email) it.copy(name = newName) else it
                            }

                            val updatedBoard = TaskBoard.Builder()
                                .name(board.name)
                                .description(board.description)
                                .users(updatedUsers)
                                .tasks(board.tasks)
                                .releaseDate(board.releaseDate)
                                .xpClaim(board.xpClaim)
                                .xpTodoToInProgress(board.xpTodoToInProgress)
                                .xpToDone(board.xpToDone)
                                .xpToNeglected(board.xpToNeglected)
                                .xpNeglectedRecovered(board.xpNeglectedRecovered)


                                .build()

                            batch.set(doc.reference, updatedBoard)
                        }

                        batch.commit()
                            .addOnSuccessListener { onComplete(true) }
                            .addOnFailureListener { onComplete(false) }
                    }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun refreshTaskStatusCountsForUser(onComplete: (Boolean) -> Unit) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return onComplete(false)

        FirebaseFirestore.getInstance()
            .collection("boards")
            .get()
            .addOnSuccessListener { snapshot ->
                var todo = 0
                var inProgress = 0
                var done = 0
                var neglected = 0

                for (doc in snapshot.documents) {
                    val board = doc.toObject(TaskBoard::class.java) ?: continue
                    if (board.users.any { it.email == email }) {
                        for (task in board.tasks) {
                            if (task.assignedTo == email) {
                                when (task.status) {
                                    Task.Status.TODO -> todo++
                                    Task.Status.IN_PROGRESS -> inProgress++
                                    Task.Status.DONE -> done++
                                    Task.Status.NEGLECTED -> neglected++
                                }
                            }
                        }
                    }
                }

                val userRef = FirebaseFirestore.getInstance().collection("users").document(email)
                val updateMap = mapOf(
                    "tasksTodo" to todo,
                    "tasksInProgress" to inProgress,
                    "tasksDone" to done,
                    "tasksNeglected" to neglected
                )

                userRef.update(updateMap)
                    .addOnSuccessListener {
                        currentUser?.apply {
                            tasksTodo = todo
                            tasksInProgress = inProgress
                            tasksDone = done
                            tasksNeglected = neglected
                        }
                        onComplete(true)
                    }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun refreshUserName(onComplete: (String?) -> Unit) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return onComplete(null)
        val userRef = FirebaseFirestore.getInstance().collection("users").document(email)

        userRef.get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getString("name")
                if (name != null) {
                    currentUser?.name = name
                    onComplete(name)
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }


}
