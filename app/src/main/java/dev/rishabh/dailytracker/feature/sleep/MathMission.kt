package dev.rishabh.dailytracker.feature.sleep

import kotlin.random.Random

/** A single wake-up maths problem: the prompt to show and the answer that dismisses it. */
data class MathProblem(val question: String, val answer: Int)

/**
 * The dismiss mission that is always available.
 *
 * "An alarm that can never be silenced is a bug" — so whatever fancier mission is set, a
 * solvable maths problem is the guaranteed way out. Two-digit × one-digit is enough thought
 * to interrupt a half-asleep dismiss without being cruel at 6am; [Random] is injected so the
 * problem is deterministic under test.
 */
object MathMission {
    fun next(random: Random = Random.Default): MathProblem {
        val a = random.nextInt(6, 13)   // 6..12
        val b = random.nextInt(3, 10)   // 3..9
        return MathProblem(question = "$a × $b", answer = a * b)
    }
}
