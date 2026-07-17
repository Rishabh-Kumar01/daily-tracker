package dev.rishabh.dailytracker.core.db

/**
 * Every enum stored in the database carries an explicit [wire] string rather than relying
 * on Kotlin's `name`. The wire values are the schema's contract and appear in exported
 * template JSON; decoupling them from Kotlin identifiers means a rename here can never
 * silently rewrite what's already on disk.
 */
interface WireEnum {
    val wire: String
}

/**
 * The closed field-type vocabulary.
 *
 * Deliberately resolved from a raw string via [fromWire] rather than stored as an enum
 * column: templates can arrive from import or (later) an LLM draft, and an unrecognised
 * type must render as a read-only "unsupported field" card, never throw. A Room
 * TypeConverter would crash the query instead.
 */
enum class FieldType(override val wire: String) : WireEnum {
    CHECKBOX("checkbox"),
    QUANTITY("quantity"),
    ITEM_VARIANT("item_variant"),
    PHOTO("photo"),
    SCALE("scale"),
    DURATION("duration"),
    NOTE("note"),
    SET_GROUP("set_group"),
    TIME("time"),
    SINGLE_SELECT("single_select"),
    MULTI_SELECT("multi_select"),
    ;

    companion object {
        private val byWire = entries.associateBy(FieldType::wire)

        /** Null for anything outside the vocabulary — callers render "unsupported". */
        fun fromWire(wire: String?): FieldType? = byWire[wire]
    }
}

/** activity_templates.created_by */
enum class CreatedBy(override val wire: String) : WireEnum {
    SYSTEM("system"),
    USER("user"),
    AI_ASSISTED("ai_assisted"),
}

/** items.variant_source */
enum class VariantSource(override val wire: String) : WireEnum {
    USER_LIBRARY("user_library"),
    NUTRITION_DB("nutrition_db"),
}

/** products.source */
enum class ProductSource(override val wire: String) : WireEnum {
    BARCODE_LOOKUP("barcode_lookup"),
    LABEL_OCR("label_ocr"),
    MANUAL("manual"),
    USDA("usda"),
    OFF("off"),
}

/** chapters.ingest_status */
enum class IngestStatus(override val wire: String) : WireEnum {
    PENDING("pending"),
    CHUNKING("chunking"),
    GENERATING_MCQ("generating_mcq"),
    READY("ready"),
    FAILED("failed"),
}

/** sleep_sessions.lock_mode */
enum class LockMode(override val wire: String) : WireEnum {
    STRICT("strict"),
    DND_ONLY("dnd_only"),
    OFF("off"),
}

/** sleep_sessions.mission_type */
enum class MissionType(override val wire: String) : WireEnum {
    PHOTO_OF_ITEM("photo_of_item"),
    MATH("math"),
    NONE("none"),
}

/** media.type */
enum class MediaType(override val wire: String) : WireEnum {
    SCALP("scalp"),
    MEAL("meal"),
    PRODUCT_FRONT("product_front"),
    PRODUCT_BACK("product_back"),
    MISSION_REFERENCE("mission_reference"),
    MISSION_ATTEMPT("mission_attempt"),
    PDF("pdf"),
}

/** ai_jobs.task_type */
enum class AiTaskType(override val wire: String) : WireEnum {
    PDF_INGEST("pdf_ingest"),
    MCQ_GENERATE("mcq_generate"),
    LABEL_EXTRACT("label_extract"),
    TEMPLATE_DRAFT("template_draft"),
    PROFILE_UPDATE("profile_update"),
}

/** ai_jobs.status */
enum class AiJobStatus(override val wire: String) : WireEnum {
    QUEUED("queued"),
    RUNNING("running"),
    DONE("done"),
    FAILED("failed"),
}

/**
 * Nutrient keys used by the macro computation and the Diet UI.
 *
 * product_nutrients is long-format precisely so any micronutrient can be stored without a
 * migration, so this is a convenience list of the well-known keys, NOT a closed set — the
 * column stays a free string.
 */
object NutrientKeys {
    const val ENERGY_KCAL = "energy_kcal"
    const val PROTEIN_G = "protein_g"
    const val CARBS_G = "carbs_g"
    const val FAT_G = "fat_g"
    const val FIBER_G = "fiber_g"
}

/**
 * activity_templates.summary_metric_type values used by the built-in templates.
 *
 * The schema gives these as examples rather than a closed enum, so the column stays TEXT
 * and unknown values degrade to "no summary" rather than failing.
 */
object SummaryMetricTypes {
    const val COMPLETION_PERCENT = "completion_percent"
    const val SUM_FIELD = "sum_field"
}
