package com.example.tasksprout.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasksprout.R
import com.example.tasksprout.adapters.TaskAdapter
import com.example.tasksprout.interfaces.Callback_TaskClicked
import com.example.tasksprout.interfaces.TaskInteractionHandler
import com.example.tasksprout.model.BoardUser
import com.example.tasksprout.model.Task
import com.example.tasksprout.model.TaskBoard
import com.example.tasksprout.model.TaskBoardDataManager
import com.example.tasksprout.model.TaskDataManager
import com.example.tasksprout.model.UserDataManager
import com.google.firebase.firestore.FirebaseFirestore

class TaskFragment : Fragment() {
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private val allTasks = mutableListOf<Task>()
    var TaskItemClicked: Callback_TaskClicked? = null
    private var currentStatus: Task.Status = Task.Status.TODO
    private lateinit var boardUsers: List<BoardUser>
    private lateinit var currentUserEmail: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        boardUsers = arguments?.getSerializable("board_users") as? List<BoardUser> ?: emptyList()
        currentUserEmail = arguments?.getString("current_user_email") ?: ""

        findViews(view)
        initRecyclerView()

        return view
    }



    private fun findViews(view: View) {
        recyclerView = view.findViewById(R.id.main_RV_list)
    }

    override fun onResume() {
        super.onResume()
        updateDisplayedTasks(currentStatus)
    }

    fun refreshTasksFromFirestore() {
        loadTasksFromFirestore()
    }

    private fun initRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

//        taskAdapter = TaskAdapter(emptyList())
        taskAdapter = TaskAdapter(emptyList(), boardUsers, currentUserEmail)

        recyclerView.adapter = taskAdapter

        taskAdapter.taskCallback = object : Callback_TaskClicked {
            override fun onClick(task: Task) {
                TaskDataManager.markTaskAsOpened(task.name)
                taskAdapter.taskInteractionHandler?.onEditTask(task)
            }
        }

        taskAdapter.taskInteractionHandler = object : TaskInteractionHandler {
            override fun onEditTask(task: Task) {
                val menu = parentFragmentManager.findFragmentById(R.id.main_FRAME_task_board_menu)
                        as? TaskUpperMenuFragment
                menu?.showTaskDialog(task)
            }

            override fun onDeleteTask(task: Task) {
                deleteTaskFromFirestore(task)
            }

            override fun onClaimTask(task: Task) {
                updateTaskInFirestore(task)
            }
        }

        loadTasksFromFirestore()
    }


    fun deleteTaskFromFirestore(task: Task) {
        val board = activity?.intent?.getSerializableExtra("board") as? TaskBoard ?: return

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
                    .build()

                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(doc.id)
                    .set(updatedBoard)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                        refreshTasksFromFirestore()
                    }
            }
    }


    private fun loadTasksFromFirestore() {
         val board = activity?.intent?.getSerializableExtra("board") as? TaskBoard ?: return

        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", board.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val boardDoc = snapshot.documents.firstOrNull()
                val updatedBoard = boardDoc?.toObject(TaskBoard::class.java)
                updatedBoard?.tasks?.let {
                    allTasks.clear()
                    allTasks.addAll(it)

                    // show TO DO by default
                    updateDisplayedTasks(Task.Status.TODO)
                }

            }
    }
    fun updateDisplayedTasks(status: Task.Status) {
        currentStatus = status
        val filtered = allTasks.filter { it.status == status }
        taskAdapter.updateList(filtered)
    }

    fun findTaskByNameAndDescription(name: String, desc: String): Task? {
        return allTasks.find { it.name == name && it.description == desc }
    }


    fun getTaskAt(index: Int): Task? {
        return if (index in allTasks.indices) allTasks[index] else null
    }

    fun updateTaskInFirestore(updatedTask: Task) {
        val board = activity?.intent?.getSerializableExtra("board") as? TaskBoard ?: return

        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", board.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                val boardDoc = doc?.toObject(TaskBoard::class.java) ?: return@addOnSuccessListener

                // Find the original task before updating
                val oldTask = boardDoc.tasks.find {
                    it.name == updatedTask.name && it.description == updatedTask.description
                }

                //  Check for XP change before updating Firestore
                if (oldTask != null && oldTask.status != updatedTask.status) {
                    UserDataManager.checkAndApplyXPChange(
                        oldStatus = oldTask.status,
                        newStatus = updatedTask.status,
                        assignedTo = updatedTask.assignedTo,
                        boardName = board.name
                    )
                }

                val updatedTasks = boardDoc.tasks.map {
                    if (it.name == updatedTask.name && it.description == updatedTask.description) {

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
                    .build()

                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(doc.id)
                    .set(updatedBoard)
                    .addOnSuccessListener {
                        refreshTasksFromFirestore()
                    }
            }
    }


}
