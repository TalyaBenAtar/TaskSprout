package com.example.tasksprout.model

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.tasksprout.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import com.example.tasksprout.CurrentActivityProvider
import com.example.tasksprout.utilities.SingleSoundPlayer
import com.google.firebase.auth.FirebaseAuth
import nl.dionsegijn.konfetti.core.PartyFactory
import java.util.Date
import java.util.Locale
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit





object AchievementManager {
    private val db = FirebaseFirestore.getInstance()
    private val cache = mutableMapOf<String, Achievement>()

    fun tryUnlock(userEmail: String, achievementId: String, onUnlocked: ((Achievement) -> Unit)? = null) {
        val userRef = db.collection("users").document(userEmail)

        userRef.get().addOnSuccessListener { doc ->
            val unlocked = doc["unlockedAchievements"] as? List<String> ?: listOf()
            if (achievementId in unlocked) return@addOnSuccessListener // already unlocked

            getAchievement(achievementId) { achievement ->
                // Add ID to unlocked list
                userRef.update("unlockedAchievements", FieldValue.arrayUnion(achievementId))
                // Grant XP
                userRef.update("xp", FieldValue.increment(achievement.xpReward.toLong()))
                // Show UI
                onUnlocked?.invoke(achievement)
            }
        }
    }

    private fun getAchievement(id: String, onResult: (Achievement) -> Unit) {
        cache[id]?.let { onResult(it) } ?: db.collection("achievements").document(id).get()
            .addOnSuccessListener {
                val a = it.toObject(Achievement::class.java)
                if (a != null) {
                    cache[id] = a
                    onResult(a)
                }
            }
    }

    fun showAchievementPopup(context: Context, view: ViewGroup, achievement: Achievement) {
        val popup = LayoutInflater.from(context).inflate(R.layout.view_achievement_popup, view, false)
        val confettiContainer = LayoutInflater.from(context).inflate(R.layout.view_confetti_overlay, view, false)
        val konfettiView = confettiContainer.findViewById<KonfettiView>(R.id.konfettiView)

        val icon = popup.findViewById<ImageView>(R.id.popupIcon)
        val title = popup.findViewById<TextView>(R.id.popupTitle)
        val xpText = popup.findViewById<TextView>(R.id.popupXP)

        val emitterConfig = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)

        title.text = "Achievement Unlocked: ${achievement.title}"
        icon.setImageResource(getIconForType(achievement.type))
        xpText.text = "+${achievement.xpReward} XP"

        Log.d("ACHIEVEMENT_POPUP", "Preparing popup for: ${achievement.title}")

