package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.domain.model.ParentChild
import com.example.taskoday.domain.repository.ChildrenRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildrenRepositoryImpl
    @Inject
    constructor(
        private val childrenApi: ChildrenApi,
    ) : ChildrenRepository {
        override suspend fun fetchChildren(): List<ParentChild> =
            childrenApi.getChildren().map { child ->
                ParentChild(
                    id = child.id,
                    displayName = child.displayName,
                    email = child.email,
                )
            }
    }
