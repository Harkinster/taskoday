package com.example.taskoday.features.familyhome

import androidx.lifecycle.SavedStateHandle
import com.example.taskoday.domain.model.*
import com.example.taskoday.domain.repository.*
import com.example.taskoday.features.followup.FollowUpViewModel
import java.lang.reflect.Proxy
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

/** LOCAL_SIMULATION_PASS only: real ViewModels, in-memory repositories, no server or credentials. */
@OptIn(ExperimentalCoroutinesApi::class)
class FamilyTaskJourneyTest {
    @Test fun `parent creates assigned house mission child starts and completes parent validates`() = scenario { world ->
        val create = world.creator()
        runCurrent()
        create.onTitleChanged("Ranger les livres")
        create.toggleAssignee(101L)
        create.onValidationRequiredChanged(true)
        create.submit()
        runCurrent()
        assertTrue(create.uiState.value.created)
        assertEquals(listOf(101L), world.lastInput!!.assigneeUserIds)
        assertEquals(FamilyActionScope.HOUSE, world.lastInput!!.scope)

        val parentHome = FamilyHomeViewModel(world.tasks, world.auth(world.parent))
        val childHome = FamilyHomeViewModel(world.tasks, world.auth(world.child))
        runCurrent()
        assertEquals("Ranger les livres", parentHome.uiState.value.todayTasks.single().task.title)
        world.actor = world.child
        childHome.runQuickAction(childHome.uiState.value.todayTasks.single().task)
        runCurrent()
        assertEquals(FamilyTaskStatus.IN_PROGRESS, world.occurrence!!.status)
        val childDetail = world.detail(world.child)
        runCurrent()
        assertTrue(childDetail.uiState.value.canCompleteToday)
        childDetail.completeTodayOccurrence()
        runCurrent()
        assertEquals(FamilyTaskStatus.PENDING_VALIDATION, childDetail.uiState.value.todayOccurrence!!.status)
        assertFalse(childDetail.uiState.value.canValidateToday)

        world.actor = world.parent
        val followUp = world.followUp()
        val parentDetail = world.detail(world.parent)
        runCurrent()
        assertEquals(FamilyTaskStatus.PENDING_VALIDATION, followUp.uiState.value.house!!.items.single().familyTask!!.status)
        parentDetail.validateTodayOccurrence()
        runCurrent()
        followUp.refresh()
        parentHome.refresh()
        runCurrent()
        assertEquals(FamilyTaskStatus.VALIDATED, parentDetail.uiState.value.todayOccurrence!!.status)
        assertEquals(1, followUp.uiState.value.house!!.completed)
        assertEquals(1, parentHome.uiState.value.completedTasks)
        assertEquals(1, world.createCalls)
        assertEquals(1, world.completeCalls)
        assertEquals(1, world.validateCalls)
    }

    @Test fun `personal mission belongs to selected child and sibling cannot view or mutate`() = scenario { world ->
        val create = world.creator(FamilyActionType.PERSONAL_MISSION)
        runCurrent()
        create.onTitleChanged("Préparer son cartable")
        create.selectPersonalAssignee(world.child.id)
        create.submit()
        runCurrent()
        assertEquals(listOf(world.child.id), world.lastInput!!.assigneeUserIds)
        val child = world.detail(world.child)
        val sibling = world.detail(world.sibling)
        runCurrent()
        assertNotNull(child.uiState.value.task)
        assertNull(sibling.uiState.value.task)
        assertNotNull(sibling.uiState.value.errorMessage)
        sibling.completeTodayOccurrence()
        child.validateTodayOccurrence()
        child.deleteTask()
        runCurrent()
        assertEquals(0, world.completeCalls)
        assertEquals(0, world.validateCalls)
        assertEquals(0, world.deleteCalls)
        world.actor = world.child
        child.completeTodayOccurrence()
        runCurrent()
        assertEquals(FamilyTaskStatus.COMPLETED, child.uiState.value.todayOccurrence!!.status)
        val followUp = world.followUp()
        runCurrent()
        assertEquals(1, followUp.uiState.value.members.first { it.memberId == world.child.id }.completed)
        assertEquals(0, followUp.uiState.value.members.first { it.memberId == world.sibling.id }.total)
    }

