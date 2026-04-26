package com.example.taskoday.features.auth

import androidx.lifecycle.ViewModel
import com.example.taskoday.data.remote.auth.SessionEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow

@HiltViewModel
class SessionEventsViewModel
    @Inject
    constructor(
        sessionEventBus: SessionEventBus,
    ) : ViewModel() {
        val unauthorizedEvents: SharedFlow<Unit> = sessionEventBus.unauthorizedEvents
    }
