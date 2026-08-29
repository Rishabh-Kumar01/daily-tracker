package dev.rishabh.dailytracker.feature.sleep

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

/** The one mission that must never lock the user out, so its arithmetic is pinned. */
class MathMissionTest {

    @Test
    fun the_answer_always_matches_the_shown_problem() {
        repeat(300) {
            val problem = MathMission.next()
            val (a, b) = problem.question.split(" × ").map { it.toInt() }
            assertThat(problem.answer).isEqualTo(a * b)
            assertThat(a).isAtLeast(6)
            assertThat(a).isAtMost(12)
            assertThat(b).isAtLeast(3)
            assertThat(b).isAtMost(9)
        }
    }

    @Test
    fun is_deterministic_under_a_seed() {
        assertThat(MathMission.next(Random(42))).isEqualTo(MathMission.next(Random(42)))
    }
}
