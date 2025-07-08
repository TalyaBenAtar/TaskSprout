package com.example.tasksprout.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.tasksprout.R
import com.example.tasksprout.model.Achievement
import com.example.tasksprout.model.AchievementManager

class AchievementAdapter(
    private val context: Context,
    private val achievements: List<Achievement>,
    private val unlocked: List<String>,
    private val progress: Map<String, Long>
) : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun getItemCount(): Int = achievements.size

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        val isUnlocked = achievement.id in unlocked
        var currentProgress = progress[achievement.id]?.toInt() ?: 0
        val target = achievement.targetCount

        if (currentProgress>target) currentProgress=target

        holder.title.text = achievement.title
        holder.description.text = achievement.description
        holder.icon.setImageResource(AchievementManager.getIconForType(achievement.type))

        holder.progressBar.max = target
        holder.progressBar.progress = currentProgress
        holder.progressText.text = "$currentProgress / $target"

        if (isUnlocked) {
            holder.statusIcon.setImageResource(R.drawable.check)
        } else {
            holder.statusIcon.setImageResource(R.drawable.lock)
        }

        val cardColor = if (isUnlocked)
            R.color.achievement_unlocked
        else
            R.color.achievement_locked

        holder.card.setBackgroundColor(context.getColor(cardColor))
    }

    inner class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: View = itemView.findViewById(R.id.achievement_card)
        val title: TextView = itemView.findViewById(R.id.achievement_title)
        val description: TextView = itemView.findViewById(R.id.achievement_desc)
        val icon: ImageView = itemView.findViewById(R.id.achievement_icon)
        val progressText: TextView = itemView.findViewById(R.id.achievement_progress_text)
        val progressBar: ProgressBar = itemView.findViewById(R.id.achievement_progress_bar)
        val statusIcon: ImageView = itemView.findViewById(R.id.achievement_status_icon)
    }
}
