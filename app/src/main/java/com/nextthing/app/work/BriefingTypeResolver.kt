package com.nextthing.app.work

import com.nextthing.app.domain.service.AIBriefingGenerator.BriefingType
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.min

internal object BriefingTypeResolver {

    fun resolve(
        scheduledType: String?,
        now: LocalTime,
        morningTime: LocalTime,
        eveningTime: LocalTime
    ): BriefingType {
        scheduledType?.let { rawType ->
            runCatching { BriefingType.valueOf(rawType) }.getOrNull()?.let { return it }
        }

        val nowMinute = now.hour * MINUTES_PER_HOUR + now.minute
        val morningMinute = morningTime.hour * MINUTES_PER_HOUR + morningTime.minute
        val eveningMinute = eveningTime.hour * MINUTES_PER_HOUR + eveningTime.minute

        return if (
            circularMinuteDistance(nowMinute, morningMinute) <=
            circularMinuteDistance(nowMinute, eveningMinute)
        ) {
            BriefingType.MORNING
        } else {
            BriefingType.EVENING
        }
    }

    private fun circularMinuteDistance(first: Int, second: Int): Int {
        val directDistance = abs(first - second)
        return min(directDistance, MINUTES_PER_DAY - directDistance)
    }

    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
}
