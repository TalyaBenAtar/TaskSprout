package com.example.tasksprout

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasksprout.databinding.ActivityMainBinding
import com.example.tasksprout.model.AchievementManager
import com.example.tasksprout.model.TaskBoard
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.tasksprout.model.TaskBoardDataManager
import com.example.tasksprout.model.User
import com.example.tasksprout.model.UserDataManager
import com.example.tasksprout.ui.BoardFragment
import com.example.tasksprout.utilities.SignalManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        AchievementManager.createDefaultAchievementsInFirestore()
        TaskBoardDataManager.init(applicationContext)
        initCurrentUser(){
            AchievementManager.trackDailyUsageAndUpdateProgress()
        }
        initViews()

    }

    private fun initViews() {
        // Go to profile page
        binding.mainBTNProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.mainBTNAddBoard.setOnClickListener {
            showCreateBoardDialog()
        }

        // Show list of boards in fragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_FRAME_boardList, BoardFragment())
            .commit()
    }
    private fun initCurrentUser(onSuccess: (() -> Unit)? = null){
        FirebaseAuth.getInstance().currentUser?.email?.let { email ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(email)
                .get()
                .addOnSuccessListener { doc ->
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        UserDataManager.currentUser = user
                        Log.d("XP_DEBUG", "currentUser initialized in MainActivity: ${user.email}")
                        onSuccess?.invoke()
                    }
                }
        }
    }

    private fun getDatabaseReference(path: String): DatabaseReference {
        val database = Firebase.database
        return database.getReference(path)
    }

    private fun showCreateBoardDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_board, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val boardNameET = dialogView.findViewById<EditText>(R.id.dialog_ET_board_name)
        val boardDescET = dialogView.findViewById<EditText>(R.id.dialog_ET_board_description)
        val emailListLayout = dialogView.findViewById<LinearLayout>(R.id.dialog_LAY_email_list)
        val addEmailBtn = dialogView.findViewById<Button>(R.id.dialog_BTN_add_email)
        val createBoardBtn = dialogView.findViewById<Button>(R.id.dialog_BTN_create_board)

        setupAddEmailButton(addEmailBtn, emailListLayout)
        setupCreateBoardButton(createBoardBtn, boardNameET, boardDescET, emailListLayout, dialog)

        dialog.show()
    }

    private fun setupAddEmailButton(addEmailBtn: Button, emailListLayout: LinearLayout) {
        addEmailBtn.setOnClickListener {
            val emailET = EditText(this).apply {
                hint = "User email"
                setHintTextColor(ContextCompat.getColor(context, R.color.grey))
                setTextColor(ContextCompat.getColor(context, R.color.black))
            }
            emailListLayout.addView(emailET)
        }
    }

    private fun setupCreateBoardButton(
        createBoardBtn: Button,
        boardNameET: EditText,
        boardDescET: EditText,
        emailListLayout: LinearLayout,
        dialog: AlertDialog
    ) {
        createBoardBtn.setOnClickListener {
            val boardName = boardNameET.text.toString().trim()
            val description = boardDescET.text.toString().trim()
            val emails = mutableListOf<String>()

            for (i in 0 until emailListLayout.childCount) {
                val emailET = emailListLayout.getChildAt(i) as? EditText
                val email = emailET?.text?.toString()?.trim()
                if (!email.isNullOrEmpty()) emails.add(email)
            }

            if (boardName.isEmpty() || emails.isEmpty()) {
                SignalManager.getInstance().toast("Please fill in board name and at least one email")
                return@setOnClickListener
            }

            val date = java.time.LocalDate.now()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")
            val formattedDate = date.format(formatter)
            val currentEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: return@setOnClickListener
            val allUsers = mutableListOf<com.example.tasksprout.model.BoardUser>()

// Add creator as MANAGER with default name
            allUsers.add(
                com.example.tasksprout.model.BoardUser(
                    email = currentEmail,
                    name = "Anonymous",
                    role = com.example.tasksprout.model.Role.MANAGER
                )
            )

// Add the entered emails as MEMBERS
            emails.filter { it != currentEmail }.forEach { email ->
                allUsers.add(
                    com.example.tasksprout.model.BoardUser(
                        email = email,
                        name = "Anonymous",
                        role = com.example.tasksprout.model.Role.MEMBER
                    )
                )
            }

            val board = TaskBoard.Builder()
                .name(boardName)
                .users(allUsers)
                .tasks(emptyList())
                .description(description)
                .releaseDate(formattedDate)
                .build()

            TaskBoardDataManager.createBoardInFirestore(
                context = this,
                taskBoard = board,
                onSuccess = {
                    SignalManager.getInstance().toast("Board created successfully!")
                    dialog.dismiss()
                },
                onFailure = { exception ->
                    SignalManager.getInstance().toast("Failed to create board")
                }
            )
            dialog.dismiss()
        }

    }


    private fun getMessageFromDB(textView: android.widget.TextView) {
        val ref = getDatabaseReference("messages")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                textView.text = dataSnapshot.value?.toString() ?: ""
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("Data Error:", "Failed to read value.", error.toException())
            }
        }
        )
    }

}
