package com.example.tasksprout.model
import java.io.Serializable


data class TaskBoard(
    val name: String = "",
    val users: List<BoardUser> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val description: String = "",
    val releaseDate: String = "",
    var xpClaim: Int =5,
    var xpTodoToInProgress: Int = 10,
    var xpToDone: Int=50,
    var xpToNeglected: Int = -30,
    var xpNeglectedRecovered:Int =15

) : Serializable{
    class Builder {
        private var name: String = ""
        private var users: MutableList<BoardUser> = mutableListOf()
        private var tasks: MutableList<Task> = mutableListOf()
        private var description: String = ""
        private var releaseDate: String = ""

        private var xpClaim: Int = 5
        private var xpTodoToInProgress: Int = 10
        private var xpToDone: Int = 50
        private var xpToNeglected: Int = -30
        private var xpNeglectedRecovered: Int = 15


        fun name(name: String) = apply { this.name = name }
        fun releaseDate(releaseDate: String) = apply { this.releaseDate = releaseDate }

        fun addUser(user: BoardUser) = apply { this.users.add(user) }
        fun users(users: List<BoardUser>) = apply {
            this.users.clear()
            this.users.addAll(users)
        }

        fun addTask(task: Task) = apply { this.tasks.add(task) }
        fun tasks(tasks: List<Task>) = apply {
            this.tasks.clear()
            this.tasks.addAll(tasks)
        }

        fun description(description: String) = apply { this.description = description }

        fun xpClaim(value: Int) = apply { this.xpClaim = value }
        fun xpTodoToInProgress(value: Int) = apply { this.xpTodoToInProgress = value }
        fun xpToDone(value: Int) = apply { this.xpToDone = value }
        fun xpToNeglected(value: Int) = apply { this.xpToNeglected = value }
        fun xpNeglectedRecovered(value: Int) = apply { this.xpNeglectedRecovered = value }

        fun build() = TaskBoard(
            name, users, tasks, description, releaseDate,
            xpClaim, xpTodoToInProgress, xpToDone, xpToNeglected, xpNeglectedRecovered
        )

    }
}
