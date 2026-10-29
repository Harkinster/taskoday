package com.example.taskoday.features.notifications

import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.data.remote.auth.SessionEventBus
import java.time.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface NotificationSessionGate {
    fun current(): NotificationScope?
    fun <T> withCurrent(scope: NotificationScope, action: () -> T): T?
}
interface AccountNoticeLedger {
    fun read(scope: NotificationScope): NotificationLedger
    fun write(scope: NotificationScope, ledger: NotificationLedger)
}
interface NotificationSnapshotSource {
    suspend fun load(scope: NotificationScope, since: Instant, now: ZonedDateTime): NotificationSnapshot
}
interface AccountNotificationPublisher {
    /** All messages are generic; never accepts a task title or family/member name. */
    fun publish(scope: NotificationScope, kind: NoticeKind, count: Int): Boolean
}
enum class NotificationRunResult { DISABLED, STALE_SESSION, NO_SESSION, RETRY, QUIET, CHECKED }

@Singleton
class AccountNotificationRunner @Inject constructor(
    private val auth: AuthRepository,
    private val preferences: FamilyNotificationPreferences,
    private val gate: NotificationSessionGate,
    private val ledger: AccountNoticeLedger,
    private val source: NotificationSnapshotSource,
    private val publisher: AccountNotificationPublisher,
    private val sessionEvents: SessionEventBus = SessionEventBus(),
) {
    private val mutex = Mutex()
    suspend fun run(scope: NotificationScope, now: ZonedDateTime = ZonedDateTime.now()): NotificationRunResult = mutex.withLock {
        sessionEvents.suppressUnauthorizedEvents {
            if (gate.current() != scope) return@suppressUnauthorizedEvents NotificationRunResult.STALE_SESSION
            val settings = preferences.getSettings()
            if (!settings.enabled) return@suppressUnauthorizedEvents NotificationRunResult.DISABLED
            if (auth.getAccessToken().isNullOrBlank()) return@suppressUnauthorizedEvents NotificationRunResult.NO_SESSION
            try {
                fun matches(id: Long, parent: Boolean, families: List<Long>) =
                    gate.current() == scope && id == scope.userId && parent == scope.parent && scope.familyId in families
                val me = auth.fetchMe()
                if (!matches(me.id,me.role.equals("PARENT",true),me.familyIds)) return@suppressUnauthorizedEvents NotificationRunResult.STALE_SESSION
                val old = ledger.read(scope)
                val time = now.toInstant()
                val sinceMs = maxOf(time.minusSeconds(86400).toEpochMilli(),
                    minOf(time.toEpochMilli(), maxOf(settings.enabledSinceMs,old.cursorMs-60000)))
                val snapshot = source.load(scope,Instant.ofEpochMilli(sinceMs),now)
                require(snapshot.familyId == scope.familyId)
                val confirmed = auth.fetchMe()
                if (!matches(confirmed.id,confirmed.role.equals("PARENT",true),confirmed.familyIds)) return@suppressUnauthorizedEvents NotificationRunResult.STALE_SESSION
                gate.withCurrent(scope) {
                    // Settings can change while requests are in flight: use the current values.
                    val current = preferences.getSettings()
                    if (!current.enabled) return@withCurrent NotificationRunResult.DISABLED
                    val latest = ledger.read(scope)
                    val fresh = AccountNotificationPolicy.candidates(scope,current,snapshot,Instant.ofEpochMilli(maxOf(sinceMs,current.enabledSinceMs)),now)
                    val keys = fresh.map { it.key }.toSet()
                    val delivered = latest.delivered.filterValues { it >= time.minusSeconds(14*86400L).toEpochMilli() }.toMutableMap()
                    val pending = (latest.pending + fresh).distinctBy { it.key }.filter {
                        it.key !in delivered && AccountNotificationPolicy.allowed(it.kind,current,scope.parent) && AccountNotificationPolicy.relevant(it,scope,snapshot,keys,time)
                    }.sortedByDescending { it.createdMs }.take(200)
                    val remaining = pending.toMutableList()
                    val quiet = AccountNotificationPolicy.quiet(current,now)
                    if (!quiet) for ((kind,items) in pending.groupBy { it.kind }) {
                        if (publisher.publish(scope,kind,items.size)) {
                            items.forEach { delivered[it.key] = time.toEpochMilli() }
                            remaining.removeAll(items.toSet())
                        }
                    }
                    ledger.write(scope,NotificationLedger(time.toEpochMilli(),delivered.entries.sortedByDescending { it.value }.take(2000).associate { it.toPair() },remaining))
                    if (quiet) NotificationRunResult.QUIET else NotificationRunResult.CHECKED
                } ?: NotificationRunResult.STALE_SESSION
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { NotificationRunResult.RETRY }
        }
    }
}
