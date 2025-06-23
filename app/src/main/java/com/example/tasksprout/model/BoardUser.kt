package com.example.tasksprout.model

import java.io.Serializable

data class BoardUser(
    val email: String = "",
    val name: String = "Anonymous",
    val role: Role = Role.MEMBER,
    var xp: Int = 0
) : Serializable




enum class Role {
    MANAGER,
    MEMBER
}