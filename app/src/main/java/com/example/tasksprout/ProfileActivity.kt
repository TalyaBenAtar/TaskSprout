package com.example.tasksprout

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tasksprout.databinding.ActivityProfileBinding
import com.example.tasksprout.model.UserDataManager
import com.example.tasksprout.utilities.ImageLoader
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.storage
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.tasksprout.utilities.SignalManager


class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var selectedImageUri: Uri? = null
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            SignalManager.getInstance().toast("Image selected")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        ImageLoader.init(this)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
    }

    private fun initViews() {
        Log.d("ProfileActivity", "User email: ${FirebaseAuth.getInstance().currentUser?.email}")
        UserDataManager.init(this) { success ->
            if (success) {
                val user = UserDataManager.currentUser!!
                binding.profileLBLName.text = user.name
                binding.profileLBLEmail.text = user.email
                binding.profileLBLXp.text = "${user.xp} XP"
                binding.profileLBLTaskCount.text = "You finished ${user.tasksDone} tasks!"
                binding.profileLBLTaskStatus.text = "${user.tasksTodo} To Do's\n${user.tasksInProgress} In Progress\n${user.tasksDone} Done\n${user.tasksNeglected} Neglected"
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    UserDataManager.init(this) { retrySuccess ->
                        if (retrySuccess) {
                            val user = UserDataManager.currentUser!!
                            // ...
                        } else {
                            SignalManager.getInstance().toast("Failed to load profile")
                        }
                    }
                }, 1000) // Retry after 1 second
            }
        }

        // Sign Out
        binding.profileBTNSignOut.setOnClickListener {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
        }

        // Back to MainActivity
        binding.profileBTNBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.profileBTNEditName.setOnClickListener {
            showEditNameAndPictureDialog()
        }
        uplaodPictureFromStorage()
        updateTaskCounters()

    }

    private fun showEditNameAndPictureDialog(){
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val nameET = dialogView.findViewById<EditText>(R.id.edit_profile_ET_name)
        val selectBtn = dialogView.findViewById<Button>(R.id.edit_profile_BTN_select)
        val uploadBtn = dialogView.findViewById<Button>(R.id.edit_profile_BTN_upload)

        nameET.setText(binding.profileLBLName.text.toString())

        selectBtn.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        uploadBtn.setOnClickListener {
            val newName = nameET.text.toString().trim()
            if (newName.isNotEmpty()) {
                UserDataManager.updateUserName(newName) {
                    if (it) binding.profileLBLName.text = newName
                }
            }

            if (selectedImageUri != null) {
                val fileName = "banner_${FirebaseAuth.getInstance().currentUser?.uid}.jpg"
                val ref = Firebase.storage.reference.child("banners/$fileName")

                ref.putFile(selectedImageUri!!)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        ref.downloadUrl
                    }.addOnSuccessListener { downloadUrl ->
                        ImageLoader.getInstance().loadImage(downloadUrl.toString(), binding.profileViewBanner)
                        selectedImageUri = null
                        SignalManager.getInstance().toast("Image uploaded")
                        dialog.dismiss()
                    }.addOnFailureListener {
                        SignalManager.getInstance().toast("Image upload failed")
                    }
            } else {
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun uplaodPictureFromStorage(){
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val bannerRef = Firebase.storage.reference.child("banners/banner_${uid}.jpg")
        bannerRef.downloadUrl
            .addOnSuccessListener { downloadUrl ->
                ImageLoader.getInstance().loadImage(downloadUrl.toString(), binding.profileViewBanner)
            }
            .addOnFailureListener {
                // Optional: show placeholder or ignore silently
            }

    }


    private fun updateTaskCounters(){
        UserDataManager.refreshTaskStatusCountsForUser {
            if (it) {
                val user = UserDataManager.currentUser
                @Suppress("SetTextI18n")
                binding.profileLBLTaskStatus.text =
                    "${user?.tasksTodo} To Do's\n" +
                            "${user?.tasksInProgress} In Progress\n" +
                            "${user?.tasksDone} Done\n" +
                            "${user?.tasksNeglected} Neglected"
            }
        }
    }

}
