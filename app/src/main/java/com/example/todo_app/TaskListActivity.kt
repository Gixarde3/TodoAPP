package com.example.todo_app

import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.type.Date
import java.util.Locale
import java.util.UUID

class TaskListActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var tasksAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        db = FirebaseFirestore.getInstance()
        auth = Firebase.auth

        tasksAdapter = TaskAdapter(onTaskClicked = { task ->
            // Manejar el clic en una tarea
        },
        onTaskCheckedChanged = { task, isChecked ->
            updateTaskCompletionStatus(task, isChecked)
        })
        findViewById<RecyclerView>(R.id.tasksRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@TaskListActivity)
            adapter = tasksAdapter
        }

        findViewById<FloatingActionButton>(R.id.addTaskFab).setOnClickListener {
            showAddTaskDialog()
        }

        loadTasks()
    }

    /*
    * Función encargada de cargar las tareas del usuario. Obtiene el id del usuario logeado y obtiene
    * todas las tareas creadas por el.
    * */
    private fun loadTasks() {
        db.collection("tasks")
            .whereEqualTo("userId", auth.currentUser?.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Manejar error
                    return@addSnapshotListener
                }

                val tasks = snapshot?.toObjects(Task::class.java) ?: listOf()
                tasksAdapter.updateTasks(tasks)
            }
    }

    private fun updateTaskCompletionStatus(task: Task, completed: Boolean) {
        db.collection("tasks").document(task.id)
            .update("completed", completed)
            .addOnSuccessListener {
                // Actualización exitosa
            }
            .addOnFailureListener { e ->
                // Manejar el error
            }
    }

    private fun showAddTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.taskTitleEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.taskDescriptionEditText)
        val dueDateEditText = dialogView.findViewById<EditText>(R.id.taskDueDateEditText)

        val calendar = Calendar.getInstance()

        dueDateEditText.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    dueDateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar nueva tarea")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                val title = titleEditText.text.toString()
                val description = descriptionEditText.text.toString()
                val dueDate = Timestamp(java.util.Date(calendar.timeInMillis))

                if (title.isNotEmpty()) {
                    addTask(title, description, dueDate)
                } else {
                    Toast.makeText(this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun addTask(title: String, description: String, dueDate: Timestamp) {
        val userId = auth.currentUser?.uid ?: return

        val task = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            dueDate = dueDate,
            completed = false,
            userId = userId,
            createdAt = Timestamp.now(),
        )

        db.collection("tasks")
            .document(task.id)
            .set(task)
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea agregada con éxito", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar la tarea: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}