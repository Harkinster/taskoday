package com.example.taskoday.navigation

import com.example.taskoday.domain.model.AuthenticatedUser

internal fun AuthenticatedUser?.preferredAppRoute(isLocalMode: Boolean): String =
    when {
        isLocalMode -> TaskodayDestination.Home.route
        this?.role?.equals("PARENT", ignoreCase = true) == true && familyIds.isEmpty() ->
            TaskodayDestination.FamilyHousehold.route
        this?.role?.equals("PARENT", ignoreCase = true) == true ||
            this?.role?.equals("CHILD", ignoreCase = true) == true -> TaskodayDestination.FamilyHome.route
        else -> TaskodayDestination.Home.route
    }

internal fun accountHomeDestination(hasRemoteSession: Boolean, localChildMode: Boolean): TaskodayDestination =
    if (hasRemoteSession && !localChildMode) TaskodayDestination.FamilyHome else TaskodayDestination.Home

internal fun canEnterParentDestination(
    verifiedParentSession: Boolean,
    isChild: Boolean,
    localChildMode: Boolean,
): Boolean = verifiedParentSession && !isChild && !localChildMode

internal fun canEnterHouseholdDestination(hasRemoteSession: Boolean, localChildMode: Boolean): Boolean =
    hasRemoteSession && !localChildMode

internal fun visibleAccountDestinations(isChild: Boolean, localChildMode: Boolean = false): List<TaskodayDestination> =
    TopLevelDestinations.filterNot {
        ((isChild || localChildMode) && it == TaskodayDestination.FollowUp) ||
            (localChildMode && it == TaskodayDestination.FamilyHome)
    }
