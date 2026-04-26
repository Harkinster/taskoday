package com.example.taskoday.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.taskoday.data.local.dao.PointsTransactionDao
import com.example.taskoday.data.local.dao.ProjectDao
import com.example.taskoday.data.local.dao.QuestCompletionDao
import com.example.taskoday.data.local.dao.QuestDao
import com.example.taskoday.data.local.dao.RewardDao
import com.example.taskoday.data.local.dao.RoutineDao
import com.example.taskoday.data.local.dao.TagDao
import com.example.taskoday.data.local.dao.TaskCheckDao
import com.example.taskoday.data.local.dao.TaskDao
import com.example.taskoday.data.local.entity.PointsTransactionEntity
import com.example.taskoday.data.local.entity.ProjectEntity
import com.example.taskoday.data.local.entity.QuestCompletionEntity
import com.example.taskoday.data.local.entity.QuestEntity
import com.example.taskoday.data.local.entity.RewardEntity
import com.example.taskoday.data.local.entity.RoutineEntity
import com.example.taskoday.data.local.entity.TagEntity
import com.example.taskoday.data.local.entity.TaskCheckEntity
import com.example.taskoday.data.local.entity.TaskEntity
import com.example.taskoday.data.local.entity.TaskTagCrossRefEntity

@Database(
    entities = [
        TaskEntity::class,
        ProjectEntity::class,
        RoutineEntity::class,
        TagEntity::class,
        TaskTagCrossRefEntity::class,
        TaskCheckEntity::class,
        QuestEntity::class,
        QuestCompletionEntity::class,
        RewardEntity::class,
        PointsTransactionEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskCheckDao(): TaskCheckDao
    abstract fun projectDao(): ProjectDao
    abstract fun routineDao(): RoutineDao
    abstract fun tagDao(): TagDao
    abstract fun questDao(): QuestDao
    abstract fun questCompletionDao(): QuestCompletionDao
    abstract fun rewardDao(): RewardDao
    abstract fun pointsTransactionDao(): PointsTransactionDao

    companion object {
        const val DATABASE_NAME: String = "taskoday.db"

        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN taskType TEXT NOT NULL DEFAULT 'ONE_TIME'")
                    db.execSQL("ALTER TABLE tasks ADD COLUMN dayPart TEXT NOT NULL DEFAULT 'MORNING'")
                    db.execSQL("ALTER TABLE tasks ADD COLUMN scheduledDate INTEGER")

                    db.execSQL(
                        """
                        UPDATE tasks
                        SET taskType = CASE WHEN isRoutine = 1 THEN 'DAILY' ELSE 'ONE_TIME' END
                        """.trimIndent(),
                    )

                    db.execSQL(
                        """
                        UPDATE tasks
                        SET dayPart = CASE
                            WHEN dueDate IS NULL THEN 'MORNING'
                            WHEN CAST(strftime('%H', dueDate / 1000, 'unixepoch', 'localtime') AS INTEGER) BETWEEN 5 AND 8 THEN 'MORNING'
                            WHEN CAST(strftime('%H', dueDate / 1000, 'unixepoch', 'localtime') AS INTEGER) BETWEEN 9 AND 11 THEN 'LATE_MORNING'
                            WHEN CAST(strftime('%H', dueDate / 1000, 'unixepoch', 'localtime') AS INTEGER) BETWEEN 12 AND 13 THEN 'NOON'
                            WHEN CAST(strftime('%H', dueDate / 1000, 'unixepoch', 'localtime') AS INTEGER) BETWEEN 14 AND 17 THEN 'AFTERNOON'
                            ELSE 'EVENING'
                        END
                        """.trimIndent(),
                    )

                    db.execSQL(
                        """
                        UPDATE tasks
                        SET scheduledDate = CASE
                            WHEN dueDate IS NULL THEN CAST(strftime('%s', 'now', 'localtime', 'start of day', 'utc') AS INTEGER) * 1000
                            ELSE CAST(strftime('%s', dueDate / 1000, 'unixepoch', 'localtime', 'start of day', 'utc') AS INTEGER) * 1000
                        END
                        """.trimIndent(),
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS task_checks (
                            taskId INTEGER NOT NULL,
                            dayStartMillis INTEGER NOT NULL,
                            isChecked INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            PRIMARY KEY(taskId, dayStartMillis),
                            FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )

                    db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_taskType ON tasks(taskType)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_dayPart ON tasks(dayPart)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_scheduledDate ON tasks(scheduledDate)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_task_checks_taskId ON task_checks(taskId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_task_checks_dayStartMillis ON task_checks(dayStartMillis)")
                }
            }

        val MIGRATION_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN emoji TEXT NOT NULL DEFAULT '\u2B50'")
                    db.execSQL(
                        """
                        UPDATE tasks
                        SET emoji = CASE dayPart
                            WHEN 'MORNING' THEN '🌞'
                            WHEN 'LATE_MORNING' THEN '🧸'
                            WHEN 'NOON' THEN '🍽️'
                            WHEN 'AFTERNOON' THEN '🎮'
                            ELSE '🌙'
                        END
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_3_4: Migration =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN routineDays TEXT")

                    db.execSQL(
                        """
                        UPDATE tasks
                        SET dayPart = CASE dayPart
                            WHEN 'MORNING' THEN 'MATIN'
                            WHEN 'LATE_MORNING' THEN 'MATINEE'
                            WHEN 'NOON' THEN 'MIDI'
                            WHEN 'AFTERNOON' THEN 'APRES_MIDI'
                            WHEN 'EVENING' THEN 'SOIR'
                            ELSE dayPart
                        END
                        """.trimIndent(),
                    )

                    db.execSQL(
                        """
                        UPDATE tasks
                        SET dayPart = CASE
                            WHEN dayPart IS NULL OR dayPart = '' THEN 'MATIN'
                            ELSE dayPart
                        END
                        """.trimIndent(),
                    )

                    db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_routineDays ON tasks(routineDays)")
                }
            }

        val MIGRATION_4_5: Migration =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS quests (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT,
                            emoji TEXT NOT NULL,
                            pointsReward INTEGER NOT NULL,
                            isActive INTEGER NOT NULL,
                            dayPart TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_quests_isActive ON quests(isActive)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_quests_dayPart ON quests(dayPart)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS quest_completions (
                            questId INTEGER NOT NULL,
                            dayStartMillis INTEGER NOT NULL,
                            completedAt INTEGER NOT NULL,
                            PRIMARY KEY(questId, dayStartMillis),
                            FOREIGN KEY(questId) REFERENCES quests(id) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_quest_completions_questId ON quest_completions(questId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_quest_completions_dayStartMillis ON quest_completions(dayStartMillis)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS rewards (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT,
                            cost INTEGER NOT NULL,
                            emoji TEXT NOT NULL,
                            isActive INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_rewards_isActive ON rewards(isActive)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_rewards_cost ON rewards(cost)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS points_transactions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            amount INTEGER NOT NULL,
                            reason TEXT NOT NULL,
                            sourceType TEXT NOT NULL,
                            sourceId INTEGER,
                            dayStartMillis INTEGER,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_points_transactions_sourceType ON points_transactions(sourceType)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_points_transactions_sourceId ON points_transactions(sourceId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_points_transactions_dayStartMillis ON points_transactions(dayStartMillis)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_points_transactions_createdAt ON points_transactions(createdAt)")
                }
            }
    }
}
