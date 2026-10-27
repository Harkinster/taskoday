package com.example.taskoday.core.util

/** ViewModels already start their initial load. A retained instance refreshes on subsequent resumes. */
internal class InitialResumeRefresh {
    private var resumed = false
    fun onResume(refresh: () -> Unit) {
        if (resumed) refresh() else resumed = true
    }
}
