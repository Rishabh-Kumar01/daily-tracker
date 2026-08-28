package dev.rishabh.dailytracker.core.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The user's optional daily macro goals. A null field means "no goal set for this macro". */
data class DietTargets(
    val kcal: Double? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
) {
    /** True when at least one goal is set — the day header shows progress only then. */
    val any: Boolean get() = kcal != null || proteinG != null || carbsG != null || fatG != null
}

private val Context.dietTargets by preferencesDataStore(name = "diet_targets")

/**
 * Daily macro goals, stored as plain user settings — never in the schema.
 *
 * Goals are a preference the user can change any day, not history, so they live in a
 * DataStore rather than a table (the spec is explicit: targets are settings, not schema).
 * They are not sensitive, so unlike the USDA key they are stored in the clear.
 */
@Singleton
class DietTargetsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val targets: Flow<DietTargets> = context.dietTargets.data.map { it.toTargets() }

    /** Persists [targets]; a null field clears that macro's goal. */
    suspend fun setTargets(targets: DietTargets) {
        context.dietTargets.edit { prefs ->
            prefs.setOrClear(KCAL, targets.kcal)
            prefs.setOrClear(PROTEIN, targets.proteinG)
            prefs.setOrClear(CARBS, targets.carbsG)
            prefs.setOrClear(FAT, targets.fatG)
        }
    }

    private fun Preferences.toTargets() = DietTargets(
        kcal = this[KCAL],
        proteinG = this[PROTEIN],
        carbsG = this[CARBS],
        fatG = this[FAT],
    )

    private companion object {
        val KCAL = doublePreferencesKey("kcal")
        val PROTEIN = doublePreferencesKey("protein_g")
        val CARBS = doublePreferencesKey("carbs_g")
        val FAT = doublePreferencesKey("fat_g")
    }
}

private fun androidx.datastore.preferences.core.MutablePreferences.setOrClear(
    key: Preferences.Key<Double>,
    value: Double?,
) {
    if (value == null) remove(key) else set(key, value)
}
