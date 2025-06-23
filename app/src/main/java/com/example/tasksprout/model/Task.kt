package com.example.tasksprout.model

import java.io.Serializable
import java.time.LocalDate

data class Task(
    val name: String = "",
    val description: String = "",
    val releaseDate: String = "",
    val status: Status = Status.TODO,
    val assignedTo: String? = null
): Serializable {
    constructor() : this("", "", "", Status.TODO)

    class Builder {
        private var name: String = ""
        private var description: String = ""
        private var releaseDate: String = LocalDate.now().toString()
        private var status: Status = Status.TODO
        private var assignedTo: String? = null


        fun name(name: String) = apply { this.name = name }
        fun description(description: String) = apply { this.description = description }
        fun releaseDate(releaseDate: LocalDate) = apply { this.releaseDate = releaseDate.toString() }
        fun status(status: Status) = apply { this.status = status }
        fun assignedTo(email: String?) = apply { this.assignedTo = email }


        fun build() = Task(name, description, releaseDate, status, assignedTo)
    }

    enum class Status {
        TODO,
        IN_PROGRESS,
        DONE,
        NEGLECTED
    }
}

