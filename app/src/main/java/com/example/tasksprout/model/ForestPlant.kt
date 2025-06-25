package com.example.tasksprout.model

import com.google.firebase.firestore.DocumentId
import com.example.tasksprout.model.Task.Status
import java.io.Serializable

data class ForestPlant(
    @DocumentId var id: String = "",
    val boardName: String = "",
    val taskName: String = "",
    val status: Status = Status.TODO,
    val posX: Float = 0f,
    val posY: Float = 0f
) : Serializable


//fun getDrawableForStatus(status: String): Int {
//    return when (status) {
//        "TO DO" -> R.drawable.plant_seed
//        "IN_PROGRESS" -> R.drawable.watering_plants
//        "DONE" -> R.drawable.plant_done
//        "NEGLECTED" -> R.drawable.plant_dead
//        else -> R.drawable.plant_seed
//    }
//}
