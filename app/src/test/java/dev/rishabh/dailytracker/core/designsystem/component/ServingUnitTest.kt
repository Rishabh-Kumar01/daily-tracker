package dev.rishabh.dailytracker.core.designsystem.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The serving unit is purely an input-conversion layer: it decides how the amount is shown
 * and stepped, but every path still resolves to grams for storage. These are the conversions
 * the QuantitySheet relies on — "2 eggs" must be exactly 100 g of egg.
 */
class ServingUnitTest {

    private val egg = ServingUnit.from("count", "egg", 50.0)!!
    private val katori = ServingUnit.from("household", "katori", 150.0)!!
    private val tsp = ServingUnit.from("household", "tsp", 5.0)!!

    @Test
    fun `count units step by one, household by a half`() {
        assertThat(egg.step).isEqualTo(1.0)
        assertThat(katori.step).isEqualTo(0.5)
    }

    @Test
    fun `grams-logged foods have no unit`() {
        assertThat(ServingUnit.from("grams", null, null)).isNull()
        assertThat(ServingUnit.from("count", "egg", null)).isNull()
        assertThat(ServingUnit.from("count", "egg", 0.0)).isNull()
        assertThat(ServingUnit.from("count", " ", 50.0)).isNull()
    }

    @Test
    fun `two eggs is exactly one hundred grams`() {
        // The acceptance invariant: count in, grams out, no drift.
        val gramsForTwoEggs = 2 * egg.gramsPerUnit
        assertThat(gramsForTwoEggs).isEqualTo(100.0)
        assertThat(egg.isCleanMultiple(100.0)).isTrue()
        assertThat(100.0 / egg.gramsPerUnit).isEqualTo(2.0)
    }

    @Test
    fun `snap rounds a new portion to a whole unit, never below one`() {
        assertThat(egg.snap(50.0)).isEqualTo(50.0)      // 1 egg
        assertThat(egg.snap(140.0)).isEqualTo(150.0)    // 2.8 -> 3 eggs
        assertThat(egg.snap(10.0)).isEqualTo(50.0)      // 0.2 -> floor is one egg, not zero
        assertThat(katori.snap(150.0)).isEqualTo(150.0) // 1 katori
        assertThat(katori.snap(200.0)).isEqualTo(225.0) // 1.33 -> 1.5 katori (half step)
    }

    @Test
    fun `an odd logged amount is not a clean multiple, so an edit opens in grams`() {
        assertThat(egg.isCleanMultiple(73.0)).isFalse()
        assertThat(katori.isCleanMultiple(150.0)).isTrue()
        assertThat(katori.isCleanMultiple(75.0)).isTrue()   // half a katori
    }

    @Test
    fun `plural reads naturally`() {
        assertThat(egg.plural(1.0)).isEqualTo("egg")
        assertThat(egg.plural(2.0)).isEqualTo("eggs")
        assertThat(katori.plural(2.0)).isEqualTo("katoris")
        assertThat(tsp.plural(3.0)).isEqualTo("tsp")
        assertThat(ServingUnit.from("household", "glass", 200.0)!!.plural(2.0)).isEqualTo("glasses")
    }
}
