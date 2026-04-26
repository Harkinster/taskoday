package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.ParentChild

interface ChildrenRepository {
    suspend fun fetchChildren(): List<ParentChild>
}
