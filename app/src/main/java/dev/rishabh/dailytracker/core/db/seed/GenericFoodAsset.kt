package dev.rishabh.dailytracker.core.db.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Deserialization model for the generic_foods.v1.json asset.
 *
 * Kept as a separate data layer from [dev.rishabh.dailytracker.core.db.entity.ProductEntity]
 * because the JSON shape (slug, per_100g nested object, category, prep) is specific to the
 * asset format and should not leak into the Room schema.
 */
@Serializable
data class GenericFoodAsset(
    @SerialName("dataset_version") val datasetVersion: Int,
    @SerialName("foods") val foods: List<FoodEntry>,
)

@Serializable
data class FoodEntry(
    @SerialName("slug") val slug: String,
    @SerialName("generic_name") val genericName: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("category") val category: String? = null,
    @SerialName("prep") val prep: String? = null,
    @SerialName("default_serving_g") val defaultServingG: Int,
    @SerialName("source_form") val sourceForm: String? = null,
    @SerialName("source_db") val sourceDb: String? = null,
    @SerialName("source_ref") val sourceRef: String? = null,
    @SerialName("is_approx") val isApprox: Boolean = false,
    /** How this food is logged: "grams" | "count" | "household". Defaults to grams. */
    @SerialName("serving_unit") val servingUnit: String = "grams",
    /** The unit noun shown in the picker, e.g. "egg", "katori", "tsp". Null for grams. */
    @SerialName("unit_label") val unitLabel: String? = null,
    /** Grams in one [unitLabel]; the input-to-storage conversion factor. Null for grams. */
    @SerialName("grams_per_unit") val gramsPerUnit: Double? = null,
    @SerialName("per_100g") val per100g: Map<String, Double>,
)

/** Lenient parser: ignore unknown keys so future asset additions never break the seeder. */
internal val genericFoodJson = Json { ignoreUnknownKeys = true }
