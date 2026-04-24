package com.example.taskoday.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.taskoday.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SeedDatabaseCallback
    @Inject
    constructor(
        private val databaseProvider: Provider<AppDatabase>,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            applicationScope.launch {
                seedDatabase()
            }
        }

        private suspend fun seedDatabase() {
            val database = databaseProvider.get()
            val now = System.currentTimeMillis()

            val projectIds = database.projectDao().insertAll(SeedData.projects())
            database.tagDao().insertAll(SeedData.tags())
            database.routineDao().insertAll(SeedData.routines(nowMillis = now))
            database.taskDao().insertAll(SeedData.tasks(projectIds = projectIds, nowMillis = now))
        }
    }
