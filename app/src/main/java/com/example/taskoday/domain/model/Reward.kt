package com.example.taskoday.domain.model

data class Reward(
    val id: Long = 0L,
    val title: String,
    val description: String? = null,
    val cost: Int,
    val emoji: String = "\uD83C\uDF81",
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)
