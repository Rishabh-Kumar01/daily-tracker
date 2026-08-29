package dev.rishabh.dailytracker.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.rishabh.dailytracker.core.db.CreatedBy
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.ItemEntity
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.db.entity.SubMenuEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    // --- Reads ---

    /** Home screen: the activity list, archived hidden. */
    @Query("SELECT * FROM activity_templates WHERE is_archived = 0 ORDER BY sort_order, name")
    fun observeActiveTemplates(): Flow<List<ActivityTemplateEntity>>

    @Query("SELECT * FROM activity_templates ORDER BY sort_order, name")
    suspend fun getAllTemplates(): List<ActivityTemplateEntity>

    @Query("SELECT * FROM activity_templates WHERE template_id = :templateId")
    suspend fun getTemplate(templateId: String): ActivityTemplateEntity?

    @Query("SELECT * FROM activity_templates WHERE name = :name AND created_by = :createdBy LIMIT 1")
    suspend fun findTemplateByName(name: String, createdBy: CreatedBy): ActivityTemplateEntity?

    /** Highest sort_order in use, or -1 when empty — a new activity appends after it. */
    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM activity_templates")
    suspend fun maxTemplateSortOrder(): Int

    @Query("SELECT COUNT(*) FROM activity_templates WHERE created_by = :createdBy")
    suspend fun countTemplatesBy(createdBy: CreatedBy): Int

    @Query("SELECT * FROM sub_menus WHERE template_id = :templateId ORDER BY sort_order, name")
    suspend fun getSubMenus(templateId: String): List<SubMenuEntity>

    @Query("SELECT * FROM sub_menus WHERE template_id = :templateId ORDER BY sort_order, name")
    fun observeSubMenus(templateId: String): Flow<List<SubMenuEntity>>

    @Query("SELECT * FROM sub_menus WHERE sub_menu_id = :subMenuId")
    suspend fun getSubMenu(subMenuId: String): SubMenuEntity?

    @Query("SELECT * FROM items WHERE sub_menu_id = :subMenuId ORDER BY sort_order, name")
    suspend fun getItems(subMenuId: String): List<ItemEntity>

    @Query("SELECT * FROM items WHERE sub_menu_id = :subMenuId ORDER BY sort_order, name")
    fun observeItems(subMenuId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE item_id = :itemId")
    suspend fun getItem(itemId: String): ItemEntity?

    @Query("SELECT * FROM item_fields WHERE item_id = :itemId ORDER BY sort_order")
    suspend fun getFields(itemId: String): List<ItemFieldEntity>

    /**
     * Fields for a whole sub-menu in one query, so rendering a meal screen doesn't fan out
     * into one query per item.
     */
    @Query(
        """
        SELECT f.* FROM item_fields f
        INNER JOIN items i ON i.item_id = f.item_id
        WHERE i.sub_menu_id = :subMenuId
        ORDER BY i.sort_order, f.sort_order
        """,
    )
    suspend fun getFieldsForSubMenu(subMenuId: String): List<ItemFieldEntity>

    // --- Writes ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplate(template: ActivityTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubMenus(subMenus: List<SubMenuEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFields(fields: List<ItemFieldEntity>)

    @Upsert
    suspend fun upsertTemplate(template: ActivityTemplateEntity)

    @Query("UPDATE activity_templates SET is_archived = :archived WHERE template_id = :templateId")
    suspend fun setArchived(templateId: String, archived: Boolean)

    /**
     * Installs a whole template in one transaction.
     *
     * All-or-nothing on purpose: a half-written template would render as a broken activity
     * with no way for the user to tell why.
     */
    @Transaction
    suspend fun insertFullTemplate(
        template: ActivityTemplateEntity,
        subMenus: List<SubMenuEntity>,
        items: List<ItemEntity>,
        fields: List<ItemFieldEntity>,
    ) {
        insertTemplate(template)
        insertSubMenus(subMenus)
        insertItems(items)
        insertFields(fields)
    }
}
