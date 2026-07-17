package dev.rishabh.dailytracker.core.db

import dev.rishabh.dailytracker.core.common.IdGenerator
import dev.rishabh.dailytracker.core.common.TimeSource
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

/** Sequential IDs so assertions can name rows instead of chasing random UUIDs. */
class FakeIdGenerator(private val prefix: String = "id") : IdGenerator {
    private val counter = AtomicInteger(0)
    override fun newId(): String = "$prefix-${counter.incrementAndGet()}"
    fun issued(): Int = counter.get()
}

/** Pinned clock; UTC so local_date bucketing is predictable wherever tests run. */
class FakeTimeSource(var now: Long = FIXED_NOW) : TimeSource {
    override fun nowMillis(): Long = now
    override fun zone(): ZoneId = ZoneId.of("UTC")

    companion object {
        /** 2026-07-17T12:00:00Z */
        const val FIXED_NOW = 1_784_030_400_000L
    }
}
