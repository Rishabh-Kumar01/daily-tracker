package dev.rishabh.dailytracker.feature.builder

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.CreatedBy
import dev.rishabh.dailytracker.core.db.DailyTrackerDatabase
import dev.rishabh.dailytracker.core.db.FakeIdGenerator
import dev.rishabh.dailytracker.core.db.FakeTimeSource
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.feature.activities.ItemLogRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A custom activity is nothing but rows in the four template tables — this proves the
 * builder writes them correctly, validates the draft, and that the result is immediately
 * loggable through the same generic path a built-in uses.
 */
@RunWith(AndroidJUnit4::class)
class ActivityBuilderRepositoryTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var repository: ActivityBuilderRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyTrackerDatabase::class.java,
        ).build()
        repository = ActivityBuilderRepository(db.templateDao(), FakeIdGenerator(), FakeTimeSource())
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private fun field(label: String, type: BuilderFieldType, unit: String = "") =
        FieldDraft(id = label, label = label, type = type, unit = unit)

    private val hairGrowth = ActivityDraft(
        name = "Hair Growth",
        iconKey = "self_improvement",
        colorHex = "#D3A6FF",
        sections = listOf(
            SectionDraft(
                id = "s", name = "Daily",
                items = listOf(
                    ItemDraft(
                        id = "i1", name = "Minoxidil",
                        fields = listOf(
                            field("Applied", BuilderFieldType.CHECKBOX),
                            field("Itchiness", BuilderFieldType.SCALE),
                        ),
                    ),
                    ItemDraft(
                        id = "i2", name = "Water",
                        fields = listOf(field("Amount", BuilderFieldType.QUANTITY, unit = "ml")),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun writes_a_user_activity_as_template_rows() = runTest {
        val result = repository.createActivity(hairGrowth)
        assertThat(result).isInstanceOf(CreateResult.Created::class.java)
        val templateId = (result as CreateResult.Created).templateId

        val template = db.templateDao().getAllTemplates().single { it.templateId == templateId }
        assertThat(template.name).isEqualTo("Hair Growth")
        assertThat(template.createdBy).isEqualTo(CreatedBy.USER)
        assertThat(template.icon).isEqualTo("self_improvement")
        assertThat(template.color).isEqualTo("#D3A6FF")

        val subMenu = db.templateDao().getSubMenus(templateId).single()
        assertThat(subMenu.name).isEqualTo("Daily")
        val items = db.templateDao().getItems(subMenu.subMenuId)
        assertThat(items.map { it.name }).containsExactly("Minoxidil", "Water").inOrder()

        val minoxidil = items.first { it.name == "Minoxidil" }
        val fields = db.templateDao().getFields(minoxidil.itemId)
        assertThat(fields.map { it.fieldKey }).containsExactly("applied", "itchiness").inOrder()
        assertThat(fields.first { it.fieldKey == "itchiness" }.type).isEqualTo(FieldType.SCALE.wire)
        assertThat(fields.first { it.fieldKey == "itchiness" }.optionsJson).contains("\"max\":5")

        val water = db.templateDao().getFields(items.first { it.name == "Water" }.itemId).single()
        assertThat(water.type).isEqualTo(FieldType.QUANTITY.wire)
        assertThat(water.unit).isEqualTo("ml")
    }

    @Test
    fun the_new_activity_is_immediately_loggable() = runTest {
        val templateId = (repository.createActivity(hairGrowth) as CreateResult.Created).templateId
        val subMenuId = db.templateDao().getSubMenus(templateId).single().subMenuId

        val logRepo = ItemLogRepository(db.templateDao(), db.logDao(), FakeIdGenerator(), FakeTimeSource())
        val log = logRepo.observeSubMenuLog(subMenuId).first()
        assertThat(log?.items?.map { it.name }).containsExactly("Minoxidil", "Water").inOrder()
    }

    @Test
    fun a_blank_name_is_rejected_with_a_message() = runTest {
        val result = repository.createActivity(hairGrowth.copy(name = "  "))
        assertThat(result).isEqualTo(CreateResult.Invalid("Give your activity a name."))
        assertThat(db.templateDao().getAllTemplates()).isEmpty()
    }

    @Test
    fun an_item_with_no_fields_is_rejected() = runTest {
        val draft = hairGrowth.copy(
            sections = listOf(
                SectionDraft("s", "Daily", items = listOf(ItemDraft("i", "Minoxidil", fields = emptyList()))),
            ),
        )
        val result = repository.createActivity(draft)
        assertThat(result).isInstanceOf(CreateResult.Invalid::class.java)
        assertThat((result as CreateResult.Invalid).message).contains("Minoxidil")
    }

    @Test
    fun duplicate_field_labels_get_distinct_keys() = runTest {
        val draft = hairGrowth.copy(
            sections = listOf(
                SectionDraft(
                    "s", "Daily",
                    items = listOf(
                        ItemDraft(
                            "i", "Sets",
                            fields = listOf(
                                field("Amount", BuilderFieldType.QUANTITY, "kg"),
                                field("Amount", BuilderFieldType.QUANTITY, "reps"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val templateId = (repository.createActivity(draft) as CreateResult.Created).templateId
        val subMenuId = db.templateDao().getSubMenus(templateId).single().subMenuId
        val itemId = db.templateDao().getItems(subMenuId).single().itemId
        assertThat(db.templateDao().getFields(itemId).map { it.fieldKey })
            .containsExactly("amount", "amount_2").inOrder()
    }
}
