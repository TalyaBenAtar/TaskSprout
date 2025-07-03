package com.example.tasksprout

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tasksprout.databinding.ActivityForestBinding
import com.example.tasksprout.model.ForestPlant
import com.example.tasksprout.model.Task
import com.example.tasksprout.model.TaskBoard
import com.example.tasksprout.utilities.SignalManager
import com.example.tasksprout.utilities.SingleSoundPlayer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ForestActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityForestBinding
    private var activeBubble: View? = null


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

        generateForestIfNeeded()
    }


    private fun generateForestIfNeeded() {
        var ssp = SingleSoundPlayer(this)
        var plantStatusUpdated =0;
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val forestRef = db.collection("users").document(userEmail).collection("forestPlants")
        val boardRef = db.collection("boards")
        Log.d("FOREST_DEBUG", "Fetching forest for user: $userEmail")

        val existingPlants = mutableListOf<ForestPlant>()
        val validTaskKeys = mutableSetOf<String>()

        forestRef.get().addOnSuccessListener { forestSnapshot ->
            existingPlants.addAll(forestSnapshot.toObjects(ForestPlant::class.java))
            val initialSize = existingPlants.size
            val plantMap = existingPlants.associateBy { "${it.boardName}::${it.taskName}" }.toMutableMap()

            boardRef.get().addOnSuccessListener { boardSnapshots ->
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

                        val existing = plantMap[taskKey]

                        if (existing != null) {
                            if (existing.status != status) {
                                forestRef
                                    .whereEqualTo("boardName", boardName)
                                    .whereEqualTo("taskName", taskName)
                                    .get()
                                    .addOnSuccessListener { docs ->
                                        for (doc in docs) {
                                            doc.reference.update("status", status)
                                            Log.d("FOREST_DEBUG", "🌿 Updated plant status: $taskName in $boardName to $status")
                                            reloadForestUI()
                                            SignalManager.getInstance().toast("A plant's status was updated!🌿")
                                            plantStatusUpdated=1;
                                        }
                                    }
                                existing.status = status // update locally
                            } else {
                                Log.d("FOREST_DEBUG", "🌿 Already exists: $taskName on $boardName")
                            }
                        } else {
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
                }

                for (plant in existingPlants) {
                    val plantKey = "${plant.boardName}::${plant.taskName}"
                    if (plantKey !in validTaskKeys) {
                        forestRef
                            .whereEqualTo("boardName", plant.boardName)
                            .whereEqualTo("taskName", plant.taskName)
                            .get()
                            .addOnSuccessListener { docs ->
                                for (doc in docs) {
                                    doc.reference.delete()
                                    Log.d("FOREST_DEBUG", "🗑️ Deleted orphaned plant ${plant.taskName} from ${plant.boardName}")
                                    reloadForestUI()
                                    SignalManager.getInstance().toast("A plant was deleted🗑️")
                                }
                            }
                    }
                }
                forestRef.get().addOnSuccessListener { updatedSnapshot ->
                    val updatedSize = updatedSnapshot.size()

                    val newCount = updatedSize - initialSize
                    if (newCount > 0) {
                        ssp.playSound(R.raw.plant_grew)
                        if (newCount == 1)
                            SignalManager.getInstance().toast("🌱 A new plant sprouted!")
                        else
                            SignalManager.getInstance().toast("🌱 $newCount new plants sprouted in your forest!")
                    }
                    if (plantStatusUpdated!=0) ssp.playSound(R.raw.plant_grew)

                    loadAndDisplayForest()
                }
            }
        }
    }

    private fun reloadForestUI() {
        binding.forestBoundsFrame.removeAllViews()
        loadAndDisplayForest()
    }

    private fun loadAndDisplayForest() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val forestRef = db.collection("users").document(userEmail).collection("forestPlants")

        forestRef.get().addOnSuccessListener { snapshot ->
            val plants = snapshot.toObjects(ForestPlant::class.java)

            for (plant in plants) {
                addPlantToView(plant)
            }
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
            Log.d("FOREST_DEBUG", "Drawing plant: ${plant.taskName} at (${plant.posX}, ${plant.posY}) with status ${plant.status}")
        }catch (e: Exception) {
            Log.e("FOREST_DEBUG", "Failed to set image for plant status: ${plant.status}", e)
        }
        imageView.setOnClickListener {
            activeBubble?.let { binding.forestBoundsFrame.removeView(it) }

            // Create and show new bubble
            val bubble = createInfoBubble(this, plant)
            activeBubble = bubble

            // Position it above the plant
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            val bubbleWidth = 600 // match the maxWidth you set
            layoutParams.leftMargin = (plant.posX - bubbleWidth / 2).toInt().coerceAtLeast(0)
            layoutParams.topMargin = (plant.posY - 140).toInt() // Adjust to float above the plant
            bubble.layoutParams = layoutParams

            binding.forestBoundsFrame.addView(bubble)
        }
        binding.forestBoundsFrame.addView(imageView)
    }

    private fun createInfoBubble(context: Context, plant: ForestPlant): View {
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(0, 0, 0, 0)
        container.alpha = 0f // start transparent

        val bubble = TextView(context)
        bubble.setPadding(24, 16, 24, 16)
        bubble.setBackgroundResource(R.drawable.bubble_background)
        bubble.setTextColor(Color.BLACK)
        bubble.text = "🌱 ${plant.taskName}\n📋 ${plant.boardName}\n📌 ${plant.status}"

        bubble.maxWidth = 800
        bubble.minWidth = 500
        bubble.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 0)
        }

        val tail = ImageView(context)
        tail.setImageResource(R.drawable.bubble_tail)
        tail.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START
            setMargins(48, -6, 0, 0)
        }

        container.addView(bubble)
        container.addView(tail)

        container.setOnClickListener {
            binding.forestBoundsFrame.removeView(container)
            activeBubble = null
        }

        // Animate fade-in
        container.alpha = 0f
        container.translationY = 30f

        container.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        container.setOnClickListener {
            container.animate()
                .alpha(0f)
                .translationY(30f)
                .setDuration(300)
                .withEndAction {
                    binding.forestBoundsFrame.removeView(container)
                    activeBubble = null
                }
                .start()
        }
        return container
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


    //dev only function to delete positions of plants, can only be used when button is visible in app
    private fun resetPlantPositions() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val forestRef = db.collection("users").document(userEmail).collection("forestPlants")

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

            binding.forestBoundsFrame.removeAllViews()
            loadAndDisplayForest()
        }.addOnFailureListener {
            Log.e("FOREST_DEBUG", "Failed to fetch plants for reset", it)
        }
    }


}
