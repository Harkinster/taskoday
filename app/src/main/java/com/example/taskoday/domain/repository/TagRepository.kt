package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    suspend fun upsertTag(tag: Tag): Long
}
