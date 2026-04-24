package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.TagDao
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.mapper.toEntity
import com.example.taskoday.domain.model.Tag
import com.example.taskoday.domain.repository.TagRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl
    @Inject
    constructor(
        private val tagDao: TagDao,
    ) : TagRepository {
        override fun observeTags(): Flow<List<Tag>> = tagDao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override suspend fun upsertTag(tag: Tag): Long = tagDao.upsert(tag.toEntity())
    }