    @Test fun `rapid submit before coroutine dispatch creates exactly one task`() = scenario { world ->
        val create = world.creator()
        runCurrent()
        create.onTitleChanged("Une seule mission")
        world.createGate = CompletableDeferred()
        create.submit()
        create.submit()
        runCurrent()
        assertEquals(1, world.createCalls)
        world.createGate!!.complete(Unit)
        runCurrent()
        assertTrue(create.uiState.value.created)
    }

    @Test fun `full form assignment command selects one personal member and multiple house members`() = scenario { world ->
        val personal = world.creator(FamilyActionType.PERSONAL_MISSION)
        val house = world.creator()
        runCurrent()
        personal.selectAssignee(world.child.id)
        personal.selectAssignee(world.sibling.id)
        assertEquals(setOf(world.sibling.id), personal.uiState.value.selectedAssigneeUserIds)
        house.selectAssignee(world.child.id)
        house.selectAssignee(world.sibling.id)
        assertEquals(setOf(world.child.id, world.sibling.id), house.uiState.value.selectedAssigneeUserIds)
        house.selectAssignee(999L)
        assertEquals(setOf(world.child.id, world.sibling.id), house.uiState.value.selectedAssigneeUserIds)
    }

    @Test fun `draft is marked unsaved on edits and cleared only after successful save`() = scenario { world ->
        val create = world.creator()
        runCurrent()
        assertFalse(create.uiState.value.hasUnsavedChanges)
        create.onTitleChanged("Brouillon à conserver")
        assertTrue(create.uiState.value.hasUnsavedChanges)
        world.createError = java.net.SocketTimeoutException()
        create.submit()
        runCurrent()
        assertTrue(create.uiState.value.hasUnsavedChanges)
        world.createError = null
        create.submit()
        runCurrent()
        assertFalse(create.uiState.value.hasUnsavedChanges)
    }

    @Test fun `submit during initial load does not create an unassigned task`() = scenario { world ->
        val create = world.creator()
        create.onTitleChanged("Attendre les membres")
        create.submit()
        runCurrent()
        assertEquals(0, world.createCalls)
        assertFalse(create.uiState.value.created)
    }

    @Test fun `repeated delete while dispatch is pending sends one command`() = scenario { world ->
        world.seed()
        val detail = world.detail(world.parent)
        runCurrent()
        detail.deleteTask()
        detail.deleteTask()
        runCurrent()
        assertEquals(1, world.deleteCalls)
    }

    @Test fun `a successful form cannot submit again while navigation is pending`() = scenario { world ->
        val create = world.creator()
        runCurrent()
        create.onTitleChanged("Une seule sauvegarde")
        create.submit()
        runCurrent()
        create.submit()
        runCurrent()
        assertEquals(1, world.createCalls)
    }

    @Test fun `creation failure preserves draft and retry succeeds without false success`() = scenario { world ->
        val create = world.creator()
        runCurrent()
        create.onTitleChanged("Conserver ce brouillon")
        create.toggleAssignee(101L)
        world.createError = java.net.SocketTimeoutException()
        create.submit()
        runCurrent()
        assertFalse(create.uiState.value.created)
        assertFalse(create.uiState.value.isSubmitting)
        assertNotNull(create.uiState.value.errorMessage)
        assertEquals("Conserver ce brouillon", create.uiState.value.title)
        assertEquals(setOf(101L), create.uiState.value.selectedAssigneeUserIds)
        world.createError = null
        create.submit()
        runCurrent()
        assertTrue(create.uiState.value.created)
    }

