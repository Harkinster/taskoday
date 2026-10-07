package com.example.taskoday.navigation

import com.example.taskoday.domain.model.AuthenticatedUser

internal fun AuthenticatedUser?.preferredAppRoute(isLocalMode: Boolean): String =
    when {
        isLocalMode -> TaskodayDestination.Home.route
        this?.role?.equals("PARENT", ignoreCase = true) == true && familyIds.isEmpty() ->
            TaskodayDestination.FamilyHousehold.route
        this?.role?.equals("PARENT", ignoreCase = true) == true -> TaskodayDestination.FamilyHome.route
        this?.role?.equals("CHILD", ignoreCase = true) == true -> TaskodayDestination.Exploration.route
        else -> TaskodayDestination.Home.route
    }

internal fun accountHomeDestination(hasRemoteSession: Boolean, localChildMode: Boolean, isChild: Boolean = false): TaskodayDestination =
    if (hasRemoteSession && !localChildMode) {
        if (isChild) TaskodayDestination.Exploration else TaskodayDestination.FamilyHome
    } else TaskodayDestination.Home

internal fun canEnterParentDestination(
    verifiedParentSession: Boolean,
    isChild: Boolean,
    localChildMode: Boolean,
): Boolean = verifiedParentSession && !isChild && !localChildMode

internal fun canEnterHouseholdDestination(hasRemoteSession: Boolean, localChildMode: Boolean): Boolean =
    hasRemoteSession && !localChildMode

internal fun chestUtilityRoute(): String =
    TaskodayDestination.Shop.createRoute(TaskodayDestination.Shop.SECTION_CHESTS)

internal fun visibleAccountDestinations(isChild: Boolean, localChildMode: Boolean = false): List<TaskodayDestination> =
    TopLevelDestinations.filterNot {
        ((isChild || localChildMode) && it == TaskodayDestination.FollowUp) ||
            (localChildMode && it == TaskodayDestination.FamilyHome)
    }
