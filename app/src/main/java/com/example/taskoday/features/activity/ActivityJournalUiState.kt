package com.example.taskoday.features.activity

enum class ActivityJournalKind {
    ACTION,
    WISH_PENDING,
    WISH_APPROVED,
    WISH_REFUSED,
    WISH_USED,
}

data class ActivityJournalItem(
    val id: String,
    val sortKey: String,
    val dateLabel: String?,
    val typeLabel: String,
    val title: String,
    val detail: String,
    val kind: ActivityJournalKind,
)

data class ActivityJournalUiState(
    val isLoading: Boolean = true,
    val isParent: Boolean = false,
    val childLabel: String? = null,
    val events: List<ActivityJournalItem> = emptyList(),
    val errorMessage: String? = null,
)
