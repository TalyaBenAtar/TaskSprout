package com.example.tasksprout

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.example.tasksprout.adapters.LeaderboardAdapter
import com.example.tasksprout.databinding.ActivityBoardSettingsBinding
import com.example.tasksprout.model.BoardUser
import com.example.tasksprout.model.Task
import com.example.tasksprout.model.TaskBoard
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.tasksprout.model.Role
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasksprout.utilities.SignalManager
import com.github.mikephil.charting.components.Legend


class BoardSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBoardSettingsBinding
    private var currentBoard: TaskBoard? = null
    private var currentUserEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.boardSettingBTNBack.setOnClickListener {
            val intent = Intent(this, TaskBoardActivity::class.java)
            intent.putExtra("board", currentBoard)
            startActivity(intent)
        }

        binding.settingsRVLeaderboard.layoutManager = LinearLayoutManager(this)

        currentBoard = intent.getSerializableExtra("board") as? TaskBoard
        currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        if (currentBoard == null || currentUserEmail == null) {
            SignalManager.getInstance().toast("Board or user info missing")
            finish()
            return
        }

        setupXPSettings()
        setupLeaderboard()
        setupCharts()
    }


    private fun setupXPSettings() {
        val isManager = currentBoard?.users?.find { it.email == currentUserEmail }?.role == Role.MANAGER
        binding.boardSettingsXPSection.visibility = if (isManager) View.VISIBLE else View.GONE

        if (isManager) {
            binding.editTextClaim.setText(currentBoard?.xpClaim?.toString())
            binding.editTextTodoToInProgress.setText(currentBoard?.xpTodoToInProgress?.toString())
            binding.editTextToDone.setText(currentBoard?.xpToDone?.toString())
            binding.editTextToNeglected.setText(currentBoard?.xpToNeglected?.toString())
            binding.editTextNeglectedRecovered.setText(currentBoard?.xpNeglectedRecovered?.toString())

            binding.btnSaveXPSettings.setOnClickListener {
                val updatedBoard = currentBoard?.copy(
                    xpClaim = binding.editTextClaim.text.toString().toIntOrNull() ?: 5,
                    xpTodoToInProgress = binding.editTextTodoToInProgress.text.toString().toIntOrNull() ?: 10,
                    xpToDone = binding.editTextToDone.text.toString().toIntOrNull() ?: 50,
                    xpToNeglected = binding.editTextToNeglected.text.toString().toIntOrNull() ?: -30,
                    xpNeglectedRecovered = binding.editTextNeglectedRecovered.text.toString().toIntOrNull() ?: 15
                )

                updatedBoard?.let { board ->
                    FirebaseFirestore.getInstance()
                        .collection("boards")
                        .whereEqualTo("name", board.name)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val doc = snapshot.documents.firstOrNull() ?: return@addOnSuccessListener
                            FirebaseFirestore.getInstance()
                                .collection("boards")
                                .document(doc.id)
                                .set(board)
                                .addOnSuccessListener {
                                    SignalManager.getInstance().toast("XP settings saved")
                                    currentBoard = board
                                }
                        }
                }
            }
        }
    }

    private fun setupLeaderboard() {
        val leaderboard = currentBoard?.users?.sortedByDescending { it.xp } ?: return
        val adapter = LeaderboardAdapter(leaderboard)
        binding.settingsRVLeaderboard.adapter = adapter
    }

    private fun setupCharts() {
        val tasks = currentBoard?.tasks ?: return
        val users = currentBoard?.users ?: return

        users.forEach { user ->
            val claimed = tasks.count { it.assignedTo == user.email }
            val todos = tasks.count { it.status == Task.Status.TODO && it.assignedTo == user.email }
            val inProgress = tasks.count { it.status == Task.Status.IN_PROGRESS && it.assignedTo == user.email }
            val done = tasks.count { it.status == Task.Status.DONE && it.assignedTo == user.email }
            val neglected = tasks.count { it.status == Task.Status.NEGLECTED && it.assignedTo == user.email }

            addToPie(binding.boardSettingsChartClaimed, "Claimed", user.name, claimed)
            addToPie(binding.boardSettingsChartTodo, "To Do", user.name, todos)
            addToPie(binding.boardSettingsChartInProgress, "In Progress", user.name, inProgress)
            addToPie(binding.boardSettingsChartDone, "Done", user.name, done)
            addToPie(binding.boardSettingsChartNeglected, "Neglected", user.name, neglected)
        }
    }

    private fun addToPie(pieChart: PieChart, label: String, name: String, count: Int) {
        val entry = PieEntry(count.toFloat(), name)
        val existingData = pieChart.data

        val entries = if (existingData != null && existingData.dataSet != null) {
            existingData.dataSet as PieDataSet
        } else {
            PieDataSet(mutableListOf(), label)
        }
        entries.addEntry(entry)
        val customColors = listOf(
            ContextCompat.getColor(this, R.color.soft_pink),
            ContextCompat.getColor(this, R.color.soft_blue),
            ContextCompat.getColor(this, R.color.soft_green),
            ContextCompat.getColor(this, R.color.soft_gold),
            ContextCompat.getColor(this, R.color.soft_salmon)
        )
        entries.colors = customColors
        entries.valueTextColor = Color.BLACK
//        entries.value

//        entries.colors = ColorTemplate.MATERIAL_COLORS.toList()

        val data = PieData(entries)
        pieChart.data = data

        pieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        pieChart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
        pieChart.legend.setDrawInside(false)
//        pieChart.setExtraTopOffset(20f)
        pieChart.legend.textColor = Color.BLACK // optional for better visibility
        pieChart.legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            textColor = Color.BLACK
            textSize = 12f
            form = Legend.LegendForm.SQUARE
            formSize = 10f
            xEntrySpace = 10f
            yEntrySpace = 8f
        }

        pieChart.invalidate()
    }

}
