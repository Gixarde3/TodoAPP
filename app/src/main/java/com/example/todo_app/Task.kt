package com.example.todo_app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Task(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: Timestamp = Timestamp.now(),
    val completed: Boolean = false,
    val userId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
)