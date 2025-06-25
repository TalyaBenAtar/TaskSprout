package com.example.tasksprout.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasksprout.databinding.BoardItemBinding
import com.example.tasksprout.interfaces.Callback_BoardClicked
import com.example.tasksprout.model.TaskBoard
import com.example.tasksprout.model.TaskBoardDataManager


class BoardAdapter(
    private val boards: List<TaskBoard>
) : RecyclerView.Adapter<BoardAdapter.BoardViewHolder>() {

    var boardCallback: Callback_BoardClicked? = null
//    val memberCountTextView: TextView = itemView.findViewById(R.id.board_item_member_count)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = BoardItemBinding.inflate(inflater, parent, false)
        return BoardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val board = boards[position]
        holder.bind(board)
    }

    override fun getItemCount() = boards.size

    inner class BoardViewHolder(private val binding: BoardItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(board: TaskBoard) {
            val context = itemView.context
            if (TaskBoardDataManager.hasBoardBeenOpened(board.name)) {
                binding.boardLBLNewBubble.visibility = View.INVISIBLE
            } else {
                binding.boardLBLNewBubble.visibility = View.VISIBLE
            }

            binding.boardLBLAddedDate.text = board.releaseDate
            binding.boardLBLName.text = board.name
            binding.boardLBLDescription.text = board.description

            val memberCount = board.users.size
            binding.boardItemMemberCount.text = "$memberCount member${if (memberCount != 1) "s" else ""}"

            binding.root.setOnClickListener {
                boardCallback?.onBoardClicked(board)
            }

            binding.root.setOnLongClickListener {
                boardCallback?.onBoardLongClicked(board)
                true
            }


        }
    }
}
