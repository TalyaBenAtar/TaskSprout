package com.example.tasksprout

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasksprout.model.Achievement
import com.example.tasksprout.adapters.AchievementAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AchievementsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AchievementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish() // Close this activity and return to profile
        }

        recyclerView = findViewById(R.id.recycler_achievements)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userEmail).get().addOnSuccessListener { userDoc ->
            val unlocked = userDoc["unlockedAchievements"] as? List<String> ?: emptyList()
            val progressMap = userDoc["achievementProgress"] as? Map<String, Long> ?: emptyMap()

            db.collection("achievements").get().addOnSuccessListener { achievementDocs ->
                val achievements = achievementDocs.mapNotNull { it.toObject(Achievement::class.java) }
                    .sortedBy { it.title }

                adapter = AchievementAdapter(this, achievements, unlocked, progressMap)
                recyclerView.adapter = adapter
            }
        }

        val resetBtn = findViewById<Button>(R.id.btn_reset_achievements)
        resetBtn.setOnClickListener {
            val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return@setOnClickListener
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(userEmail)

            val resetMap = mapOf(
                "unlockedAchievements" to emptyList<String>(),
                "achievementProgress" to emptyMap<String, Any>()
            )

            userRef.update(resetMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Achievements reset successfully 🎯", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to reset achievements 😢", Toast.LENGTH_SHORT).show()
                }
        }

    }
}
