package com.example.taskoday.data.demo

import com.example.taskoday.domain.model.PointsSourceType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class DemoPointsDataSource
    @Inject
    constructor() {
        private val balance = MutableStateFlow(INITIAL_BALANCE)
        private val grantedKeys = mutableSetOf<String>()

        fun observeBalance(): Flow<Int> = balance

        fun grant(
            sourceType: PointsSourceType,
            sourceId: Long,
            dayStartMillis: Long,
            points: Int,
        ) {
            val key = "$sourceType:$sourceId:$dayStartMillis"
            if (grantedKeys.add(key)) balance.value += points
        }

        fun revoke(sourceType: PointsSourceType, sourceId: Long, dayStartMillis: Long, points: Int) {
            val key = "$sourceType:$sourceId:$dayStartMillis"
            if (grantedKeys.remove(key)) balance.value = (balance.value - points).coerceAtLeast(0)
        }

        fun spend(cost: Int): Boolean {
            if (balance.value < cost) return false
            balance.value -= cost
            return true
        }

        fun reset() {
            balance.value = INITIAL_BALANCE
            grantedKeys.clear()
        }

        private companion object {
            const val INITIAL_BALANCE = 24
        }
    }
