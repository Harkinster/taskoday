package com.example.taskoday.di

import com.example.taskoday.features.notifications.AndroidFamilyNotificationNotifier
import com.example.taskoday.features.notifications.FamilyDailySummaryNotificationPublisher
import com.example.taskoday.features.notifications.FamilyNotificationPreferences
import com.example.taskoday.features.notifications.FamilyNotificationScheduler
import com.example.taskoday.features.notifications.SharedPreferencesFamilyNotificationPreferences
import com.example.taskoday.features.notifications.WorkManagerFamilyNotificationScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.taskoday.features.notifications.*

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds @Singleton
    abstract fun bindNotificationGate(impl: SharedPreferencesNotificationSessionGate): NotificationSessionGate
    @Binds @Singleton
    abstract fun bindNotificationLedger(impl: SharedPreferencesAccountNoticeLedger): AccountNoticeLedger
    @Binds @Singleton
    abstract fun bindNotificationSource(impl: ApiNotificationSnapshotSource): NotificationSnapshotSource
    @Binds @Singleton
    abstract fun bindAccountPublisher(impl: AndroidAccountNotificationPublisher): AccountNotificationPublisher
    @Binds
    @Singleton
    abstract fun bindFamilyNotificationPreferences(impl: SharedPreferencesFamilyNotificationPreferences): FamilyNotificationPreferences

    @Binds
    @Singleton
    abstract fun bindFamilyNotificationScheduler(impl: WorkManagerFamilyNotificationScheduler): FamilyNotificationScheduler

    @Binds
    @Singleton
    abstract fun bindFamilyDailySummaryNotificationPublisher(
        impl: AndroidFamilyNotificationNotifier,
    ): FamilyDailySummaryNotificationPublisher
}
