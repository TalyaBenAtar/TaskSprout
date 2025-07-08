package com.example.tasksprout.model

data class Achievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val xpReward: Int = 0,
    val type: AchievementType = AchievementType.TASK,
    val targetCount: Int = 1
)


enum class AchievementType {
    TASK,
    BOARD,
    FOREST,
    XP,
    TIME
}
