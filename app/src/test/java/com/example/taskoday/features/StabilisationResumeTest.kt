package com.example.taskoday.features

import com.example.taskoday.domain.model.*
import com.example.taskoday.domain.repository.*
import com.example.taskoday.features.exploration.ExplorationViewModel
import com.example.taskoday.features.followup.FollowUpViewModel
import com.example.taskoday.features.gamification.NestViewModel
import com.example.taskoday.data.repository.NestRepository
import com.example.taskoday.data.remote.gamification.NestApi
import com.example.taskoday.data.remote.dto.*
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StabilisationResumeTest {
    private var todayCalls=0
    private val auth=object : AuthRepository by unused() {
        override fun getAccessToken()="test-only-session"
        override suspend fun fetchMe()=AuthenticatedUser(1L,"qa@example.test","PARENT",true,listOf(1L))
        override suspend fun getActiveFamilyId(forceRefresh:Boolean)=1L
        override suspend fun getActiveChildId(forceRefresh:Boolean):Long?=null
    }
    private val family=object : FamilyTasksRepository by unused() {
        override suspend fun fetchMembers()=Result.success(listOf(FamilyTaskMember(1L,"Parent",null,FamilyTaskMemberRole.PARENT,true)))
        override suspend fun fetchToday():Result<FamilyTasksToday> {todayCalls++;return Result.success(FamilyTasksToday(1L,"2026-10-25",emptyList()))}
        override suspend fun fetchOverdueOccurrences()=Result.success(FamilyTaskOccurrencesRange(1L,"2026-10-01","2026-10-25",emptyList()))
    }
    private val children=object : ChildrenRepository by unused() {
        override suspend fun fetchChildren()=emptyList<ParentChild>()
    }
    private fun exploration()=ExplorationViewModel(auth,children,family,unused(),unused(),unused(),unused(),unused(),unused())

    @Test fun `initial exploration resume does not restart an in-flight load and later resume refreshes`()=scenario {
        val vm=exploration()
        vm.onScreenResumed()
        runCurrent()
        assertEquals(1,todayCalls);assertFalse(vm.uiState.value.isLoading);assertNull(vm.uiState.value.errorMessage)
        vm.onScreenResumed();runCurrent()
        assertEquals(2,todayCalls)
        vm.refresh();runCurrent();assertEquals(3,todayCalls)
    }
    @Test fun `initial exploration resume after completed load does not duplicate the request`()=scenario {
        val vm=exploration();runCurrent()
        vm.onScreenResumed();runCurrent();assertEquals(1,todayCalls)
        vm.onScreenResumed();runCurrent();assertEquals(2,todayCalls)
    }
    @Test fun `follow-up preserves initial load and refreshes when returning to the screen`()=scenario {
        val vm=FollowUpViewModel(auth,children,family,unused(),unused(),unused(),unused())
        vm.onScreenResumed();runCurrent()
        assertEquals(1,todayCalls);assertFalse(vm.uiState.value.isLoading);assertNull(vm.uiState.value.errorMessage)
        vm.onScreenResumed();runCurrent();assertEquals(2,todayCalls)
    }
    @Test fun `nest refreshes personal companions once initially and again on subsequent resume`()=scenario {
        var calls=0
        val api=object : NestApi by unused() {
            override suspend fun getMyCompanions():ApiEnvelopeDto<MyCompanionsDto> {calls++;return ApiEnvelopeDto(true,MyCompanionsDto(emptyList(),activeState="NONE"))}
            override suspend fun getMyActiveCompanion()=ApiEnvelopeDto<PersonalCompanionDto?>(true,null)
            override suspend fun getPersonalRewards(familyId:Long)=ApiEnvelopeDto(true,PersonalRewardSummaryDto(1L,1L,0,0,0))
        }
        val vm=NestViewModel(NestRepository(api,auth))
        vm.onScreenResumed();runCurrent()
        assertEquals(1,calls);assertTrue(vm.uiState.value.personalCompanionsLoaded)
        vm.onScreenResumed();runCurrent();assertEquals(2,calls)
    }
    private fun scenario(block:suspend TestScope.()->Unit)=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {block()} finally {Dispatchers.resetMain()}
    }
}
private inline fun <reified T> unused():T=Proxy.newProxyInstance(T::class.java.classLoader,arrayOf(T::class.java)){_,method,_->error("Unexpected call: ${method.name}")} as T
