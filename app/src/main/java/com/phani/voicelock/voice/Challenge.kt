package com.phani.voicelock.voice

import java.security.SecureRandom

/** What the user must say: [display] is shown on screen, [expected] is what the recogniser should hear. */
data class Challenge(val display: String, val expected: String)

object Challenges {
    private val digitWords = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    private val random = SecureRandom()

    fun create(phrase: String, digits: Int): Challenge {
        if (digits <= 0) return Challenge(phrase, phrase)
        val ds = List(digits) { random.nextInt(10) }
        return Challenge(
            display = "$phrase  ${ds.joinToString(" ")}",
            expected = "$phrase ${ds.joinToString(" ") { digitWords[it] }}",
        )
    }
}
