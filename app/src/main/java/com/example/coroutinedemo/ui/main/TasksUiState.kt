package com.example.coroutinedemo.ui.main

import com.example.coroutinedemo.data.Task

data class TasksUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val announcement: String = "",
    val courseCount: Int? = null,          // Latihan 1
    val errorMessage: String? = null,
    val tapCount: Int = 0,
    val loadTimeMs: Long? = null           // Pengembangan bebas: lama pemuatan terakhir
)
