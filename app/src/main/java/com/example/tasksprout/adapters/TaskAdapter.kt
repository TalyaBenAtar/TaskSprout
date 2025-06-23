package com.example.tasksprout.adapters

import android.content.ClipData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.tasksprout.databinding.TaskItemBinding
import com.example.tasksprout.interfaces.Callback_TaskClicked
import com.example.tasksprout.model.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.core.content.ContextCompat
import com.example.tasksprout.R
import com.example.tasksprout.interfaces.TaskInteractionHandler
import com.example.tasksprout.model.BoardUser
import com.example.tasksprout.model.TaskBoard
import com.example.tasksprout.model.TaskDataManager
import com.example.tasksprout.model.UserDataManager


class TaskAdapter(
    private var tasks: List<Task>,
    private val boardUsers: List<BoardUser>,
    private val currentUserEmail: String
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    var taskInteractionHandler: TaskInteractionHandler? = null
    var taskCallback: Callback_TaskClicked? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = TaskItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    fun updateList(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int = tasks.size


    override fun onBindViewHolder(holder: TaskViewHolder, index: Int) {
        holder.bind(tasks[index])
    }


    inner class TaskViewHolder(private val binding: TaskItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            //"assgned to: name" logic
            val assignedToName = boardUsers.find { it.email == task.assignedTo }?.name ?: "Unknown"

            if (task.assignedTo == null) {
                binding.taskBTNClaim.visibility = View.VISIBLE
                binding.taskLBLAssignedTo.visibility = View.INVISIBLE

                binding.taskBTNClaim.setOnClickListener {
                    val claimedTask = task.copy(assignedTo = currentUserEmail)

                    // ✅ Award XP for claiming the task
                    val context = itemView.context
                    val boardName = if (context is AppCompatActivity) {
                        val board = context.intent.getSerializableExtra("board") as? TaskBoard
                        board?.name
                    } else null

                    if (boardName != null) {
                        UserDataManager.handleXPChange("CLAIM", boardName)
                    }

                    taskInteractionHandler?.onClaimTask(claimedTask)
                }
            } else {
                binding.taskBTNClaim.visibility = View.GONE
                binding.taskLBLAssignedTo.visibility = View.VISIBLE
                binding.taskLBLAssignedTo.text = "Assigned to: $assignedToName"
            }

            //"NEW" bubble logic
            val context = itemView.context
            if (com.example.tasksprout.model.TaskDataManager.hasTaskBeenOpened(task.name)) {
                binding.taskLBLNewBubble.visibility = View.INVISIBLE
            } else {
                binding.taskLBLNewBubble.visibility = View.VISIBLE
            }

            binding.taskLBLName.text = task.name
            binding.taskLBLDescription.text = task.description

            try {
                val parsedDate = LocalDate.parse(task.releaseDate)
                val formattedDate = parsedDate.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
                binding.taskLBLAddedDate.text = formattedDate
            } catch (_: Exception) {
                binding.taskLBLAddedDate.text = task.releaseDate
            }


            // Set color and image based on status
            when (task.status) {
                Task.Status.TODO -> {
                    binding.taskCVData.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.blue_400)
                    )
                    binding.taskIMGStage.setImageDrawable(
                        ContextCompat.getDrawable(binding.root.context, R.drawable.plant_seed)
                    )
                }
                Task.Status.IN_PROGRESS -> {
                    binding.taskCVData.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.IN_PROGRESS_orange)
                    )
                    binding.taskIMGStage.setImageDrawable(
                        ContextCompat.getDrawable(binding.root.context, R.drawable.watering_plants)
                    )
                }
                Task.Status.DONE -> {
                    binding.taskCVData.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.DONE_green)
                    )
                    binding.taskIMGStage.setImageDrawable(
                        ContextCompat.getDrawable(binding.root.context, R.drawable.plant_done)
                    )
                }
                Task.Status.NEGLECTED -> {
                    binding.taskCVData.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.NEGLECT_red)
                    )
                    binding.taskIMGStage.setImageDrawable(
                        ContextCompat.getDrawable(binding.root.context, R.drawable.plant_dead)
                    )
                }
            }

            binding.taskCVData.setOnClickListener {
                TaskDataManager.markTaskAsOpened(task.name)
                notifyItemChanged(adapterPosition) // optional: update just this item
                showTaskOptionsPopup(it, task)
            }


            binding.root.setOnLongClickListener {
                val dragData = ClipData.newPlainText("task_key", "${task.name}|${task.description}")
                val shadow = View.DragShadowBuilder(it)
                it.startDragAndDrop(dragData, shadow, it, 0)
                true
            }


        }
    }

    private fun showTaskOptionsPopup(view: View, task: Task) {
        val context = view.context
        val popupView = LayoutInflater.from(context).inflate(R.layout.dialog_task_options, null)
        val popup = AlertDialog.Builder(context).setView(popupView).create()

        val editBtn = popupView.findViewById<Button>(R.id.dialog_BTN_edit_task)
        val deleteBtn = popupView.findViewById<Button>(R.id.dialog_BTN_delete_task)

        editBtn.setOnClickListener {
            popup.dismiss()
            taskInteractionHandler?.onEditTask(task)
        }

        deleteBtn.setOnClickListener {
            popup.dismiss()
            taskInteractionHandler?.onDeleteTask(task)
        }

        popup.show()
    }




}
