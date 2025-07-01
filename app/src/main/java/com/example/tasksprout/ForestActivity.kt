package com.example.tasksprout

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.tasksprout.databinding.ActivityForestBinding
import com.example.tasksprout.model.ForestPlant
import com.example.tasksprout.model.Task
import com.example.tasksprout.model.TaskBoard
import com.example.tasksprout.utilities.SignalManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ForestActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityForestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.forestBTNResetPositions.setOnClickListener {
            resetPlantPositions()
        }

        binding.forestBackButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        Log.d("FOREST_DEBUG", "Binding initialized: ${binding.forestBoundsFrame}")

        generateForestIfNeeded()
    }

    private fun generateForestIfNeeded() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val forestRef = db.collection("users").document(userEmail).collection("forestPlants")
        val boardRef = db.collection("boards")

        Log.d("FOREST_DEBUG", "Fetching forest for user: $userEmail")

        val existingPlants = mutableListOf<ForestPlant>()
        val validTaskKeys = mutableSetOf<String>()

        forestRef.get().addOnSuccessListener { forestSnapshot ->
            existingPlants.addAll(forestSnapshot.toObjects(ForestPlant::class.java))

            boardRef.get().addOnSuccessListener { boardSnapshots ->
                val taskRequests = mutableListOf<com.google.android.gms.tasks.Task<*>>()

                for (boardDoc in boardSnapshots) {
                    val board = boardDoc.toObject(TaskBoard::class.java)
                    val boardName = board.name ?: continue

                    val isUserInBoard = board.users.any { it.email == userEmail }
                    if (!isUserInBoard) continue

                    Log.d("FOREST_DEBUG", "User is in board: $boardName")

                    val tasks = board.tasks ?: emptyList()
                    for (task in tasks) {
                        if (task.assignedTo != userEmail) continue

                        val taskName = task.name ?: continue
                        val status = task.status

                        val taskKey = "$boardName::$taskName"
                        validTaskKeys.add(taskKey)

                        val alreadyExists = existingPlants.any {
                            it.boardName == boardName && it.taskName == taskName
                        }

                        if (alreadyExists) {
                            Log.d("FOREST_DEBUG", "🌿 Already exists: $taskName on $boardName")
                            continue
                        }

                        val screenWidth = resources.displayMetrics.widthPixels
                        val screenHeight = resources.displayMetrics.heightPixels
                        val newPos = generateUniquePosition(existingPlants)

                        if (newPos != null) {
                            val newPlant = ForestPlant(
                                boardName = boardName,
                                taskName = taskName,
                                status = status,
                                posX = newPos.first,
                                posY = newPos.second
                            )
                            Log.d("FOREST_DEBUG", "🌱 Creating plant: $taskName on board $boardName with status $status at $newPos")

                            forestRef.add(newPlant)
                                .addOnSuccessListener {
                                    Log.d("FOREST_DEBUG", "✅ Successfully added forest plant $taskName")
                                }
                                .addOnFailureListener {
                                    Log.e("FOREST_DEBUG", "❌ Failed to add forest plant $taskName", it)
                                }

                            existingPlants.add(newPlant)
                        } else {
                            Log.w("FOREST_DEBUG", "⚠️ Could not place plant: $taskName, no room")
                        }
                    }
                }

                // Remove orphaned plants
                for (plant in existingPlants) {
                    val plantKey = "${plant.boardName}::${plant.taskName}"
                    if (plantKey !in validTaskKeys) {
                        forestRef
                            .whereEqualTo("boardName", plant.boardName)
                            .whereEqualTo("taskName", plant.taskName)
                            .get()
                            .addOnSuccessListener { docs ->
                                for (doc in docs) doc.reference.delete()
                            }
                    }
                }

                loadAndDisplayForest()
            }
        }
    }

    private fun loadAndDisplayForest() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val forestRef = db.collection("users").document(userEmail).collection("forestPlants")



        forestRef.get().addOnSuccessListener { snapshot ->
            Log.d("FOREST_DEBUG", "Loading and displaying forest")
            val plants = snapshot.toObjects(ForestPlant::class.java)
            Log.d("FOREST_DEBUG", "Found ${plants.size} plants in Firestore")

            for (plant in plants) {
                addPlantToView(plant)
            }
        }.addOnFailureListener {
        Log.e("FOREST_DEBUG", "Failed to load forest plants", it)
    }
    }

    private fun getDrawableForStatus(status: Task.Status): Int {
        return when (status) {
            Task.Status.TODO -> R.drawable.plant_seed
            Task.Status.IN_PROGRESS -> R.drawable.watering_plants
            Task.Status.DONE -> R.drawable.plant_done
            Task.Status.NEGLECTED -> R.drawable.plant_dead
        }
    }

    private fun addPlantToView(plant: ForestPlant) {
        val imageView = ImageView(this)
        val size = 120
        val layoutParams = FrameLayout.LayoutParams(size, size)
        layoutParams.leftMargin = plant.posX.toInt()
        layoutParams.topMargin = plant.posY.toInt()
        imageView.layoutParams = layoutParams
        try {
            imageView.setImageResource(getDrawableForStatus(plant.status))
            Log.d(
                "FOREST_DEBUG",
                "Drawing plant: ${plant.taskName} at (${plant.posX}, ${plant.posY}) with status ${plant.status}"
            )
        }catch (e: Exception) {
            Log.e("FOREST_DEBUG", "Failed to set image for plant status: ${plant.status}", e)
        }
        imageView.setOnClickListener {
            val message = "🌱 ${plant.taskName}\n📋 ${plant.boardName}\n📌 ${plant.status}"
            SignalManager.getInstance().toast(message)
        }

        binding.forestBoundsFrame.addView(imageView)
    }

    private fun generateUniquePosition(
        existingPlants: List<ForestPlant>
    ): Pair<Float, Float>? {
        val margin = 130
        val minDistance = 150
        val maxTries = 100

        val width = binding.forestBoundsFrame.width
        val height = binding.forestBoundsFrame.height

        if (width == 0 || height == 0) {
            Log.w("FOREST_DEBUG", "⚠️ forestBoundsFrame hasn't been measured yet!")
            return null
        }

        repeat(maxTries) {
            val x = (margin..(width - margin)).random().toFloat()
            val y = (margin..(height - margin)).random().toFloat()

            val tooClose = existingPlants.any {
                val dx = it.posX - x
                val dy = it.posY - y
                Math.sqrt((dx * dx + dy * dy).toDouble()) < minDistance
            }

            if (!tooClose) return x to y
        }

        Log.w("FOREST_DEBUG", "⚠️ No unique position found for task")
        return null
    }


