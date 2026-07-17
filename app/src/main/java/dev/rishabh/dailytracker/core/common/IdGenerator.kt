package dev.rishabh.dailytracker.core.common

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source of primary keys.
 *
 * Every PK in the schema is an app-generated UUID. This is an interface, not a bare
 * `UUID.randomUUID()` call, for two reasons: tests need deterministic IDs, and routing all
 * ID creation through one place makes it hard to accidentally accept an ID that came from
 * somewhere else — an LLM proposing a template must never supply its own IDs.
 */
interface IdGenerator {
    fun newId(): String
}

@Singleton
class UuidGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
