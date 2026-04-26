package com.example.taskoday.di

import com.example.taskoday.data.repository.ProjectRepositoryImpl
import com.example.taskoday.data.repository.ParentPlanningRepositoryImpl
import com.example.taskoday.data.repository.PairingRepositoryImpl
import com.example.taskoday.data.repository.ProfileRepositoryImpl
import com.example.taskoday.data.repository.MissionsRepositoryImpl
import com.example.taskoday.data.repository.PointsRepositoryImpl
import com.example.taskoday.data.repository.PlanningSyncRepositoryImpl
import com.example.taskoday.data.repository.QuestRepositoryImpl
import com.example.taskoday.data.repository.QuestsRepositoryImpl
import com.example.taskoday.data.repository.RewardRepositoryImpl
import com.example.taskoday.data.repository.RoutinesRepositoryImpl
import com.example.taskoday.data.repository.RoutineRepositoryImpl
import com.example.taskoday.data.repository.AuthRepositoryImpl
import com.example.taskoday.data.repository.ChildrenRepositoryImpl
import com.example.taskoday.data.repository.TagRepositoryImpl
import com.example.taskoday.data.repository.TaskRepositoryImpl
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ChildrenRepository
import com.example.taskoday.domain.repository.MissionsRepository
import com.example.taskoday.domain.repository.ParentPlanningRepository
import com.example.taskoday.domain.repository.PairingRepository
import com.example.taskoday.domain.repository.PointsRepository
import com.example.taskoday.domain.repository.PlanningSyncRepository
import com.example.taskoday.domain.repository.ProfileRepository
import com.example.taskoday.domain.repository.ProjectRepository
import com.example.taskoday.domain.repository.QuestRepository
import com.example.taskoday.domain.repository.QuestsRepository
import com.example.taskoday.domain.repository.RewardRepository
import com.example.taskoday.domain.repository.RoutineRepository
import com.example.taskoday.domain.repository.RoutinesRepository
import com.example.taskoday.domain.repository.TagRepository
import com.example.taskoday.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPlanningSyncRepository(impl: PlanningSyncRepositoryImpl): PlanningSyncRepository

    @Binds
    @Singleton
    abstract fun bindChildrenRepository(impl: ChildrenRepositoryImpl): ChildrenRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindRoutinesRepository(impl: RoutinesRepositoryImpl): RoutinesRepository

    @Binds
    @Singleton
    abstract fun bindMissionsRepository(impl: MissionsRepositoryImpl): MissionsRepository

    @Binds
    @Singleton
    abstract fun bindQuestsRepository(impl: QuestsRepositoryImpl): QuestsRepository

    @Binds
    @Singleton
    abstract fun bindPairingRepository(impl: PairingRepositoryImpl): PairingRepository

    @Binds
    @Singleton
    abstract fun bindParentPlanningRepository(impl: ParentPlanningRepositoryImpl): ParentPlanningRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindQuestRepository(impl: QuestRepositoryImpl): QuestRepository

    @Binds
    @Singleton
    abstract fun bindRewardRepository(impl: RewardRepositoryImpl): RewardRepository

    @Binds
    @Singleton
    abstract fun bindPointsRepository(impl: PointsRepositoryImpl): PointsRepository
}
