package com.phani.voicelock.voice

/** Decides whether what the recogniser heard is close enough to what the user was asked to say. */
object PhraseMatcher {
    private val DIGITS = setOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

    // Words a recogniser commonly writes instead of a spoken digit.
    private val HOMOPHONES = mapOf(
        "for" to "four", "fore" to "four",
        "to" to "two", "too" to "two",
        "won" to "one",
        "ate" to "eight",
        "oh" to "zero",
    )

    fun tokens(text: String): List<String> =
        text.lowercase()
            .replace(Regex("[^a-z ]"), " ")
            .split(' ')
            .filter { it.isNotBlank() }
            .map { HOMOPHONES[it] ?: it }

    /**
     * The random digits at the end of the challenge must be heard exactly (that is what defeats a replay);
     * the rest of the phrase may have up to [maxWordErrorRate] mistakes.
     */
    fun matches(expected: String, heard: String?, maxWordErrorRate: Double = 0.34): Boolean {
        if (heard.isNullOrBlank()) return false
        val e = tokens(expected)
        val h = tokens(heard)
        if (e.isEmpty() || h.isEmpty()) return false

        val digits = e.takeLastWhile { it in DIGITS }
        if (digits.isNotEmpty() && !containsSequence(h, digits)) return false

        return editDistance(e, h).toDouble() / e.size <= maxWordErrorRate
    }

    private fun containsSequence(haystack: List<String>, needle: List<String>): Boolean {
        if (needle.size > haystack.size) return false
        for (start in 0..haystack.size - needle.size) {
            if (haystack.subList(start, start + needle.size) == needle) return true
        }
        return false
    }

    private fun editDistance(a: List<String>, b: List<String>): Int {
        var prev = IntArray(b.size + 1) { it }
        for (i in 1..a.size) {
            val cur = IntArray(b.size + 1)
            cur[0] = i
            for (j in 1..b.size) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[b.size]
    }
}
