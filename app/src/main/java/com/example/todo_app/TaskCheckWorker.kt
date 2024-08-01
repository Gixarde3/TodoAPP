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

/*
* Worker que verifica cada cierto tiempo las condiciones, y en caso de que se cumplan, se envía una notificación
* */
class TaskCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()
        val auth = Firebase.auth

        // Verificar tareas vencidas
        checkOverdueTasks(db, auth)

        // Verificar si no se han agregado tareas en un día
        checkNoNewTasks(db, auth)

        // Verificar si hay tareas vencidas hoy
        nearOverDueTasks(db, auth)

        return Result.success()
    }


    /*
    * Verifica si hay tareas vencidas, y si es así, envía una notificación
    * */
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

    /*
    * Verifica si no se han agregado tareas en un día, y si es así, envía una notificación
    * */
    private suspend fun checkNoNewTasks(db: FirebaseFirestore, auth: FirebaseAuth) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Calcular la fecha de hace 24 horas
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = Timestamp(calendar.time)

            val newTasksSnapshot: QuerySnapshot = db.collection("tasks")
                .whereEqualTo("userId", user.uid)
                .whereGreaterThan("createdAt", yesterday)
                .get()
                .await()

            println("New tasks: ${newTasksSnapshot.isEmpty}")

            if (newTasksSnapshot.isEmpty) {
                sendNotification("Sin nuevas tareas", "No has agregado nuevas tareas en el último día.")
            }
        }
    }

    /*
    * Verifica si hay tareas que se vencen hoy, y si es así, envía una notificación
    * */
    private suspend fun nearOverDueTasks (db: FirebaseFirestore, auth: FirebaseAuth) {
        val currentUser = auth.currentUser

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = Timestamp(calendar.time)

        // Obtener el final del día
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = Timestamp(calendar.time)
        currentUser?.let { user ->
            //Checar si hay tareas que se vencen hoy
            val overdueTasksSnapshot: QuerySnapshot = db.collection("tasks")
                .whereEqualTo("userId", user.uid)
                .whereEqualTo("completed", false)
                .whereGreaterThanOrEqualTo("dueDate", startOfDay)
                .whereLessThan("dueDate", endOfDay)
                .get()
                .await()

            println("Todays tasks: ${overdueTasksSnapshot.isEmpty}")

            if (!overdueTasksSnapshot.isEmpty) {
                sendNotification("Tareas a punto de vencer", "Tienes tareas que vencen el día de hoy.")
            }
        }
    }

    /*
    * Envía una notificación usando el servicio de notificaciones de Android
    * */
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
