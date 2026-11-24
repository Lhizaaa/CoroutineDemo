# Jawaban Latihan 9 – Coroutine Demo

## 1. Permintaan ketiga `getCourseCount()`
Diimplementasikan di `TaskRepository.getCourseCount()` (`delay(1500L)`), disimpan di
`TasksUiState.courseCount`, dan dimuat dengan `async` ketiga di `TasksViewModel.loadData()`.

**Apakah total waktu masih ±2 detik?** Ya. Ketiga `async` dijalankan bersamaan (konkuren),
sehingga total waktu sama dengan pekerjaan terlama, yaitu `max(2000, 1000, 1500) = 2000 ms`,
bukan jumlahnya (4,5 detik). Aplikasi menampilkan "Last load time" untuk membuktikannya.
Ketiganya tidak saling bergantung, sehingga aman dijalankan paralel.

## 2. Tombol Retry
Di `TasksScreen.kt`, tombol **Retry** ditampilkan dalam `Row` bersama pesan error dan hanya
muncul jika `state.errorMessage != null`. Tombol ini memanggil `loadData()` lagi.

## 3. `catch (e: Exception)` menggantikan `catch (e: IOException)`
Saat menekan **Load** lalu segera **Cancel**, pesan yang muncul bukan lagi "Loading cancelled",
melainkan sesuatu seperti **"Failed to load: StandaloneCoroutine was cancelled"**.

**Alasannya:** `job.cancel()` membuat suspend function yang sedang menunggu (`await()` / `delay()`)
melempar `CancellationException`. `CancellationException` adalah turunan `Exception`, sehingga ikut
tertangkap oleh `catch (e: Exception)`. Blok catch kemudian menimpa pesan "Loading cancelled" dengan
pesan error, padahal pembatalan bukan kegagalan. Dengan `IOException`, pembatalan tidak tertangkap
dan diteruskan dengan benar. Jika terpaksa menangkap `Exception`, lempar ulang
`CancellationException` terlebih dahulu (`catch (e: CancellationException) { throw e }`).

(Kode final tetap memakai `catch (e: IOException)` karena itu cara yang benar)

## 4. Di mana macetnya?
- **`Thread.sleep(2000L)` di dalam `withContext(Dispatchers.IO)`:** layar **tidak macet**. `Thread.sleep`
  memang memblokir thread, tetapi yang diblokir adalah thread IO, bukan main thread. Tombol *Tap me*
  tetap responsif.
- **`Thread.sleep(2000L)` tanpa `withContext`:** layar **macet** ±2 detik. `viewModelScope` berjalan di
  `Dispatchers.Main`, sehingga `Thread.sleep` memblokir main thread. *Tap me* tidak bereaksi dan frame
  tidak digambar (jank/freeze, berisiko ANR).

Kesimpulan: `withContext(Dispatchers.IO)` memindahkan pekerjaan ke thread lain, sedangkan `delay()`
tidak memblokir thread sama sekali. Kelas yang melakukan pekerjaan berat bertanggung jawab memindahkannya
ke dispatcher yang tepat (main-safe).

## 5. Pertanyaan konsep
**(a) Beda `launch` dan `async`**
`launch` menghasilkan `Job` dan tidak mengembalikan nilai, cocok untuk pekerjaan "jalankan saja"
(menyimpan data, memperbarui state). `async` menghasilkan `Deferred<T>`, yaitu janji nilai yang
diambil dengan `await()`, cocok untuk beberapa pekerjaan paralel yang hasilnya dibutuhkan.

**(b) Kenapa `GlobalScope` dihindari**
`GlobalScope` tidak terikat lifecycle apa pun dan hidup selama aplikasi berjalan. Coroutine di dalamnya
tidak berhenti walau layar/ViewModel sudah hilang, sehingga bisa bocor (memory/resource leak), terus
bekerja diam-diam, dan memperbarui UI yang sudah tidak ada. Selain itu tidak ada structured concurrency
(tidak ada parent yang menunggu atau membatalkan, dan error sulit ditangani). Gunakan `viewModelScope`
atau `lifecycleScope`.

**(c) Nasib `getAnnouncement()` jika `getTasks()` gagal**
Karena structured concurrency, kegagalan child (`getTasks`) membatalkan parent (`coroutineScope`) dan
semua sibling-nya. `getAnnouncement()` langsung **dibatalkan** dan tidak pernah selesai (hasilnya tidak
dipakai), lalu `IOException` diteruskan ke pemanggil dan ditangkap `try/catch` di ViewModel menjadi
`errorMessage`. (Begitu pula `getCourseCount()`.)

## 6. Tantangan: tombol Sort
Tombol **Sort** memanggil `TasksViewModel.sortTasks()`, yang memanggil
`TaskRepository.sortTasksByTitle()`. Pengurutan dijalankan dengan
`withContext(Dispatchers.Default)` (komputasi CPU), lalu hasilnya dipakai memperbarui state di Main.
Tombol hanya aktif saat tidak loading dan daftar berisi lebih dari satu tugas.
