package dev.rishabh.dailytracker.feature.builder

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** field_key is what log_values reference, so its slugging is pinned rather than assumed. */
class BuilderKeyTest {

    @Test
    fun slugs_a_label_to_a_stable_key() {
        assertThat(fieldKey("Amount", emptySet())).isEqualTo("amount")
        assertThat(fieldKey("Felt Good?", emptySet())).isEqualTo("felt_good")
        assertThat(fieldKey("  Water intake (ml) ", emptySet())).isEqualTo("water_intake_ml")
    }

    @Test
    fun de_duplicates_within_an_item() {
        assertThat(fieldKey("Amount", setOf("amount"))).isEqualTo("amount_2")
        assertThat(fieldKey("Amount", setOf("amount", "amount_2"))).isEqualTo("amount_3")
    }

    @Test
    fun a_label_with_no_usable_characters_falls_back() {
        assertThat(fieldKey("!!!", emptySet())).isEqualTo("field")
        assertThat(fieldKey("   ", emptySet())).isEqualTo("field")
    }
}