        popup.elevation = 20f
        popup.bringToFront()

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 150
        }
        popup.layoutParams = layoutParams

        konfettiView?.z = 0f
        popup.z = 1f


        view.addView(popup)
        playSoundEffect(context)

        Log.d("POPUP_DEBUG", "Popup added with title: ${achievement.title}")

        // Start popup animation
        popup.visibility = View.VISIBLE
        popup.alpha = 0f
        popup.animate().alpha(1f).setDuration(300).start()



        // Confetti time 🎉
        confettiContainer.visibility = View.VISIBLE
        konfettiView?.apply {
            visibility = View.VISIBLE
            start(
                PartyFactory(emitterConfig)
                    .angle(270)
                    .spread(90)
                    .timeToLive(2000L)
                    .position(0.5, 1.0)
                    .build()
            )
        }
        view.addView(confettiContainer)
        Log.d("POPUP_VIEW", "Popup alpha=${popup.alpha}, visibility=${popup.visibility}, parent=${popup.parent != null}")

        Handler(Looper.getMainLooper()).postDelayed({
            popup.animate().alpha(0f).setDuration(300).withEndAction {
                view.removeView(popup)
            }.start()
        }, 5000)
    }

    fun trackDailyUsageAndUpdateProgress() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(user.email!!)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        userRef.get().addOnSuccessListener { doc ->
            val usedDays = (doc["usedDays"] as? List<String>)?.toMutableSet() ?: mutableSetOf()

            if (!usedDays.contains(today)) {
                usedDays.add(today)
                userRef.update("usedDays", usedDays.toList())

                val dayCount = usedDays.size
                val unlockedAchievements = doc["unlockedAchievements"] as? List<String> ?: listOf()

                val achievementsToCheck = listOf("used_5_days", "used_7_days")
                val activity = CurrentActivityProvider.getActivity()
                val rootLayout = activity?.findViewById<ViewGroup>(android.R.id.content)

                achievementsToCheck.forEach { achievementId ->
                    getAchievement(achievementId) { achievement ->
                        // Update progress
                        userRef.update("achievementProgress.$achievementId", dayCount)

                        // If not unlocked and requirement met — unlock + popup
                        if (!unlockedAchievements.contains(achievementId) &&
                            dayCount >= achievement.targetCount
                        ) {
                            userRef.update("unlockedAchievements", FieldValue.arrayUnion(achievementId))
                            userRef.update("xp", FieldValue.increment(achievement.xpReward.toLong()))

                            if (activity != null && rootLayout != null) {
                                showAchievementPopup(activity, rootLayout, achievement)
                            }
                        }
                    }
                }
            }
        }
    }




    fun playSoundEffect(context: Context) {
        var ssp = SingleSoundPlayer(context)
        ssp.playSound(R.raw.achievement_bell)
    }

    fun getIconForType(type: AchievementType): Int {
        return when (type) {
            AchievementType.TASK -> R.drawable.task
            AchievementType.BOARD -> R.drawable.task_board
            AchievementType.FOREST -> R.drawable.forest
            AchievementType.XP -> R.drawable.xp
            AchievementType.TIME -> R.drawable.time_passing
        }
    }

    fun incrementAchievementProgress(
        userEmail: String,
        achievementId: String,
        onUnlocked: ((Achievement) -> Unit)? = null
    ) {
        val userRef = db.collection("users").document(userEmail)

        getAchievement(achievementId) { achievement ->
            userRef.get().addOnSuccessListener { doc ->
                val unlocked = doc["unlockedAchievements"] as? List<String> ?: listOf()
                if (achievementId in unlocked) return@addOnSuccessListener

                val progressMap = doc["achievementProgress"] as? Map<String, Long> ?: mapOf()
                val currentProgress = progressMap[achievementId]?.toInt() ?: 0
                val newProgress = currentProgress + 1

                userRef.update("achievementProgress.${achievementId}", newProgress)

                if (newProgress >= achievement.targetCount) {
                    userRef.update("unlockedAchievements", FieldValue.arrayUnion(achievementId))
                    userRef.update("xp", FieldValue.increment(achievement.xpReward.toLong()))
                    onUnlocked?.invoke(achievement)
                }
            }
        }
    }


    fun createDefaultAchievementsInFirestore() {
        val achievements = listOf(
            Achievement("first_task_done", "Task Slayer", "Completed your first task", 20, AchievementType.TASK, 1),
            Achievement("first_task_claimed", "Claimed It!", "Claimed your first task", 15, AchievementType.TASK, 1),
            Achievement("joined_board", "Boarding Now", "Joined your first board", 10, AchievementType.BOARD, 1),
            Achievement("first_forest_plant", "Plant Parent", "Grew your first plant", 15, AchievementType.FOREST, 1),
            Achievement("reach_100_xp", "XP Rookie", "Reached 100 XP", 25, AchievementType.XP, 100),
            Achievement("used_5_days", "Frequent Flyer", "Used the app for 5 days", 30, AchievementType.TIME, 5),
            Achievement("done_10_tasks", "Task Machine", "Completed 10 tasks", 50, AchievementType.TASK, targetCount = 10),
            Achievement("neglect_5_tasks", "Oopsie!", "Neglected 5 tasks", 20, AchievementType.TASK, targetCount = 5),
            Achievement("plant_5", "Growing Strong", "Have 5 plants in the forest", 30, AchievementType.FOREST, targetCount = 5),
            Achievement("used_7_days", "Week Warrior", "Used the app for 7 days", 35, AchievementType.TIME, targetCount = 7)

        )

        val ref = db.collection("achievements")
        for (a in achievements) {
            ref.document(a.id).set(a)
        }
    }

}


