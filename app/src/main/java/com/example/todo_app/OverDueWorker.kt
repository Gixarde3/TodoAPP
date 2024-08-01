package com.example.todo_app


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit


class OverDueWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()
        val auth = Firebase.auth
        // Verificar tareas vencidas
        checkOverdueTasks(db, auth)
        return Result.success()
    }

    private suspend fun checkOverdueTasks(db: FirebaseFirestore, auth: FirebaseAuth) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val overdueTasksSnapshot: QuerySnapshot = db.collection("tasks")
                .whereEqualTo("userId", user.uid)
                .whereEqualTo("completed", false)
                .whereLessThan("dueDate", Timestamp.now())
                .get()
                .await()

            println("Overdue tasks: ${overdueTasksSnapshot.isEmpty}")

            if (!overdueTasksSnapshot.isEmpty) {
                sendNotification("Tareas vencidas", "Tienes tareas que han vencido.")
            }
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "task_notification_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Task Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Asegúrate de tener un icono de notificación en tu drawable
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)

        notificationManager.notify(0, notificationBuilder.build())
    }
}