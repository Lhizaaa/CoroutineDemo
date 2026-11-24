package com.example.coroutinedemo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository berperan sebagai "server tiruan".
 * Semua fungsinya adalah suspend function yang memindahkan pekerjaannya
 * sendiri ke dispatcher yang tepat, sehingga main-safe.
 */
class TaskRepository {

    suspend fun getTasks(shouldFail: Boolean = false): List<Task> =
        withContext(Dispatchers.IO) {
            // Latihan 4: coba ganti delay(2000L) dengan Thread.sleep(2000L),
            // lalu coba juga hapus withContext, dan amati perbedaannya.
            delay(2000L)
            if (shouldFail) {
                throw IOException("Server is not reachable")
            }
            listOf(
                Task(1, "Write lab report", "Mobile App Development"),
                Task(2, "Read chapter 5", "Data Science"),
                Task(3, "Prepare quiz", "Research Methodology")
            )
        }

    suspend fun getAnnouncement(): String =
        withContext(Dispatchers.IO) {
            delay(1000L)
            "Quiz 2 is next Monday"
        }

    // Latihan 1: permintaan ketiga (1,5 detik)
    suspend fun getCourseCount(): Int =
        withContext(Dispatchers.IO) {
            delay(1500L)
            3
        }

    // Latihan 6: pengurutan adalah komputasi (CPU), maka memakai Dispatchers.Default
    suspend fun sortTasksByTitle(tasks: List<Task>): List<Task> =
        withContext(Dispatchers.Default) {
            tasks.sortedBy { it.title.lowercase() }
        }
}
