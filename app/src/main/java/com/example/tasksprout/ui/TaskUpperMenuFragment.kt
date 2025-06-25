package com.example.tasksprout.ui

import android.content.Intent
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
import com.example.tasksprout.R
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.tasksprout.databinding.FragmentTaskUpperMenuBinding
import com.example.tasksprout.model.Task
import com.example.tasksprout.model.TaskBoard
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import android.content.res.ColorStateList
import android.util.Log
import com.example.tasksprout.BoardSettingsActivity
import com.example.tasksprout.model.BoardUser
import com.example.tasksprout.model.Role
import com.example.tasksprout.model.TaskBoardDataManager
import com.example.tasksprout.model.TaskDataManager
import com.example.tasksprout.model.UserDataManager
import com.google.firebase.auth.FirebaseAuth
import com.example.tasksprout.ui.TaskFragment
import com.example.tasksprout.utilities.SignalManager


class TaskUpperMenuFragment : Fragment(R.layout.fragment_task_upper_menu) {

    private lateinit var taskBoard_BTN_settings: ImageButton
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
        val taskBoard= arguments?.getSerializable("board")as? TaskBoard
        taskBoard_BTN_settings =view.findViewById(R.id.taskBoard_BTN_settings)
        binding.taskBoardBTNSettings.setOnClickListener {
            val intent = Intent(requireContext(), BoardSettingsActivity::class.java)
            intent.putExtra("board", taskBoard)
            startActivity(intent)
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
            .assignedTo(taskToEdit?.assignedTo)
            .build()


        val boardIntent = (activity?.intent?.getSerializableExtra("board") as? TaskBoard) ?: return@setOnClickListener
        if (isEdit) {
            TaskDataManager.updateTaskInFirestore(task ,taskToEdit!!, boardIntent){
                refreshTaskFragment()
            }

        } else {
            TaskDataManager.addTaskToBoard(task, boardIntent){
                refreshTaskFragment()
            }
        }
        dialog.dismiss()
    }
    dialog.show()
}

    fun refreshTaskFragment() {
        val taskFragment = parentFragmentManager.findFragmentById(R.id.main_FRAME_tasks) as? TaskFragment
        taskFragment?.loadTasksFromFirestore()
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_user, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val emailET = dialogView.findViewById<EditText>(R.id.dialog_ET_user_email)
        val addBtn = dialogView.findViewById<Button>(R.id.dialog_BTN_add_user)

        addBtn.setOnClickListener {
            val newEmail = emailET.text.toString().trim()
            if (!isValidEmail(newEmail)) {
                SignalManager.getInstance().toast("Invalid or empty email")
                return@setOnClickListener
            }

            val board = getCurrentBoard() ?: return@setOnClickListener
            if (board.users.any { it.email == newEmail }) {
                SignalManager.getInstance().toast("User already on board")
                return@setOnClickListener
            }

            val updatedBoard = TaskBoardDataManager.buildUpdatedBoardWithNewUser(board, newEmail)
            TaskBoardDataManager.updateBoardInFirestore(board.name, updatedBoard, dialog)
        }
        dialog.show()
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun getCurrentBoard(): TaskBoard? {
        return activity?.intent?.getSerializableExtra("board") as? TaskBoard
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
//                    val board = activity?.intent?.getSerializableExtra("board") as? TaskBoard
//                    if (board != null) {
//                        UserDataManager.checkAndApplyXPChange(
//                            oldStatus = task.status,
//                            newStatus = newStatus,
//                            assignedTo = task.assignedTo,
//                            boardName = board.name
//                        )
//                        Toast.makeText(requireContext(), "XP checked", Toast.LENGTH_SHORT).show()
//                    }
                    val board = activity?.intent?.getSerializableExtra("board") as? TaskBoard ?: return@setOnDragListener true
                    TaskDataManager.updateTaskInFirestore(updatedTask,null, board){
                        refreshTaskFragment()
                    }

                    SignalManager.getInstance().toast("Moved to ${newStatus.name.lowercase().replace('_', ' ')}")
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