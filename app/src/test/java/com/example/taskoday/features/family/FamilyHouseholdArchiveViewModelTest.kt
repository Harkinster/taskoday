package com.example.taskoday.features.family

import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.FamilyInvite
import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.model.FamilyMemberRole
import com.example.taskoday.domain.model.FamilySummary
import com.example.taskoday.domain.repository.FamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyHouseholdArchiveViewModelTest {
    @Test fun `archive selects other family and clears old members`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repo = ArchiveRepository(listOf(7L, 10L), 10L)
            val vm = FamilyHouseholdViewModel(repo)
            assertEquals(listOf(25L), vm.uiState.value.members.map { it.userId })

            vm.archiveFamily()

            assertEquals(10L, repo.archivedId)
            assertEquals(7L, vm.uiState.value.activeFamilyId)
            assertEquals(listOf(7L), vm.uiState.value.families.map { it.id })
            assertTrue(vm.uiState.value.members.isEmpty())
            assertFalse(vm.uiState.value.isLoading)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `archive of sole family shows empty family state`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repo = ArchiveRepository(listOf(10L), 10L)
            val vm = FamilyHouseholdViewModel(repo)
            vm.archiveFamily()
            assertEquals(null, vm.uiState.value.activeFamilyId)
            assertTrue(vm.uiState.value.families.isEmpty())
            assertTrue(vm.uiState.value.members.isEmpty())
            assertFalse(vm.uiState.value.isLoading)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `backend conflict becomes a useful message and keeps current family`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repo = ArchiveRepository(listOf(10L), 10L, conflict = true)
            val vm = FamilyHouseholdViewModel(repo)
            vm.archiveFamily()
            assertEquals(10L, vm.uiState.value.activeFamilyId)
            assertTrue(vm.uiState.value.errorMessage!!.contains("autres membres"))
            assertFalse(vm.uiState.value.isMembershipBusy)
        } finally { Dispatchers.resetMain() }
    }

    private class ArchiveRepository(
        var ids: List<Long>,
        var activeId: Long?,
        val conflict: Boolean = false,
    ) : FamilyRepository {
        var archivedId: Long? = null
        override suspend fun getCurrentUserId(): Long = 25L
        override suspend fun isParentAccount(): Boolean = true
        override suspend fun getActiveFamilyId(): Long? = activeId
        override fun setActiveFamilyId(familyId: Long) { activeId = familyId }
        override suspend fun fetchFamilies(): Result<List<FamilySummary>> =
            Result.success(ids.map { FamilySummary(it, "Famille $it") })
        override suspend fun fetchMembers(): Result<List<FamilyMember>> =
            Result.success(if (activeId == 10L) listOf(FamilyMember(25L, "Parent", null, FamilyMemberRole.PARENT, true)) else emptyList())
        override suspend fun archiveFamily(familyId: Long): Result<Long?> {
            if (conflict) return Result.failure(HttpException(Response.error<Unit>(409, "{}".toResponseBody("application/json".toMediaType()))))
            archivedId = familyId
            ids = ids.filterNot { it == familyId }
            activeId = ids.firstOrNull()
            return Result.success(activeId)
        }
        override suspend fun createParentInvite(): Result<FamilyInvite> = error("Not used")
        override suspend fun acceptParentInvite(code: String): Result<AuthenticatedUser> = error("Not used")
    }
}
