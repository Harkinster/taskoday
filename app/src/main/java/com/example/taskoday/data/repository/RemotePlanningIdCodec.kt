package com.example.taskoday.data.repository

import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.RemotePlanningRef

internal object RemotePlanningIdCodec {
    private const val ROUTINE_SUFFIX = 1L
    private const val MISSION_SUFFIX = 2L
    private const val QUEST_SUFFIX = 3L

    fun encodeTaskId(itemType: PlanningItemType, remoteItemId: Long): Long {
        require(itemType == PlanningItemType.ROUTINE || itemType == PlanningItemType.MISSION)
        val suffix = if (itemType == PlanningItemType.ROUTINE) ROUTINE_SUFFIX else MISSION_SUFFIX
        return -((remoteItemId * 10L) + suffix)
    }

    fun encodeQuestId(remoteItemId: Long): Long = -((remoteItemId * 10L) + QUEST_SUFFIX)

    fun decodeTaskId(localTaskId: Long): RemotePlanningRef? {
        if (localTaskId >= 0L) return null
        val raw = -localTaskId
        val remoteId = raw / 10L
        return when (raw % 10L) {
            ROUTINE_SUFFIX -> RemotePlanningRef(PlanningItemType.ROUTINE, remoteId)
            MISSION_SUFFIX -> RemotePlanningRef(PlanningItemType.MISSION, remoteId)
            else -> null
        }
    }

    fun decodeQuestId(localQuestId: Long): RemotePlanningRef? {
        if (localQuestId >= 0L) return null
        val raw = -localQuestId
        val remoteId = raw / 10L
        return if (raw % 10L == QUEST_SUFFIX) {
            RemotePlanningRef(PlanningItemType.QUEST, remoteId)
        } else {
            null
        }
    }
}
