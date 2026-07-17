package dev.rishabh.dailytracker.core.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import dev.rishabh.dailytracker.core.db.entity.ActivityTemplateEntity
import dev.rishabh.dailytracker.core.db.entity.ItemEntity
import dev.rishabh.dailytracker.core.db.entity.ItemFieldEntity
import dev.rishabh.dailytracker.core.db.entity.SubMenuEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateDaoTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var dao: TemplateDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyTrackerDatabase::class.java,
        ).build()
        dao = db.templateDao()
    }

    @After
    fun tearDown() = db.close()

    private fun template(id: String, name: String, sortOrder: Int = 0, archived: Boolean = false) =
        ActivityTemplateEntity(
            templateId = id,
            name = name,
            icon = "restaurant",
            color = "#75D78D",
            createdBy = CreatedBy.SYSTEM,
            summaryMetricType = SummaryMetricTypes.SUM_FIELD,
            summaryMetricLabel = "kcal",
            sortOrder = sortOrder,
            isArchived = archived,
            createdAt = 1L,
        )

    @Test
    fun insertFullTemplate_roundTripsWholeHierarchy() = runTest {
        val t = template("t1", "Diet")
        val sub = SubMenuEntity("s1", "t1", "Lunch", 0, """{"type":"daily"}""")
        val item = ItemEntity("i1", "s1", "Paneer", hasVariants = true, variantSource = VariantSource.USER_LIBRARY, sortOrder = 0)
        val field = ItemFieldEntity("f1", "i1", "amount", FieldType.QUANTITY.wire, "Amount", "g", true, 0, """{"min":0}""")

        dao.insertFullTemplate(t, listOf(sub), listOf(item), listOf(field))

        assertThat(dao.getTemplate("t1")).isEqualTo(t)
        assertThat(dao.getSubMenus("t1")).containsExactly(sub)
        assertThat(dao.getItems("s1")).containsExactly(item)
        assertThat(dao.getFields("i1")).containsExactly(field)
    }

    @Test
    fun enumsSurviveTheWireRoundTrip() = runTest {
        val t = template("t1", "Diet").copy(createdBy = CreatedBy.AI_ASSISTED)
        dao.insertFullTemplate(t, emptyList(), emptyList(), emptyList())

        // The point of the wire strings: what comes back is what went in, by value.
        assertThat(dao.getTemplate("t1")!!.createdBy).isEqualTo(CreatedBy.AI_ASSISTED)
        assertThat(dao.findTemplateByName("Diet", CreatedBy.AI_ASSISTED)).isNotNull()
        assertThat(dao.findTemplateByName("Diet", CreatedBy.SYSTEM)).isNull()
    }

    @Test
    fun observeActiveTemplates_hidesArchivedAndSortsBySortOrder() = runTest {
        dao.insertTemplate(template("t2", "Workout", sortOrder = 1))
        dao.insertTemplate(template("t1", "Diet", sortOrder = 0))
        dao.insertTemplate(template("t3", "Study", sortOrder = 2, archived = true))

        val active = dao.observeActiveTemplates().first()

        assertThat(active.map { it.name }).containsExactly("Diet", "Workout").inOrder()
    }

    @Test
    fun setArchived_removesFromActiveList() = runTest {
        dao.insertTemplate(template("t1", "Diet"))
        assertThat(dao.observeActiveTemplates().first()).hasSize(1)

        dao.setArchived("t1", true)

        assertThat(dao.observeActiveTemplates().first()).isEmpty()
        // Soft delete: the row itself survives, so history keeps resolving.
        assertThat(dao.getTemplate("t1")).isNotNull()
    }

    @Test
    fun getFieldsForSubMenu_returnsEveryFieldInItemThenFieldOrder() = runTest {
        val t = template("t1", "Diet")
        val sub = SubMenuEntity("s1", "t1", "Lunch", 0, null)
        val paneer = ItemEntity("i1", "s1", "Paneer", sortOrder = 0)
        val dal = ItemEntity("i2", "s1", "Dal", sortOrder = 1)
        val fields = listOf(
            ItemFieldEntity("f2", "i1", "amount", FieldType.QUANTITY.wire, "Amount", "g", true, 1, null),
            ItemFieldEntity("f1", "i1", "variant", FieldType.ITEM_VARIANT.wire, "Brand", null, true, 0, null),
            ItemFieldEntity("f3", "i2", "amount", FieldType.QUANTITY.wire, "Amount", "g", true, 0, null),
        )
        dao.insertFullTemplate(t, listOf(sub), listOf(paneer, dal), fields)

        val result = dao.getFieldsForSubMenu("s1")

        // Ordered by item then field so the meal screen renders in one pass.
        assertThat(result.map { it.fieldId }).containsExactly("f1", "f2", "f3").inOrder()
    }

    @Test
    fun unknownFieldTypeIsReadableRatherThanFatal() = runTest {
        // An imported template may carry a type this build has never heard of. It must come
        // back out of the DB so the renderer can show "unsupported", not blow up the query.
        val t = template("t1", "Imported")
        val sub = SubMenuEntity("s1", "t1", "Sub", 0, null)
        val item = ItemEntity("i1", "s1", "Item", sortOrder = 0)
        val alien = ItemFieldEntity("f1", "i1", "mystery", "holographic_slider", "Mystery", null, false, 0, null)
        dao.insertFullTemplate(t, listOf(sub), listOf(item), listOf(alien))

        val field = dao.getFields("i1").single()

        assertThat(field.type).isEqualTo("holographic_slider")
        assertThat(FieldType.fromWire(field.type)).isNull()
    }

    @Test
    fun deletingTemplateCascadesThroughTheHierarchy() = runTest {
        val t = template("t1", "Diet")
        val sub = SubMenuEntity("s1", "t1", "Lunch", 0, null)
        val item = ItemEntity("i1", "s1", "Paneer", sortOrder = 0)
        val field = ItemFieldEntity("f1", "i1", "amount", FieldType.QUANTITY.wire, "Amount", "g", true, 0, null)
        dao.insertFullTemplate(t, listOf(sub), listOf(item), listOf(field))

        db.openHelper.writableDatabase.execSQL("DELETE FROM activity_templates WHERE template_id = 't1'")

        assertThat(dao.getSubMenus("t1")).isEmpty()
        assertThat(dao.getItems("s1")).isEmpty()
        assertThat(dao.getFields("i1")).isEmpty()
    }

    @Test(expected = SQLiteConstraintException::class)
    fun subMenuRequiresAnExistingTemplate() = runTest {
        dao.insertSubMenus(listOf(SubMenuEntity("s1", "nope", "Orphan", 0, null)))
    }

    @Test
    fun insertFullTemplate_isAtomic() = runTest {
        val t = template("t1", "Diet")
        val sub = SubMenuEntity("s1", "t1", "Lunch", 0, null)
        // Points at an item that is never inserted, so the FK fails mid-transaction.
        val orphanField = ItemFieldEntity("f1", "missing-item", "amount", FieldType.QUANTITY.wire, "Amount", "g", true, 0, null)

        runCatching { dao.insertFullTemplate(t, listOf(sub), emptyList(), listOf(orphanField)) }

        // A half-written template would render as a broken activity, so nothing may land.
        assertThat(dao.getTemplate("t1")).isNull()
        assertThat(dao.getSubMenus("t1")).isEmpty()
    }
}