//    fun generateUniquePosition(
//        existingPlants: List<ForestPlant>,
//        screenWidth: Int,
//        screenHeight: Int,
//        minDistance: Int = 150,
//        maxTries: Int = 100
//    ): Pair<Float, Float>? {
//        val margin = 130 // safety margin based on plant size
//
//        repeat(maxTries) {
//            val x = (margin..(screenWidth - margin)).random().toFloat()
//            val y = (margin..(screenHeight - margin)).random().toFloat()
//
//            val tooClose = existingPlants.any {
//                val dx = it.posX - x
//                val dy = it.posY - y
//                Math.sqrt((dx * dx + dy * dy).toDouble()) < minDistance
//            }
//
//            if (!tooClose) return x to y
//        }
//
//        Log.w("FOREST_DEBUG", "⚠️ No unique position found for task")
//        return null
//    }


//    fun generateUniquePosition(
//        existingPlants: List<ForestPlant>,
//        screenWidth: Int,
//        screenHeight: Int,
//        minDistance: Int = 150,
//        maxTries: Int = 100
//    ): Pair<Float, Float>? {
//        repeat(maxTries) {
//            val x = (50..(screenWidth - 100)).random().toFloat()
//            val y = (100..(screenHeight - 200)).random().toFloat()
//
//            val tooClose = existingPlants.any {
//                val dx = it.posX - x
//                val dy = it.posY - y
//                Math.sqrt((dx * dx + dy * dy).toDouble()) < minDistance
//            }
//
//            if (!tooClose) return x to y
//        }
//        Log.w("FOREST_DEBUG", "⚠️ No unique position found for task:")
//
//        return null
//    }

    //dev only function to delete positions of plants
    private fun resetPlantPositions() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val forestRef = db.collection("users").document(userEmail).collection("forestPlants")

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        forestRef.get().addOnSuccessListener { snapshot ->
            val existing = snapshot.toObjects(ForestPlant::class.java).toMutableList()

            snapshot.documents.forEachIndexed { index, doc ->
                val newPos = generateUniquePosition(existing)
                if (newPos != null) {
                    doc.reference.update("posX", newPos.first, "posY", newPos.second)
                    Log.d("FOREST_DEBUG", "✅ Updated position for ${existing[index].taskName}")
                    existing[index].posX = newPos.first
                    existing[index].posY = newPos.second
                } else {
                    Log.w("FOREST_DEBUG", "⚠️ Could not find space for plant ${existing[index].taskName}")
                }
            }

            // Optional: immediately reload forest
            binding.forestBoundsFrame.removeAllViews()
            loadAndDisplayForest()
        }.addOnFailureListener {
            Log.e("FOREST_DEBUG", "Failed to fetch plants for reset", it)
        }
    }


}
