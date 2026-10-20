package com.phani.voicelock.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorMathTest {
    @Test
    fun identicalVectorsHaveCosineOne() {
        val v = VectorMath.normalize(floatArrayOf(1f, 2f, 3f))
        assertEquals(1f, VectorMath.cosine(v, v), 1e-5f)
    }

    @Test
    fun orthogonalVectorsHaveCosineZero() {
        val a = VectorMath.normalize(floatArrayOf(1f, 0f))
        val b = VectorMath.normalize(floatArrayOf(0f, 1f))
        assertEquals(0f, VectorMath.cosine(a, b), 1e-5f)
    }

    @Test
    fun sizeMismatchIsRejected() {
        assertEquals(-1f, VectorMath.cosine(floatArrayOf(1f), floatArrayOf(1f, 0f)), 0f)
    }

    @Test
    fun averageIsNormalised() {
        val avg = VectorMath.average(
            listOf(VectorMath.normalize(floatArrayOf(1f, 0f)), VectorMath.normalize(floatArrayOf(0f, 1f))),
        )
        assertEquals(1f, VectorMath.cosine(avg, avg), 1e-5f)
        assertTrue(avg[0] > 0f && avg[1] > 0f)
    }
}
