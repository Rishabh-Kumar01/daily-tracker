package dev.rishabh.dailytracker.core.db.seed

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.rishabh.dailytracker.core.db.CreatedBy
import dev.rishabh.dailytracker.core.db.DailyTrackerDatabase
import dev.rishabh.dailytracker.core.db.FakeIdGenerator
import dev.rishabh.dailytracker.core.db.FakeTimeSource
import dev.rishabh.dailytracker.core.db.FieldType
import dev.rishabh.dailytracker.core.db.VariantSource
import dev.rishabh.dailytracker.core.db.dao.TemplateDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateSeederTest {

    private lateinit var db: DailyTrackerDatabase
    private lateinit var dao: TemplateDao
    private lateinit var ids: FakeIdGenerator
    private lateinit var time: FakeTimeSource
    private lateinit var seeder: TemplateSeeder

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyTrackerDatabase::class.java,
        ).build()
        dao = db.templateDao()
        ids = FakeIdGenerator()
        time = FakeTimeSource()
        seeder = TemplateSeeder(dao, db.logDao(), ids, time)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun seedsTheFourBuiltInActivities() = runTest {
        val installed = seeder.seedIfNeeded()

        assertThat(installed).containsExactly("Diet", "Workout", "Study", "Sleep").inOrder()
        assertThat(dao.observeActiveTemplates().first().map { it.name })
            .containsExactly("Diet", "Workout", "Study", "Sleep").inOrder()
    }

    @Test
    fun builtInsAreMarkedSystemAndCarryTheirAccentColour() = runTest {
        seeder.seedIfNeeded()

        val byName = dao.getAllTemplates().associateBy { it.name }
        assertThat(byName.values.map { it.createdBy }.toSet()).containsExactly(CreatedBy.SYSTEM)
        // Colours match the gamut-mapped accents from the design tokens.
        assertThat(byName["Diet"]!!.color).isEqualTo("#75D78D")
        assertThat(byName["Workout"]!!.color).isEqualTo("#FFA460")
        assertThat(byName["Study"]!!.color).isEqualTo("#7BC3FF")
        assertThat(byName["Sleep"]!!.color).isEqualTo("#D3A6FF")
        // Icons are the design system's canonical activity icons.
        assertThat(byName["Diet"]!!.icon).isEqualTo("restaurant")
        assertThat(byName["Sleep"]!!.icon).isEqualTo("bedtime")
    }

    @Test
    fun seedingTwiceInstallsNothingTheSecondTime() = runTest {
        seeder.seedIfNeeded()
        val idsAfterFirst = ids.issued()
        val templatesAfterFirst = dao.getAllTemplates()

        val second = seeder.seedIfNeeded()

        assertThat(second).isEmpty()
        assertThat(dao.getAllTemplates()).hasSize(templatesAfterFirst.size)
        // Not one ID burned: the second pass must not build rows it then discards.
        assertThat(ids.issued()).isEqualTo(idsAfterFirst)
    }

    @Test
    fun seedingIsIdempotentAcrossManyRuns() = runTest {
        repeat(5) { seeder.seedIfNeeded() }

        assertThat(dao.getAllTemplates()).hasSize(BUILT_IN_TEMPLATES.size)
        val diet = dao.findTemplateByName("Diet", CreatedBy.SYSTEM)!!
        assertThat(dao.getSubMenus(diet.templateId).map { it.name })
            .containsExactly("Breakfast", "Lunch", "Snacks", "Dinner").inOrder()
    }

    @Test
    fun anArchivedBuiltInIsNotResurrected() = runTest {
        seeder.seedIfNeeded()
        val diet = dao.findTemplateByName("Diet", CreatedBy.SYSTEM)!!
        dao.setArchived(diet.templateId, true)

        val second = seeder.seedIfNeeded()

        // The user archived it deliberately; re-seeding must respect that.
        assertThat(second).isEmpty()
        assertThat(dao.getTemplate(diet.templateId)!!.isArchived).isTrue()
        assertThat(dao.observeActiveTemplates().first().map { it.name })
            .containsExactly("Workout", "Study", "Sleep").inOrder()
    }

    @Test
    fun aMissingBuiltInIsInstalledWithoutDuplicatingTheOthers() = runTest {
        seeder.seedIfNeeded()
        val sleep = dao.findTemplateByName("Sleep", CreatedBy.SYSTEM)!!
        db.openHelper.writableDatabase.execSQL(
            "DELETE FROM activity_templates WHERE template_id = '${sleep.templateId}'",
        )

        // Simulates a built-in added in a later release: install the absentee, touch nothing else.
        val second = seeder.seedIfNeeded()

        assertThat(second).containsExactly("Sleep")
        assertThat(dao.getAllTemplates()).hasSize(BUILT_IN_TEMPLATES.size)
    }

    @Test
    fun dietMirrorsTheLunchScreenDesign() = runTest {
        seeder.seedIfNeeded()
        val diet = dao.findTemplateByName("Diet", CreatedBy.SYSTEM)!!
        val lunch = dao.getSubMenus(diet.templateId).single { it.name == "Lunch" }

        val items = dao.getItems(lunch.subMenuId)

        assertThat(items.map { it.name })
            .containsExactly("Paneer", "Dal", "Rice", "Roti", "Curd").inOrder()
        // Every food follows the food -> brand -> product pattern.
        assertThat(items.map { it.hasVariants }.toSet()).containsExactly(true)
        assertThat(items.map { it.variantSource }.toSet()).containsExactly(VariantSource.USER_LIBRARY)
    }

    @Test
    fun foodItemsCarryAVariantRefAndAGramsField() = runTest {
        seeder.seedIfNeeded()
        val diet = dao.findTemplateByName("Diet", CreatedBy.SYSTEM)!!
        val lunch = dao.getSubMenus(diet.templateId).single { it.name == "Lunch" }
        val paneer = dao.getItems(lunch.subMenuId).single { it.name == "Paneer" }

        val fields = dao.getFields(paneer.itemId)

        assertThat(fields.map { it.fieldKey }).containsExactly("variant", "amount").inOrder()
        assertThat(FieldType.fromWire(fields[0].type)).isEqualTo(FieldType.ITEM_VARIANT)
        assertThat(FieldType.fromWire(fields[1].type)).isEqualTo(FieldType.QUANTITY)
        assertThat(fields[1].unit).isEqualTo("g")
        assertThat(fields[1].optionsJson).isEqualTo("""{"min":0,"step":10,"default":100}""")
    }

    @Test
    fun everySeededFieldTypeIsInTheClosedVocabulary() = runTest {
        seeder.seedIfNeeded()

        val allFields = dao.getAllTemplates().flatMap { t ->
            dao.getSubMenus(t.templateId).flatMap { s ->
                dao.getItems(s.subMenuId).flatMap { i -> dao.getFields(i.itemId) }
            }
        }

        assertThat(allFields).isNotEmpty()
        // A built-in that seeded an unrecognised type would render as "unsupported".
        allFields.forEach { field ->
            assertThat(FieldType.fromWire(field.type)).isNotNull()
        }
    }

    @Test
    fun workoutIsABodyPartSplitWithAStretchSection() = runTest {
        seeder.seedIfNeeded()

        val workout = dao.findTemplateByName("Workout", CreatedBy.SYSTEM)!!
        assertThat(dao.getSubMenus(workout.templateId).map { it.name })
            .containsExactly("Chest", "Triceps", "Back", "Biceps", "Shoulders", "Legs", "Stretch")
            .inOrder()

        // Exercises still log as reps × weight sets, under their body part.
        val chest = dao.getSubMenus(workout.templateId).single { it.name == "Chest" }
        val bench = dao.getItems(chest.subMenuId).single { it.name == "Bench Press" }
        assertThat(dao.getFields(bench.itemId).map { FieldType.fromWire(it.type) })
            .containsExactly(FieldType.SET_GROUP, FieldType.NOTE).inOrder()

        // Stretches are a simple done tick.
        val stretch = dao.getSubMenus(workout.templateId).single { it.name == "Stretch" }
        val hamstring = dao.getItems(stretch.subMenuId).single { it.name == "Hamstring Stretch" }
        assertThat(dao.getFields(hamstring.itemId).map { FieldType.fromWire(it.type) })
            .containsExactly(FieldType.CHECKBOX).inOrder()
    }

    @Test
    fun sleepUsesTimeAndScale() = runTest {
        seeder.seedIfNeeded()

        val sleep = dao.findTemplateByName("Sleep", CreatedBy.SYSTEM)!!
        val bedtime = dao.getSubMenus(sleep.templateId).single { it.name == "Bedtime" }
        val bed = dao.getItems(bedtime.subMenuId).single { it.name == "Bedtime" }
        assertThat(dao.getFields(bed.itemId).map { FieldType.fromWire(it.type) })
            .containsExactly(FieldType.TIME, FieldType.SCALE).inOrder()
    }

    @Test
    fun a_built_in_whose_version_bumped_is_rebuilt_and_its_logs_cleared() = runTest {
        seeder.seedIfNeeded()
        val workout = dao.findTemplateByName("Workout", CreatedBy.SYSTEM)!!
        val chest = dao.getSubMenus(workout.templateId).single { it.name == "Chest" }
        val bench = dao.getItems(chest.subMenuId).single { it.name == "Bench Press" }
        // A logged set under the current structure.
        db.logDao().insertLog(
            dev.rishabh.dailytracker.core.db.entity.LogEntryEntity(
                entryId = "e1", templateId = workout.templateId, subMenuId = chest.subMenuId,
                itemId = bench.itemId, loggedAt = 1L, localDate = "2026-07-17",
            ),
            listOf(
                dev.rishabh.dailytracker.core.db.entity.LogValueEntity(
                    valueId = "v1", entryId = "e1", fieldKey = "sets", valueJson = "[]",
                ),
            ),
        )

        // Simulate an older stored structure so the next seed rebuilds it.
        db.openHelper.writableDatabase.execSQL(
            "UPDATE activity_templates SET schema_version = 1 WHERE template_id = '${workout.templateId}'",
        )
        val touched = seeder.seedIfNeeded()

        assertThat(touched).containsExactly("Workout")
        // Same template id, refreshed structure, old log gone.
        assertThat(dao.findTemplateByName("Workout", CreatedBy.SYSTEM)!!.templateId)
            .isEqualTo(workout.templateId)
        assertThat(db.logDao().getEntry("e1")).isNull()
        assertThat(dao.getSubMenus(workout.templateId).map { it.name })
            .contains("Stretch")
    }

    @Test
    fun napsAreOrdinaryLogEntriesUnderSleepRatherThanASpecialTable() = runTest {
        seeder.seedIfNeeded()
        val sleep = dao.findTemplateByName("Sleep", CreatedBy.SYSTEM)!!

        val nap = dao.getSubMenus(sleep.templateId).single { it.name == "Nap" }
        val napItem = dao.getItems(nap.subMenuId).single()

        assertThat(FieldType.fromWire(dao.getFields(napItem.itemId).single().type))
            .isEqualTo(FieldType.DURATION)
    }

    @Test
    fun everyIdIsAppGenerated() = runTest {
        seeder.seedIfNeeded()

        val diet = dao.findTemplateByName("Diet", CreatedBy.SYSTEM)!!

        // The specs carry no IDs at all, so every key here came from the generator.
        assertThat(diet.templateId).startsWith("id-")
        assertThat(dao.getSubMenus(diet.templateId).map { it.subMenuId }).isNotEmpty()
        dao.getSubMenus(diet.templateId).forEach { assertThat(it.subMenuId).startsWith("id-") }
    }

    @Test
    fun seededRowsCarryTheInjectedClock() = runTest {
        time.now = 999L

        seeder.seedIfNeeded()

        assertThat(dao.getAllTemplates().map { it.createdAt }.toSet()).containsExactly(999L)
    }

    @Test
    fun schedulesStayAsJsonRatherThanNormalisedColumns() = runTest {
        seeder.seedIfNeeded()
        val diet = dao.findTemplateByName("Diet", CreatedBy.SYSTEM)!!

        val lunch = dao.getSubMenus(diet.templateId).single { it.name == "Lunch" }

        assertThat(lunch.scheduleJson).isEqualTo("""{"type":"daily"}""")
    }
}
