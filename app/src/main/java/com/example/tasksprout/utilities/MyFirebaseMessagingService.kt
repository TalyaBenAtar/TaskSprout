//package com.example.tasksprout.utilities
//
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.content.pm.PackageManager
//import android.os.Build
//import androidx.core.app.ActivityCompat
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import com.example.tasksprout.R
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import android.util.Log
//import com.example.tasksprout.model.Task
//import com.example.tasksprout.model.TaskBoard
//import okhttp3.*
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//import java.io.IOException
//
//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//
//        val notification = remoteMessage.notification
//        if (notification != null) {
//            val title = notification.title ?: "📢 TaskSprout"
//            val body = notification.body ?: "You have a new update!"
//
//            createNotificationChannel()
//            sendNotification(title, body)
//        } else {
//            Log.w("FCM", "Notification payload was null.")
//        }
//    }
//
//    override fun onNewToken(token: String) {
//        super.onNewToken(token)
//
//        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
//        val userRef = FirebaseFirestore.getInstance().collection("users").document(userEmail)
//
//        userRef.update("fcmToken", token)
//            .addOnSuccessListener {
//                Log.d("FCM", "Token updated successfully")
//            }
//            .addOnFailureListener { e ->
//                Log.e("FCM", "Error updating token", e)
//            }
//    }
//
//    private fun sendNotification(title: String, message: String) {
//        val builder = NotificationCompat.Builder(this, "default_channel")
//            .setSmallIcon(R.drawable.plant_done) // feel free to change icon
//            .setContentTitle(title)
//            .setContentText(message)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setAutoCancel(true)
//
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
//            == PackageManager.PERMISSION_GRANTED) {
//            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
//        } else {
//            Log.w("FCM", "Notification permission not granted.")
//        }
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                "default_channel",
//                "TaskSprout Notifications",
//                NotificationManager.IMPORTANCE_HIGH
//            ).apply {
//                description = "Channel for TaskSprout task and board updates"
//            }
//
//            val notificationManager = getSystemService(NotificationManager::class.java)
//            notificationManager?.createNotificationChannel(channel)
//        }
//    }
//
//    companion object {
//
//        fun notifyUsersOfNewBoard(board: TaskBoard) {
//            val currentEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
//            val db = FirebaseFirestore.getInstance()
//
//            for (user in board.users) {
//                val email = user.email
//                if (email == currentEmail) continue // skip the creator
//
//                db.collection("users").document(email).get()
//                    .addOnSuccessListener { userDoc ->
//                        val token = userDoc.getString("fcmToken")
//                        if (!token.isNullOrEmpty()) {
//                            sendPushNotification(
//                                token,
//                                "🌱 You were added to a board!",
//                                "You've been added to '${board.name}'. Check it out!"
//                            )
//                        }
//                    }
//            }
//        }
//
//        fun notifyUsersOfNewTask(board: TaskBoard, task: Task, creatorEmail: String) {
//            val db = FirebaseFirestore.getInstance()
//
//            for (user in board.users) {
//                val email = user.email
//                if (email == creatorEmail) continue // skip the task creator
//
//                db.collection("users").document(email).get()
//                    .addOnSuccessListener { userDoc ->
//                        val token = userDoc.getString("fcmToken")
//                        if (!token.isNullOrEmpty()) {
//                            sendPushNotification(
//                                token,
//                                "📋 New Task Added!",
//                                "${task.name} was added to the board '${board.name}'"
//                            )
//                        }
//                    }
//            }
//        }
//
//        private fun sendPushNotification(token: String, title: String, body: String) {
//            val json = org.json.JSONObject().apply {
//                put("to", token)
//                put("notification", org.json.JSONObject().apply {
//                    put("title", title)
//                    put("body", body)
//                })
//            }
//
//            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
//            val request = okhttp3.Request.Builder()
//                .url("https://fcm.googleapis.com/fcm/send")
//                .addHeader("Authorization", "key=YOUR_SERVER_KEY") // 🔥 Replace this!
//                .addHeader("Content-Type", "application/json")
//                .post(requestBody)
//                .build()
//
//            okhttp3.OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
//                override fun onFailure(call: okhttp3.Call, e: IOException) {
//                    Log.e("FCM", "Push failed: ${e.message}")
//                }
//
//                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
//                    Log.d("FCM", "Push sent: ${response.body?.string()}")
//                }
//            })
//        }
//    }
//
//}