    @Test fun `child cannot create and a changed family prevents submission`() = scenario { world ->
        val forbidden = world.creator(user = world.child)
        runCurrent()
        forbidden.onTitleChanged("Interdit")
        forbidden.submit()
        runCurrent()
        assertFalse(forbidden.uiState.value.created)
        assertEquals(0, world.createCalls)
        val parent = world.creator()
        runCurrent()
        parent.onTitleChanged("Autre famille")
        world.familyId = 2L
        parent.submit()
        runCurrent()
        assertFalse(parent.uiState.value.created)
        assertEquals(0, world.createCalls)
    }

    @Test fun `inactive assignee prevents creation`() = scenario { world ->
        val create = world.creator()
        runCurrent()
        create.onTitleChanged("Membre désactivé")
        create.toggleAssignee(world.child.id)
        world.members = world.members.map { if (it.userId == world.child.id) it.copy(isActive = false) else it }
        create.submit()
        runCurrent()
        assertFalse(create.uiState.value.created)
        assertEquals(0, world.createCalls)
    }

    @Test fun `home repeated action while request is pending sends one command`() = scenario { world ->
        world.seed()
        val home = FamilyHomeViewModel(world.tasks, world.auth(world.child))
        runCurrent()
        val task = home.uiState.value.todayTasks.single().task
        world.startGate = CompletableDeferred()
        home.runQuickAction(task)
        home.runQuickAction(task)
        runCurrent()
        assertEquals(1, world.startCalls)
        world.startGate!!.complete(Unit)
        runCurrent()
    }

    @Test fun `completion and validation errors never report success or change displayed status`() = scenario { world ->
        world.seed()
        world.occurrence = world.occurrence!!.copy(status = FamilyTaskStatus.IN_PROGRESS, contributors = listOf(FamilyTaskActor(101L, "Enfant")))
        val child = world.detail(world.child)
        runCurrent()
        world.commandError = java.net.UnknownHostException()
        child.completeTodayOccurrence()
        runCurrent()
        assertEquals(FamilyTaskStatus.IN_PROGRESS, child.uiState.value.todayOccurrence!!.status)
        assertNull(child.uiState.value.successMessage)
        assertNotNull(child.uiState.value.errorMessage)
        world.occurrence = world.occurrence!!.copy(status = FamilyTaskStatus.PENDING_VALIDATION)
        val parent = world.detail(world.parent)
        runCurrent()
        parent.validateTodayOccurrence()
        runCurrent()
        assertEquals(FamilyTaskStatus.PENDING_VALIDATION, parent.uiState.value.todayOccurrence!!.status)
        assertNull(parent.uiState.value.successMessage)
        assertNotNull(parent.uiState.value.errorMessage)
    }

    private fun scenario(block: suspend TestScope.(LocalFamily) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block(LocalFamily()) } finally { Dispatchers.resetMain() }
    }
}

/** Shared fake state models server transitions explicitly; it does not validate the real backend. */
private class LocalFamily {
    val parent = AuthenticatedUser(100L, "parent@example.test", "PARENT", true, listOf(1L), "Parent")
    val child = parent.copy(id = 101L, role = "CHILD", displayName = "Enfant")
    val sibling = child.copy(id = 102L, displayName = "Autre enfant")
    var actor = parent
    var familyId = 1L
    val date = LocalDate.now().toString()
    var members = listOf(parent, child, sibling).map { FamilyTaskMember(it.id, it.displayName, it.email, FamilyTaskMemberRole.valueOf(it.role), true) }
    var definition: FamilyTaskDefinition? = null
    var occurrence: FamilyTaskTodayItem? = null
    var lastInput: FamilyTaskCreateInput? = null
    var createCalls = 0
    var completeCalls = 0
    var validateCalls = 0
    var startCalls = 0
    var deleteCalls = 0
    var createGate: CompletableDeferred<Unit>? = null
    var startGate: CompletableDeferred<Unit>? = null
    var createError: Throwable? = null
    var commandError: Throwable? = null

