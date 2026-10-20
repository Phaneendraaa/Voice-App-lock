package com.phani.voicelock.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseMatcherTest {
    @Test
    fun exactPhraseMatches() {
        assertTrue(PhraseMatcher.matches("open sesame", "open sesame"))
    }

    @Test
    fun caseAndPunctuationAreIgnored() {
        assertTrue(PhraseMatcher.matches("open sesame", "Open, Sesame!"))
    }

    @Test
    fun digitsMustBeExact() {
        assertTrue(PhraseMatcher.matches("open sesame four seven", "open sesame four seven"))
        assertFalse(PhraseMatcher.matches("open sesame four seven", "open sesame four six"))
    }

    @Test
    fun digitHomophonesAreAccepted() {
        assertTrue(PhraseMatcher.matches("open sesame four two", "open sesame for to"))
    }

    @Test
    fun missingDigitsFail() {
        assertFalse(PhraseMatcher.matches("open sesame four seven", "open sesame"))
    }

    @Test
    fun emptyOrNullHeardFails() {
        assertFalse(PhraseMatcher.matches("open sesame", ""))
        assertFalse(PhraseMatcher.matches("open sesame", null))
    }

    @Test
    fun unrelatedSpeechFails() {
        assertFalse(PhraseMatcher.matches("open sesame", "what time is it"))
    }

    @Test
    fun oneWrongPhraseWordIsToleratedInLongerPhrases() {
        assertTrue(PhraseMatcher.matches("blue river runs north", "blue river runs forth"))
    }

    @Test
    fun challengeDisplayAndExpectedAgree() {
        val c = Challenges.create("open sesame", 3)
        val displayDigits = c.display.split(" ").filter { it.length == 1 && it[0].isDigit() }
        assertEquals(3, displayDigits.size)
        assertEquals(5, c.expected.split(" ").size)
    }

    @Test
    fun noChallengeDigitsMeansPlainPhrase() {
        val c = Challenges.create("open sesame", 0)
        assertEquals("open sesame", c.display)
        assertEquals("open sesame", c.expected)
    }
}
