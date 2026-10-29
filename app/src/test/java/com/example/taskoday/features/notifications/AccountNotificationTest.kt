package com.example.taskoday.features.notifications

import com.example.taskoday.domain.model.*
import com.example.taskoday.domain.repository.AuthRepository
import java.lang.reflect.Proxy
import java.time.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AccountNotificationTest {
    private val now = ZonedDateTime.parse("2026-10-28T12:00:00+01:00[Europe/Paris]")
    private val child = NotificationScope(3,7,false,"session-a")
    private val parent = NotificationScope(1,7,true,"session-p")
    private val settings = FamilyNotificationSettings(enabled=true,quietEnabled=false)
    private fun task(owner: Long=3,status: FamilyTaskStatus=FamilyTaskStatus.TODO,priority:FamilyTaskPriority=FamilyTaskPriority.NORMAL,cycle:Int=1) =
        FamilyTaskTodayItem(10,20,"PRIVATE TASK TITLE",listOf(FamilyTaskAssignee(owner,"PRIVATE NAME")),"2026-10-28","2026-10-28","12:05",true,
            "2026-10-28T11:05:00Z",status,true,false,priority,completedByUser=if(status==FamilyTaskStatus.PENDING_VALIDATION)FamilyTaskActor(owner,"PRIVATE NAME") else null,
            scope=FamilyActionScope.PERSONAL,kind=FamilyActionKind.MISSION,cycleNumber=cycle)
    private fun event(type:FamilyTaskEventType,to:FamilyTaskStatus,id:Long=30) = FamilyTaskEvent(id,7,10,20,FamilyActionType.PERSONAL_MISSION,
        "PRIVATE TITLE",type,FamilyTaskStatus.PENDING_VALIDATION,to,1,"PRIVATE PARENT",3,emptyList(),now.toInstant(),false,cycleNumber=1)
    private fun snapshot(tasks:List<FamilyTaskTodayItem> = listOf(task()),events:List<FamilyTaskEvent> = emptyList()) = NotificationSnapshot(7,tasks,events,setOf(3,4),setOf(1,2))
    private fun plan(scope:NotificationScope=child,data:NotificationSnapshot=snapshot(),s:FamilyNotificationSettings=settings) =
        AccountNotificationPolicy.candidates(scope,s,data,now.toInstant().minusSeconds(3600),now)

    @Test fun `personal deadlines never leak another member or a house task`() {
        assertEquals(listOf(NoticeKind.PERSONAL_DUE),plan().map {it.kind})
        assertTrue(plan(data=snapshot(listOf(task(owner=4)))).isEmpty())
        assertTrue(plan(data=snapshot(listOf(task().copy(scope=FamilyActionScope.HOUSE)))).isEmpty())
    }
    @Test fun `only parents receive child missions needing validation`() {
        val data=snapshot(listOf(task(status=FamilyTaskStatus.PENDING_VALIDATION)))
        assertEquals(listOf(NoticeKind.VALIDATION_NEEDED),plan(parent,data).map{it.kind})
        assertTrue(plan(child,data).isEmpty())
        assertTrue(plan(parent,snapshot(listOf(task(owner=1,status=FamilyTaskStatus.PENDING_VALIDATION)))).isEmpty())
    }
    @Test fun `decisions target the completer and reopened missions are not labelled purchases or failures`() {
        val approved=event(FamilyTaskEventType.VALIDATE,FamilyTaskStatus.VALIDATED)
        assertEquals(NoticeKind.MISSION_VALIDATED,plan(data=snapshot(listOf(task(status=FamilyTaskStatus.VALIDATED)),listOf(approved))).single().kind)
        assertTrue(plan(child.copy(userId=4),snapshot(listOf(task(status=FamilyTaskStatus.VALIDATED)),listOf(approved))).isEmpty())
        val returned=event(FamilyTaskEventType.REOPEN,FamilyTaskStatus.TODO)
        assertTrue(plan(data=snapshot(listOf(task(cycle=2)),listOf(returned))).any {it.kind==NoticeKind.MISSION_RETURNED})
        assertEquals("Une de tes missions est à reprendre.",accountNotificationText(NoticeKind.MISSION_RETURNED,1))
    }
    @Test fun `only unfinished important overdue tasks trigger late alerts`() {
        val late=task(priority=FamilyTaskPriority.HIGH).copy(dueAt="2026-10-28T09:00:00Z")
        assertEquals(listOf(NoticeKind.IMPORTANT_OVERDUE),plan(data=snapshot(listOf(late))).map{it.kind})
        assertTrue(plan(data=snapshot(listOf(late.copy(status=FamilyTaskStatus.VALIDATED)))).isEmpty())
        assertTrue(plan(data=snapshot(listOf(late.copy(priority=FamilyTaskPriority.NORMAL)))).isEmpty())
    }
    @Test fun `shared mission decisions also target only their actual completer`() {
        val shared=task(status=FamilyTaskStatus.VALIDATED).copy(scope=FamilyActionScope.HOUSE)
        val decision=event(FamilyTaskEventType.VALIDATE,FamilyTaskStatus.VALIDATED).copy(scope=FamilyActionScope.HOUSE)
        assertEquals(NoticeKind.MISSION_VALIDATED,plan(data=snapshot(listOf(shared),listOf(decision))).single().kind)
        assertTrue(plan(child.copy(userId=4),snapshot(listOf(shared),listOf(decision))).isEmpty())
    }
    @Test fun `a rescheduled important deadline can create a new alert without repeating the old one`() {
        val late=task(priority=FamilyTaskPriority.HIGH).copy(dueAt="2026-10-28T08:00:00Z")
        val old=plan(data=snapshot(listOf(late))).single {it.kind==NoticeKind.IMPORTANT_OVERDUE}
        val changed=plan(data=snapshot(listOf(late.copy(dueAt="2026-10-28T09:00:00Z")))).single {it.kind==NoticeKind.IMPORTANT_OVERDUE}
        assertNotEquals(old.key,changed.key)
        assertEquals(old.key,plan(data=snapshot(listOf(late))).single {it.kind==NoticeKind.IMPORTANT_OVERDUE}.key)
    }
    @Test fun `quiet windows handle midnight boundaries and daylight saving zones`() {
        val s=settings.copy(quietEnabled=true)
        assertTrue(AccountNotificationPolicy.quiet(s,now.withHour(22)))
        assertTrue(AccountNotificationPolicy.quiet(s,now.withHour(7)))
        assertFalse(AccountNotificationPolicy.quiet(s,now.withHour(8)))
        assertTrue(AccountNotificationPolicy.quiet(s,ZonedDateTime.parse("2026-10-25T02:30:00+02:00[Europe/Paris]")))
        assertTrue(AccountNotificationPolicy.quiet(s,ZonedDateTime.parse("2026-10-25T02:30:00+01:00[Europe/Paris]")))
        assertTrue(AccountNotificationPolicy.quiet(s,now.withZoneSameInstant(ZoneId.of("America/New_York"))))
        assertFalse(AccountNotificationPolicy.quiet(s,now.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))))
    }
    @Test fun `categories suppress delivery and messages cannot carry source titles`() {
        assertTrue(plan(s=settings.copy(personalDue=false)).isEmpty())
        for(kind in NoticeKind.entries) {
            val text=accountNotificationText(kind,4)
            assertFalse(text.contains("PRIVATE"));assertFalse(text.contains("@"))
        }
    }
    @Test fun `runner deduplicates across new instances and repeated polls`() = runBlocking {
        val f=Fixture();assertEquals(NotificationRunResult.CHECKED,f.runner().run(child,now))
        assertEquals(1,f.published);f.runner().run(child,now.plusMinutes(1));assertEquals(1,f.published)
    }
    @Test fun `quiet delivery remains pending and drains after quiet hours`() = runBlocking {
        val f=Fixture();f.config=settings.copy(quietEnabled=true,quietStartMinute=11*60,quietEndMinute=13*60)
        assertEquals(NotificationRunResult.QUIET,f.runner().run(child,now));assertEquals(0,f.published);assertEquals(1,f.state.pending.size)
        f.config=f.config.copy(quietEnabled=false);f.runner().run(child,now.plusMinutes(1));assertEquals(1,f.published);assertTrue(f.state.pending.isEmpty())
    }
    @Test fun `loss of network preserves cursor and retries without false delivery`() = runBlocking {
        val f=Fixture();f.failure=true
        assertEquals(NotificationRunResult.RETRY,f.runner().run(child,now));assertEquals(0L,f.state.cursorMs);assertEquals(0,f.published)
        f.failure=false;f.runner().run(child,now);assertEquals(1,f.published)
    }
    @Test fun `logout account change and family change during fetch suppress stale responses`() = runBlocking {
        for(next in listOf<NotificationScope?>(null,child.copy(userId=4,generation="other"),child.copy(familyId=8,generation="family"))) {
            val f=Fixture();f.switchDuringFetch=next;f.switch=true
            assertEquals(NotificationRunResult.STALE_SESSION,f.runner().run(child,now));assertEquals(0,f.published);assertEquals(0L,f.state.cursorMs)
        }
    }
    @Test fun `permission denial does not consume a pending event`() = runBlocking {
        val f=Fixture();f.permission=false;f.runner().run(child,now)
        assertEquals(0,f.published);assertEquals(1,f.state.pending.size)
        f.permission=true;f.runner().run(child,now.plusMinutes(1));assertEquals(1,f.published)
    }
    @Test fun `ledger changes during a network request cannot be overwritten by an old read`() = runBlocking {
        val f=Fixture()
        f.onFetch={f.state=NotificationLedger(now.toInstant().toEpochMilli(),plan().associate {it.key to now.toInstant().toEpochMilli()})}
        assertEquals(NotificationRunResult.CHECKED,f.runner().run(child,now))
        assertEquals(0,f.published);assertEquals(1,f.state.delivered.size)
    }
    @Test fun `completed or rescheduled tasks are removed from a quiet backlog`() = runBlocking {
        val f=Fixture();f.config=settings.copy(quietEnabled=true,quietStartMinute=11*60,quietEndMinute=13*60)
        f.runner().run(child,now);f.data=snapshot(listOf(task(status=FamilyTaskStatus.VALIDATED)));f.config=settings
        f.runner().run(child,now.plusMinutes(1));assertEquals(0,f.published);assertTrue(f.state.pending.isEmpty())
    }
    @Test fun `revoked membership disabled settings and no token prevent delivery`() = runBlocking {
        val f=Fixture();f.families=emptyList();assertEquals(NotificationRunResult.STALE_SESSION,f.runner().run(child,now))
        f.families=listOf(7);f.token=null;assertEquals(NotificationRunResult.NO_SESSION,f.runner().run(child,now))
        f.config=settings.copy(enabled=false);assertEquals(NotificationRunResult.DISABLED,f.runner().run(child,now));assertEquals(0,f.published)
    }
    @Test fun `a snapshot from another family is never accepted`() = runBlocking {
        val f=Fixture();f.data=f.data.copy(familyId=8)
        assertEquals(NotificationRunResult.RETRY,f.runner().run(child,now));assertEquals(0,f.published)
    }

    private inner class Fixture {
        var current:NotificationScope?=child;var config=settings;var state=NotificationLedger();var data=snapshot()
        var failure=false;var switch=false;var switchDuringFetch:NotificationScope?=null;var permission=true;var published=0
        var onFetch:(()->Unit)?=null
        var token:String?="qa-test-session";var families:List<Long> = listOf(7)
        private val auth=object:AuthRepository by unused() {
            override fun getAccessToken()=token
            override suspend fun fetchMe()=AuthenticatedUser(3,"qa@example.test","CHILD",true,families,"QA")
        }
        fun runner()=AccountNotificationRunner(auth,object:FamilyNotificationPreferences {
            override fun getSettings()=config
            override fun saveSettings(settings:FamilyNotificationSettings){config=settings}
        },object:NotificationSessionGate {
            override fun current()=current
            override fun <T> withCurrent(scope:NotificationScope,action:()->T):T?=if(scope==current)action() else null
        },object:AccountNoticeLedger {
            override fun read(scope:NotificationScope)=state
            override fun write(scope:NotificationScope,ledger:NotificationLedger){state=ledger}
        },object:NotificationSnapshotSource {
            override suspend fun load(scope:NotificationScope,since:Instant,now:ZonedDateTime):NotificationSnapshot {
                if(failure)throw java.io.IOException("QA offline")
                if(switch)current=switchDuringFetch
                onFetch?.invoke()
                return data
            }
        },object:AccountNotificationPublisher {
            override fun publish(scope:NotificationScope,kind:NoticeKind,count:Int):Boolean {if(permission)published++;return permission}
        })
    }
    @Suppress("UNCHECKED_CAST") private inline fun <reified T> unused():T =
        Proxy.newProxyInstance(T::class.java.classLoader,arrayOf(T::class.java)){_,method,_->error(method.name)} as T
}
