package com.example.tasksprout.model

import com.google.firebase.firestore.DocumentId
import com.example.tasksprout.model.Task.Status
import java.io.Serializable

data class ForestPlant(
    @DocumentId var id: String = "",
    val boardName: String = "",
    val taskName: String = "",
    var status: Status = Status.TODO,
    var posX: Float = 0f,
    var posY: Float = 0f
) : Serializable


