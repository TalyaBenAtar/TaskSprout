package com.example.tasksprout

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasksprout.databinding.ActivityTaskBoardBinding
import com.example.tasksprout.interfaces.Callback_TaskClicked
import com.example.tasksprout.model.Task
import com.example.tasksprout.model.TaskBoard
import com.example.tasksprout.model.TaskDataManager
import com.example.tasksprout.ui.TaskUpperMenuFragment
import com.example.tasksprout.ui.TaskFragment
import com.example.tasksprout.utilities.SignalManager
import kotlin.jvm.java
import com.google.firebase.auth.FirebaseAuth

class TaskBoardActivity : AppCompatActivity() {

    private lateinit var main_FRAME_task: FrameLayout
    private lateinit var main_FRAME_task_board_menu: FrameLayout
    private lateinit var taskUpperMenuFragmant: TaskUpperMenuFragment
    private lateinit var taskFragment: TaskFragment
    private lateinit var main_FRAME_bottom: FrameLayout
    private lateinit var board: TaskBoard
    private lateinit var binding: ActivityTaskBoardBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TaskDataManager.init(applicationContext)
        binding = ActivityTaskBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        setContentView(R.layout.activity_task_board)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val boardFromIntent = intent.getSerializableExtra("board") as? TaskBoard
        if (boardFromIntent == null) {
            SignalManager.getInstance().toast("Board not found")
            finish()
            return
        }
        board = boardFromIntent

        findViews()
        initViews()
    }

    private fun findViews() {
        main_FRAME_task_board_menu = findViewById(R.id.main_FRAME_task_board_menu)
        main_FRAME_task = findViewById(R.id.main_FRAME_tasks)
        main_FRAME_bottom = findViewById(R.id.main_FRAME_bottom)
    }

    private fun initViews() {
        val footerView = layoutInflater.inflate(R.layout.fragment_taskboard_bottom, null)
        main_FRAME_bottom.addView(footerView)

        footerView.findViewById<Button>(R.id.footer_BTN_back).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        taskUpperMenuFragmant = TaskUpperMenuFragment()
        val bundle = Bundle()
        bundle.putSerializable("board", board)
        taskUpperMenuFragmant.arguments = bundle
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_FRAME_task_board_menu, taskUpperMenuFragmant)
            .commit()


        taskFragment = TaskFragment().apply {
            arguments = Bundle().apply {
                putSerializable("board_users", ArrayList(board.users)) // 🔧 pass users
                putString("current_user_email", FirebaseAuth.getInstance().currentUser?.email) // 🔧 pass email
            }
        }

        taskFragment.TaskItemClicked =
            object : Callback_TaskClicked {
                override fun onClick(score: Task) {

                }
            }
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_FRAME_tasks, taskFragment)
            .commit()
    }
}