    fun auth(user: AuthenticatedUser) = object : AuthRepository by unused() {
        override suspend fun fetchMe() = user
    }
    private val family = object : FamilyRepository by unused() {
        override suspend fun getActiveFamilyId() = familyId
    }
    val tasks = object : FamilyTasksRepository by unused() {
        override suspend fun fetchMembers() = Result.success(members)
        override suspend fun fetchTasks() = Result.success(listOfNotNull(definition))
        override suspend fun fetchTask(taskId: Long) = definition?.let { Result.success(it) } ?: Result.failure(IllegalArgumentException("Introuvable"))
        override suspend fun fetchToday() = Result.success(FamilyTasksToday(familyId, date, listOfNotNull(occurrence)))
        override suspend fun fetchOccurrences(startDate: String, endDate: String) = Result.success(FamilyTaskOccurrencesRange(familyId, startDate, endDate, emptyList()))
        override suspend fun fetchOverdueOccurrences() = Result.success(FamilyTaskOccurrencesRange(familyId, date, date, emptyList()))
        override suspend fun createTask(input: FamilyTaskCreateInput): Result<Unit> {
            createCalls++
            createGate?.await()
            createError?.let { return Result.failure(it) }
            lastInput = input
            val assignees = input.assigneeUserIds.map { id -> FamilyTaskAssignee(id, members.first { it.userId == id }.displayName) }
            definition = FamilyTaskDefinition(1L, familyId, input.title, input.description, assignees, input.dueDate, input.dueTime, false, null, input.recurrence, input.selectedWeekdays, input.validationRequired, input.gamificationEnabled, input.priority, true, category = input.category)
            occurrence = FamilyTaskTodayItem(1L, 11L, input.title, assignees, date, input.dueDate, input.dueTime, false, null, FamilyTaskStatus.TODO, input.validationRequired, input.gamificationEnabled, input.priority, category = input.category)
            return Result.success(Unit)
        }
        override suspend fun startOccurrence(occurrenceId: Long): Result<Unit> {
            startCalls++
            startGate?.await()
            occurrence = occurrence!!.copy(status = FamilyTaskStatus.IN_PROGRESS, contributors = listOf(FamilyTaskActor(actor.id, actor.displayName)))
            return Result.success(Unit)
        }
        override suspend fun completeOccurrenceWithBundle(occurrenceId: Long): Result<CompletionReward> {
            completeCalls++
            commandError?.let { return Result.failure(it) }
            occurrence = occurrence!!.copy(status = if (occurrence!!.validationRequired) FamilyTaskStatus.PENDING_VALIDATION else FamilyTaskStatus.COMPLETED, completedByUser = FamilyTaskActor(actor.id, actor.displayName))
            return Result.success(CompletionReward())
        }
        override suspend fun validateOccurrenceWithBundle(occurrenceId: Long): Result<CompletionReward> {
            validateCalls++
            commandError?.let { return Result.failure(it) }
            occurrence = occurrence!!.copy(status = FamilyTaskStatus.VALIDATED, validatedByUser = FamilyTaskActor(actor.id, actor.displayName))
            return Result.success(CompletionReward(xp = 5))
        }
        override suspend fun deleteTask(taskId: Long): Result<Unit> { deleteCalls++; return Result.success(Unit) }
    }
    fun creator(type: FamilyActionType = FamilyActionType.HOUSE_MISSION, user: AuthenticatedUser = parent) =
        FamilyTaskCreateViewModel(SavedStateHandle(mapOf("kind" to type.name)), tasks, auth(user), family)
    fun detail(user: AuthenticatedUser) = FamilyTaskDetailViewModel(SavedStateHandle(mapOf("taskId" to 1L)), tasks, auth(user))
    fun followUp() = FollowUpViewModel(auth(parent), object : ChildrenRepository by unused() {
        override suspend fun fetchChildren() = emptyList<ParentChild>()
    }, tasks, unused(), unused(), unused(), unused())
    suspend fun seed() { tasks.createTask(FamilyTaskCreateInput("Mission locale", null, date, null, FamilyTaskRecurrence.NONE, emptyList(), listOf(child.id), true, false, FamilyTaskPriority.NORMAL, category = FamilyActionType.HOUSE_MISSION.category)) }
}

private inline fun <reified T> unused(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
    error("Unexpected repository call: ${method.name}")
} as T
