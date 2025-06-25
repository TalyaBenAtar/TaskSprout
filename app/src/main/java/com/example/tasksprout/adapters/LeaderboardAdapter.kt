package com.example.tasksprout.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasksprout.R
import com.example.tasksprout.model.BoardUser

class LeaderboardAdapter(private val users: List<BoardUser>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.leaderboard_TXT_name)
        val xp: TextView = itemView.findViewById(R.id.leaderboard_TXT_xp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.name.text = user.name
        holder.xp.text = "${user.xp} XP"
    }

    override fun getItemCount(): Int = users.size
}
