package com.example.taskoday.features.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesNotificationSessionGate @Inject constructor(@ApplicationContext context: Context) : NotificationSessionGate {
    private val prefs = context.getSharedPreferences("taskoday_notification_session",Context.MODE_PRIVATE)
    @Synchronized fun owner(): Long? = prefs.getLong("user",0).takeIf { it > 0 }
    @Synchronized fun generation(): String = prefs.getString("generation","").orEmpty()
    @Synchronized override fun current(): NotificationScope? {
        val owner = owner() ?: return null
        val family = prefs.getLong("family",0).takeIf { it > 0 } ?: return null
        return NotificationScope(owner,family,prefs.getBoolean("parent",false),prefs.getString("generation","")!!)
    }
    @Synchronized fun bind(user: Long, parent: Boolean, family: Long?): Boolean {
        require(user > 0 && (family == null || family > 0))
        if (owner()==user && prefs.getBoolean("parent",false)==parent && prefs.getLong("family",0)==(family ?: 0L)) return false
        prefs.edit().putLong("user",user).putBoolean("parent",parent).putLong("family",family ?: 0)
            .putString("generation",UUID.randomUUID().toString()).commit()
        return true
    }
    @Synchronized fun familyChanged(family: Long?): Boolean = owner()?.let { bind(it,prefs.getBoolean("parent",false),family) } ?: false
    @Synchronized fun invalidate() { prefs.edit().clear().commit() }
    @Synchronized override fun <T> withCurrent(scope: NotificationScope, action: () -> T): T? = if (current()==scope) action() else null
    @Synchronized fun <T> withOwner(user: Long, action: () -> T): T? = if (owner()==user) action() else null
}
