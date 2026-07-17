package dev.rishabh.dailytracker.core.db

import androidx.room.TypeConverter

/**
 * Enum <-> wire-string converters.
 *
 * These cover the enums the app itself writes, where an unknown value means the database
 * is corrupt and failing loudly is correct. item_fields.type is deliberately NOT here: it
 * is an import surface and must degrade to an "unsupported field" card instead of throwing.
 */
class Converters {

    private inline fun <reified T> decode(wire: String?, values: Array<T>): T?
        where T : Enum<T>, T : WireEnum {
        if (wire == null) return null
        return values.firstOrNull { it.wire == wire }
            ?: error("Unknown ${T::class.simpleName} value in database: '$wire'")
    }

    @TypeConverter fun createdByToWire(v: CreatedBy?): String? = v?.wire

    @TypeConverter fun wireToCreatedBy(v: String?): CreatedBy? = decode(v, CreatedBy.entries.toTypedArray())

    @TypeConverter fun variantSourceToWire(v: VariantSource?): String? = v?.wire

    @TypeConverter fun wireToVariantSource(v: String?): VariantSource? = decode(v, VariantSource.entries.toTypedArray())

    @TypeConverter fun productSourceToWire(v: ProductSource?): String? = v?.wire

    @TypeConverter fun wireToProductSource(v: String?): ProductSource? = decode(v, ProductSource.entries.toTypedArray())

    @TypeConverter fun ingestStatusToWire(v: IngestStatus?): String? = v?.wire

    @TypeConverter fun wireToIngestStatus(v: String?): IngestStatus? = decode(v, IngestStatus.entries.toTypedArray())

    @TypeConverter fun lockModeToWire(v: LockMode?): String? = v?.wire

    @TypeConverter fun wireToLockMode(v: String?): LockMode? = decode(v, LockMode.entries.toTypedArray())

    @TypeConverter fun missionTypeToWire(v: MissionType?): String? = v?.wire

    @TypeConverter fun wireToMissionType(v: String?): MissionType? = decode(v, MissionType.entries.toTypedArray())

    @TypeConverter fun mediaTypeToWire(v: MediaType?): String? = v?.wire

    @TypeConverter fun wireToMediaType(v: String?): MediaType? = decode(v, MediaType.entries.toTypedArray())

    @TypeConverter fun aiTaskTypeToWire(v: AiTaskType?): String? = v?.wire

    @TypeConverter fun wireToAiTaskType(v: String?): AiTaskType? = decode(v, AiTaskType.entries.toTypedArray())

    @TypeConverter fun aiJobStatusToWire(v: AiJobStatus?): String? = v?.wire

    @TypeConverter fun wireToAiJobStatus(v: String?): AiJobStatus? = decode(v, AiJobStatus.entries.toTypedArray())
}
