package com.example.tasksprout.ui

import android.os.Bundle
import android.view.DragEvent
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import com.example.tasksprout.R
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.tasksprout.databinding.FragmentTaskUpperMenuBinding
import com.example.tasksprout.model.Task
import com.example.tasksprout.model.TaskBoard
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import android.content.res.ColorStateList
import com.example.tasksprout.model.BoardUser
import com.example.tasksprout.model.Role
import com.example.tasksprout.model.UserDataManager
import com.google.firebase.auth.FirebaseAuth


class TaskUpperMenuFragment : Fragment(R.layout.fragment_task_upper_menu) {

    private var _binding: FragmentTaskUpperMenuBinding? = null
    private val binding get() = _binding!!
    private lateinit var addButton: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskUpperMenuBinding.bind(view)
        addButton = view.findViewById(R.id.taskBoard_BTN_add)
        addButton.setOnClickListener {
            showAddOptionsDialog()
        }
        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.taskBoardBTNTodo.setOnClickListener {
            getTaskFragment()?.updateDisplayedTasks(Task.Status.TODO)
        }
        binding.taskBoardBTNInProgress.setOnClickListener {
            getTaskFragment()?.updateDisplayedTasks(Task.Status.IN_PROGRESS)
        }
        binding.taskBoardBTNDone.setOnClickListener {
            getTaskFragment()?.updateDisplayedTasks(Task.Status.DONE)
        }
        binding.taskBoardBTNNeglect.setOnClickListener {
            getTaskFragment()?.updateDisplayedTasks(Task.Status.NEGLECTED)
        }

        setupDropTarget(binding.taskBoardBTNTodo, Task.Status.TODO)
        setupDropTarget(binding.taskBoardBTNInProgress, Task.Status.IN_PROGRESS)
        setupDropTarget(binding.taskBoardBTNDone, Task.Status.DONE)
        setupDropTarget(binding.taskBoardBTNNeglect, Task.Status.NEGLECTED)

    }

    private fun getTaskFragment(): TaskFragment? {
        return parentFragmentManager.findFragmentById(R.id.main_FRAME_tasks) as? TaskFragment
    }

    private fun showAddOptionsDialog() {
        val optionsDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_options, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(optionsDialogView).create()

        val addTaskBtn = optionsDialogView.findViewById<Button>(R.id.dialog_BTN_add_task)
        val addUserBtn = optionsDialogView.findViewById<Button>(R.id.dialog_BTN_add_user)

        addTaskBtn.setOnClickListener {
            dialog.dismiss()
            showTaskDialog()
        }
        addUserBtn.setOnClickListener {
            dialog.dismiss()
            showAddUserDialog()
        }

        dialog.show()
    }


