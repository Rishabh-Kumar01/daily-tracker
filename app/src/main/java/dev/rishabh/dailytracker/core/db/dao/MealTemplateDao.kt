package dev.rishabh.dailytracker.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.rishabh.dailytracker.core.db.entity.MealTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.MealTemplateItemEntity
import kotlinx.coroutines.flow.Flow

/** A template's name plus how many food lines it holds — what the chip row shows. */
data class MealTemplateSummary(
    val mealTemplateId: String,
    val name: String,
    val itemCount: Int,
)

@Dao
interface MealTemplateDao {

    // --- Reads ---

    /**
     * The saved templates for a meal, newest first, each with its line count.
     *
     * A left join keeps a template with no surviving items visible (count 0) rather than
     * dropping it silently.
     */
    @Query(
        """
        SELECT t.meal_template_id AS mealTemplateId,
               t.name AS name,
               COUNT(i.id) AS itemCount
        FROM meal_templates t
        LEFT JOIN meal_template_items i ON i.meal_template_id = t.meal_template_id
        WHERE t.sub_menu_id = :subMenuId
        GROUP BY t.meal_template_id
        ORDER BY t.created_at DESC
        """,
    )
    fun observeForSubMenu(subMenuId: String): Flow<List<MealTemplateSummary>>

    @Query("SELECT * FROM meal_templates WHERE meal_template_id = :mealTemplateId")
    suspend fun getTemplate(mealTemplateId: String): MealTemplateEntity?

    @Query("SELECT * FROM meal_template_items WHERE meal_template_id = :mealTemplateId")
    suspend fun getItems(mealTemplateId: String): List<MealTemplateItemEntity>

    // --- Writes ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplate(template: MealTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<MealTemplateItemEntity>)

    /** A template and its food lines are one save, so they commit together. */
    @Transaction
    suspend fun insertTemplateWithItems(
        template: MealTemplateEntity,
        items: List<MealTemplateItemEntity>,
    ) {
        insertTemplate(template)
        insertItems(items)
    }

    /** Cascades to meal_template_items via the foreign key. */
    @Query("DELETE FROM meal_templates WHERE meal_template_id = :mealTemplateId")
    suspend fun deleteTemplate(mealTemplateId: String)
}
