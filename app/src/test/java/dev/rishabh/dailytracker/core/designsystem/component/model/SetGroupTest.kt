package dev.rishabh.dailytracker.core.designsystem.component.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The set_group representation is shared by the editor and the previous-session recall, so
 * its parsing and formatting are pinned here rather than trusted to two call sites.
 */
class SetGroupTest {

    @Test
    fun parses_the_stored_json_shape() {
        val rows = parseSetRows("""[{"reps":8,"weight":60.0},{"reps":6,"weight":62.5}]""")
        assertThat(rows).containsExactly(SetRow(8, 60.0), SetRow(6, 62.5)).inOrder()
    }

    @Test
    fun null_blank_and_malformed_json_yield_no_rows() {
        assertThat(parseSetRows(null)).isEmpty()
        assertThat(parseSetRows("")).isEmpty()
        assertThat(parseSetRows("not json")).isEmpty()
        assertThat(parseSetRows("{}")).isEmpty()
    }

    @Test
    fun encode_then_parse_round_trips() {
        val rows = listOf(SetRow(10, 40.0), SetRow(8, 45.0))
        assertThat(parseSetRows(encodeSetRows(rows))).isEqualTo(rows)
    }

    @Test
    fun recall_collapses_a_uniform_session() {
        assertThat(formatSetsRecall(listOf(SetRow(8, 60.0), SetRow(8, 60.0), SetRow(8, 60.0))))
            .isEqualTo("3 × 8 @ 60 kg")
    }

    @Test
    fun recall_lists_a_varied_session() {
        assertThat(formatSetsRecall(listOf(SetRow(8, 60.0), SetRow(8, 60.0), SetRow(6, 62.5))))
            .isEqualTo("8@60 · 8@60 · 6@62.5 kg")
    }

    @Test
    fun recall_is_null_without_sets() {
        assertThat(formatSetsRecall(emptyList())).isNull()
    }

    @Test
    fun weight_drops_a_needless_decimal() {
        assertThat(formatWeight(60.0)).isEqualTo("60")
        assertThat(formatWeight(62.5)).isEqualTo("62.5")
    }
}