fun showTaskDialog(taskToEdit: Task? = null) {
    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
    val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

    val nameET = dialogView.findViewById<EditText>(R.id.dialog_ET_task_name)
    val descET = dialogView.findViewById<EditText>(R.id.dialog_ET_task_description)
    val statusSpinner = dialogView.findViewById<Spinner>(R.id.dialog_spinner_status)
    val addBtn = dialogView.findViewById<Button>(R.id.dialog_BTN_add_task)

    // Spinner setup
    val adapter = ArrayAdapter.createFromResource(
        requireContext(),
        R.array.task_status_array,
        R.layout.spinner_item
    )
    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
    statusSpinner.adapter = adapter

    val isEdit = taskToEdit != null
    if (isEdit) {
        nameET.setText(taskToEdit!!.name)
        descET.setText(taskToEdit.description)
        statusSpinner.setSelection(taskToEdit.status.ordinal)
        addBtn.text = "Save"
    }

    addBtn.setOnClickListener {
        val name = nameET.text.toString().trim()
        val description = descET.text.toString().trim()
        val selectedText = statusSpinner.selectedItem.toString()

        val status = when (selectedText) {
            "To Do" -> Task.Status.TODO
            "In Progress" -> Task.Status.IN_PROGRESS
            "Done" -> Task.Status.DONE
            "Neglected" -> Task.Status.NEGLECTED
            else -> Task.Status.TODO
        }

        val task = Task.Builder()
            .name(name)
            .description(description)
            .status(status)
            // ⚠️ Preserve assignedTo if editing, null if new
            .assignedTo(taskToEdit?.assignedTo)
            .build()

        if (isEdit) {
            updateTaskInFirestore(taskToEdit!!, task)
        } else {
            addTaskToBoard(task)
        }
        dialog.dismiss()
    }
    dialog.show()
}

    private fun addTaskToBoard(task: Task) {
        val boardIntent = (activity?.intent?.getSerializableExtra("board") as? TaskBoard) ?: return

        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", boardIntent.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                val fullBoard = doc?.toObject(TaskBoard::class.java)
                if (fullBoard == null || doc == null) return@addOnSuccessListener

                val updatedTasks = fullBoard.tasks.toMutableList()
                updatedTasks.add(task)  // Make sure this task has the correct status

                val updatedBoard = TaskBoard.Builder()
                    .name(fullBoard.name)
                    .description(fullBoard.description)
                    .users(fullBoard.users)
                    .tasks(updatedTasks)
                    .releaseDate(fullBoard.releaseDate)
                    .build()

                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(doc.id)
                    .set(updatedBoard)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Task added", Toast.LENGTH_SHORT).show()

                        val taskFragment = parentFragmentManager.findFragmentById(R.id.main_FRAME_tasks) as? TaskFragment
                        taskFragment?.refreshTasksFromFirestore()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to add task", Toast.LENGTH_SHORT).show()
                    }
            }
    }


    private fun updateTaskInFirestore(oldTask: Task, newTask: Task) {
        val board = activity?.intent?.getSerializableExtra("board") as? TaskBoard ?: return

        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", board.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                val boardDoc = doc?.toObject(TaskBoard::class.java) ?: return@addOnSuccessListener

                // Check for XP change before updating Firestore
                val board = activity?.intent?.getSerializableExtra("board") as? TaskBoard
                if (board != null) {
                    UserDataManager.checkAndApplyXPChange(
                        oldStatus = oldTask.status,
                        newStatus = newTask.status,
                        assignedTo = oldTask.assignedTo,
                        boardName = board.name
                    )
                }

                val updatedTasks = boardDoc.tasks.map {
                    if (it.name == oldTask.name && it.description == oldTask.description)
                        newTask else it
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
                        Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show()

                        val taskFragment = parentFragmentManager.findFragmentById(R.id.main_FRAME_tasks) as? TaskFragment
                        taskFragment?.refreshTasksFromFirestore()
                    }
            }
    }


    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_user, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val emailET = dialogView.findViewById<EditText>(R.id.dialog_ET_user_email)
        val addBtn = dialogView.findViewById<Button>(R.id.dialog_BTN_add_user)

        addBtn.setOnClickListener {
            val newEmail = emailET.text.toString().trim()
            if (!isValidEmail(newEmail)) {
                Toast.makeText(requireContext(), "Invalid or empty email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val board = getCurrentBoard() ?: return@setOnClickListener
            if (board.users.any { it.email == newEmail }) {
                Toast.makeText(requireContext(), "User already on board", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedBoard = buildUpdatedBoardWithNewUser(board, newEmail)
            updateBoardInFirestore(board.name, updatedBoard, dialog)
        }

        dialog.show()
    }


    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun getCurrentBoard(): TaskBoard? {
        return activity?.intent?.getSerializableExtra("board") as? TaskBoard
    }

    private fun buildUpdatedBoardWithNewUser(board: TaskBoard, newEmail: String): TaskBoard {
        val newUser = BoardUser(email = newEmail, name = "Anonymous", role = Role.MEMBER)

        return TaskBoard.Builder()
            .name(board.name)
            .description(board.description)
            .users(board.users + newUser)
            .tasks(board.tasks)
            .releaseDate(board.releaseDate)
            .build()
    }

    private fun updateBoardInFirestore(
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
                            Toast.makeText(requireContext(), "User added to board", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to update board", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Board not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error accessing Firestore", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupDropTarget(button: MaterialButton, newStatus: Task.Status) {
        val originalTint = button.backgroundTintList
        val highlightTint = getLighterColorForStatus(newStatus)

        button.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> {
                    button.backgroundTintList = ColorStateList.valueOf(highlightTint)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED,
                DragEvent.ACTION_DRAG_ENDED -> {
                    button.backgroundTintList = originalTint
                    true
                }
                DragEvent.ACTION_DROP -> {
                    button.backgroundTintList = originalTint

                    val taskKey = event.clipData.getItemAt(0).text.toString()
                    val parts = taskKey.split("|")
                    if (parts.size != 2) return@setOnDragListener true

                    val name = parts[0]
                    val desc = parts[1]

                    val taskFragment = getTaskFragment() ?: return@setOnDragListener true
                    val task = taskFragment.findTaskByNameAndDescription(name, desc) ?: return@setOnDragListener true

                    val updatedTask = task.copy(status = newStatus)

                    // 🔧 Apply XP change logic before updating Firestore
                    val board = activity?.intent?.getSerializableExtra("board") as? TaskBoard
                    if (board != null) {
                        UserDataManager.checkAndApplyXPChange(
                            oldStatus = task.status,
                            newStatus = newStatus,
                            assignedTo = task.assignedTo,
                            boardName = board.name
                        )
                    }

                    taskFragment.updateTaskInFirestore(updatedTask)

                    Toast.makeText(requireContext(), "Moved to ${newStatus.name.lowercase().replace('_', ' ')}", Toast.LENGTH_SHORT).show()

                    true
                }
                else -> true
            }
        }
    }

    private fun getLighterColorForStatus(status: Task.Status): Int {
        val context = requireContext()
        return when (status) {
            Task.Status.TODO -> ContextCompat.getColor(context, R.color.blue_200)
            Task.Status.IN_PROGRESS -> ContextCompat.getColor(context, R.color.orange_200)
            Task.Status.DONE -> ContextCompat.getColor(context, R.color.green_200)
            Task.Status.NEGLECTED -> ContextCompat.getColor(context, R.color.red_200)
        }
    }




}