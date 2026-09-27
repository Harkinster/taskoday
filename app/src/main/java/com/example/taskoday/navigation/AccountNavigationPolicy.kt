package com.example.taskoday.navigation

internal fun visibleAccountDestinations(isChild: Boolean, localChildMode: Boolean = false): List<TaskodayDestination> =
    TopLevelDestinations.filterNot {
        ((isChild || localChildMode) && it == TaskodayDestination.FollowUp) ||
            (localChildMode && it == TaskodayDestination.FamilyHome)
    }
