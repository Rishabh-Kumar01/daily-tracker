package dev.rishabh.dailytracker.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's clock.
 *
 * Injected rather than called statically so tests can pin time — seeding, log timestamps
 * and `local_date` bucketing all depend on it.
 */
interface TimeSource {
    /** Epoch millis UTC. */
    fun nowMillis(): Long

    /** The device timezone, which is what `local_date` is expressed in. */
    fun zone(): ZoneId

    /** Today as the denormalised `YYYY-MM-DD` string the log tables index. */
    fun today(): String = localDateOf(nowMillis())

    /** The `local_date` bucket an instant falls into, in the device timezone. */
    fun localDateOf(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalDate().format(ISO_DATE)

    companion object {
        val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun parse(localDate: String): LocalDate = LocalDate.parse(localDate, ISO_DATE)
    }
}

@Singleton
class SystemTimeSource @Inject constructor() : TimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
