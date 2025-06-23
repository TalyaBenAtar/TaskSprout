package com.example.tasksprout.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasksprout.R
import com.example.tasksprout.TaskBoardActivity
import com.example.tasksprout.adapters.BoardAdapter
import com.example.tasksprout.interfaces.Callback_BoardClicked
import com.example.tasksprout.model.TaskBoard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.tasksprout.model.BoardUser
import com.example.tasksprout.model.Role
import com.example.tasksprout.model.TaskBoardDataManager

class BoardFragment : Fragment() {

    private lateinit var boardAdapter: BoardAdapter
    private val boardList = mutableListOf<TaskBoard>()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        val view = inflater.inflate(R.layout.fragment_boards, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.boards_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        boardAdapter = BoardAdapter(boardList)
        boardAdapter.boardCallback = object : Callback_BoardClicked {
            override fun onBoardClicked(board: TaskBoard) {
                TaskBoardDataManager.markBoardAsOpened(board.name)

                openBoard(board)
            }
            override fun onBoardLongClicked(board: TaskBoard) {
                showBoardOptionsPopup(board)
            }
        }
        recyclerView.adapter = boardAdapter
        listenToBoardChanges()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }

    override fun onResume() {
        super.onResume()
        boardAdapter.notifyDataSetChanged()
    }


    private fun openBoard(board: TaskBoard) {
        val intent = Intent(requireContext(), TaskBoardActivity::class.java)
        intent.putExtra("board", board)
        startActivity(intent)
    }


    private fun listenToBoardChanges() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        listenerRegistration = FirebaseFirestore.getInstance()
            .collection("boards")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BoardFragment", "Failed to listen for board updates: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val boards = snapshot.documents.mapNotNull { doc ->
                        try {
                            val board = doc.toObject(TaskBoard::class.java)
                            if (board != null && board.releaseDate.isBlank()) {
                                val updatedBoard = board.copy(releaseDate = "17-Jun-2025")
                                doc.reference.set(updatedBoard)
                                updatedBoard
                            } else {
                                board
                            }
                        } catch (e: Exception) {
                            Log.e("BoardFragment", "Skipping malformed board '${doc.id}': ${e.message}")
                            null
                        }
                    }.filter { board ->
                        board.users.any { it.email == userEmail }
                    }

                    boardList.clear()
                    boardList.addAll(boards)
                    boardAdapter.notifyDataSetChanged()
                }
            }
    }


    private fun showBoardOptionsPopup(board: TaskBoard) {
        val view =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_board_options, null)
        val popup = AlertDialog.Builder(requireContext()).setView(view).create()

        val editBtn = view.findViewById<Button>(R.id.dialog_BTN_edit_board)
        val deleteBtn = view.findViewById<Button>(R.id.dialog_BTN_delete_board)
        val removeUserBtn = view.findViewById<Button>(R.id.dialog_BTN_remove_user)

        editBtn.setOnClickListener {
            popup.dismiss()
            showEditBoardDialog(board)
        }

        deleteBtn.setOnClickListener {
            popup.dismiss()
            deleteBoardFromFirestore(board)
        }

        removeUserBtn.setOnClickListener {
            popup.dismiss()
            showRemoveUserDialog(board)
        }

        popup.show()
    }


    private fun showEditBoardDialog(board: TaskBoard) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_board, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()

        val nameET = view.findViewById<EditText>(R.id.dialog_ET_board_name)
        val descET = view.findViewById<EditText>(R.id.dialog_ET_board_description)
        val saveBtn = view.findViewById<Button>(R.id.dialog_BTN_create_board)
        val emailListLayout = view.findViewById<LinearLayout>(R.id.dialog_LAY_email_list)
        val addEmailBtn = view.findViewById<Button>(R.id.dialog_BTN_add_email)

        // Pre-fill name and description
        nameET.setText(board.name)
        descET.setText(board.description)
        saveBtn.text = "Save Changes"

        // Add email field dynamically
        addEmailBtn.setOnClickListener {
            val emailET = EditText(requireContext()).apply {
                hint = "User Email"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                setHintTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
            }
            emailListLayout.addView(emailET)
        }

        saveBtn.setOnClickListener {
            val newName = nameET.text.toString().trim()
            val newDesc = descET.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Board name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Collect existing emails to avoid duplication
            val existingEmails = board.users.map { it.email }

            // Gather new valid BoardUser objects
            val newUsers = mutableListOf<BoardUser>()
            for (i in 0 until emailListLayout.childCount) {
                val field = emailListLayout.getChildAt(i)
                if (field is EditText) {
                    val email = field.text.toString().trim()
                    if (
                        email.isNotEmpty() &&
                        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                        !existingEmails.contains(email)
                    ) {
                        newUsers.add(BoardUser(email = email, name = "Anonymous", role = Role.MEMBER))
                    }
                }
            }

            val updatedBoard = board.copy(
                name = newName,
                description = newDesc,
                users = board.users + newUsers,
                releaseDate = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
            )

            FirebaseFirestore.getInstance()
                .collection("boards")
                .whereEqualTo("name", board.name)
                .get()
                .addOnSuccessListener { snapshot ->
                    val docId = snapshot.documents.firstOrNull()?.id ?: return@addOnSuccessListener
                    FirebaseFirestore.getInstance()
                        .collection("boards")
                        .document(docId)
                        .set(updatedBoard)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Board updated", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                }
        }

        dialog.show()
    }


    private fun deleteBoardFromFirestore(board: TaskBoard) {
        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", board.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val docId = snapshot.documents.firstOrNull()?.id ?: return@addOnSuccessListener
                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(docId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Board deleted", Toast.LENGTH_SHORT).show()
                    }
            }
    }


    private fun showRemoveUserDialog(board: TaskBoard) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_remove_user_from_board, null)
        val userListContainer = view.findViewById<LinearLayout>(R.id.user_list_container)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        board.users.forEach { user ->
            val button = Button(requireContext()).apply {
                text = "Remove ${user.email}"
                setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.red_dark))
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                setOnClickListener {
                    dialog.dismiss()
                    removeUserFromBoard(board, user.email)
                }
            }
            userListContainer.addView(button)
        }

        dialog.show()
    }


    private fun removeUserFromBoard(board: TaskBoard, email: String) {
        val updatedUsers = board.users.filter { it.email != email }
        val updatedBoard = board.copy(users = updatedUsers)

        FirebaseFirestore.getInstance()
            .collection("boards")
            .whereEqualTo("name", board.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val docId = snapshot.documents.firstOrNull()?.id ?: return@addOnSuccessListener
                FirebaseFirestore.getInstance()
                    .collection("boards")
                    .document(docId)
                    .set(updatedBoard)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Removed $email from board", Toast.LENGTH_SHORT).show()
                    }
            }
    }


}
