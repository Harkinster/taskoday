package com.example.taskoday.data.repository

import com.example.taskoday.domain.repository.*
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanningCacheInvalidationTest {
    @Test fun `no active child invalidates all previous child projections`() = runBlocking {
        var tasksCleared = 0
        var questsCleared = 0
        val auth = object : AuthRepository by unused() {
            override fun getAccessToken() = "stored-access"
            override suspend fun getActiveChildId(forceRefresh: Boolean): Long? = null
        }
        val tasks = object : TaskRepository by unused() {
            override suspend fun clearRemoteRoutineCache() { tasksCleared++ }
            override suspend fun clearRemoteMissionCache() { tasksCleared++ }
        }
        val quests = object : QuestRepository by unused() {
            override suspend fun clearRemoteCache() { questsCleared++ }
        }
        assertFalse(RoutinesRepositoryImpl(auth, unused(), unused(), tasks).syncRoutinesForDay(0L).usedRemoteData)
        assertFalse(MissionsRepositoryImpl(auth, unused(), tasks).syncMissions().usedRemoteData)
        assertFalse(QuestsRepositoryImpl(auth, unused(), quests).syncQuests().usedRemoteData)
        assertEquals(2, tasksCleared)
        assertEquals(1, questsCleared)
    }

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        error("Unexpected API/cache call: ${method.name}")
    } as T
}
