package com.example.tasksprout.model

import java.io.Serializable

data class User(
    val email: String = "",
    var name: String = "",
    var xp: Int = 0,
    var tasksTodo: Int = 0,
    var tasksInProgress: Int = 0,
    var tasksDone: Int = 0,
    var tasksNeglected: Int = 0,
    var unlockedAchievements: List<String> = emptyList(),
    var achievementProgress: Map<String, Int> = emptyMap()
) : Serializable
