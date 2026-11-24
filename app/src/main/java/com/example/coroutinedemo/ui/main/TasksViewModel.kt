package com.example.coroutinedemo.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coroutinedemo.data.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.system.measureTimeMillis

class TasksViewModel(
    private val repository: TaskRepository = TaskRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadData(shouldFail: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val elapsed = measureTimeMillis {
                    coroutineScope {
                        // Tiga permintaan yang tidak saling bergantung dijalankan bersamaan
                        val tasks = async { repository.getTasks(shouldFail) }
                        val announcement = async { repository.getAnnouncement() }
                        val courseCount = async { repository.getCourseCount() } // Latihan 1

                        val loadedTasks = tasks.await()
                        val loadedAnnouncement = announcement.await()
                        val loadedCourseCount = courseCount.await()

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                tasks = loadedTasks,
                                announcement = loadedAnnouncement,
                                courseCount = loadedCourseCount
                            )
                        }
                    }
                }
                _uiState.update { it.copy(loadTimeMs = elapsed) }
            } catch (e: IOException) {
                // Latihan 3: jika diganti catch (e: Exception), CancellationException
                // ikut tertangkap dan pesan "Loading cancelled" tertimpa pesan error.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load: ${e.message}"
                    )
                }
            }
        }
    }

    fun cancelLoading() {
        loadJob?.cancel()
        _uiState.update {
            it.copy(isLoading = false, errorMessage = "Loading cancelled")
        }
    }

    fun onTap() {
        _uiState.update { it.copy(tapCount = it.tapCount + 1) }
    }

    // Contoh buruk yang disengaja: memblokir main thread
    fun blockMainThread() {
        Thread.sleep(3000L)
    }

    // Latihan 6: urutkan di Dispatchers.Default (via repository), lalu perbarui state di Main
    fun sortTasks() {
        val current = _uiState.value.tasks
        if (current.size < 2) return
        viewModelScope.launch {
            val sorted = repository.sortTasksByTitle(current)
            _uiState.update { it.copy(tasks = sorted) }
        }
    }
}
