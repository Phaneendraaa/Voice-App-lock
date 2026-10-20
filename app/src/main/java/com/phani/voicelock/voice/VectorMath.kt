package com.phani.voicelock.voice

import kotlin.math.sqrt

/** Small vector helpers for comparing voiceprints. */
object VectorMath {
    fun normalize(v: FloatArray): FloatArray {
        var s = 0.0
        for (x in v) s += x * x
        val n = sqrt(s).toFloat()
        return if (n == 0f) v else FloatArray(v.size) { v[it] / n }
    }

    /** Cosine similarity of two vectors. Both are expected to be L2-normalised already. */
    fun cosine(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || a.size != b.size) return -1f
        var dot = 0.0
        for (i in a.indices) dot += a[i] * b[i]
        return dot.toFloat()
    }

    /** Mean of several embeddings, re-normalised. */
    fun average(all: List<FloatArray>): FloatArray {
        val dim = all.first().size
        val sum = FloatArray(dim)
        for (e in all) for (i in 0 until dim) sum[i] += e[i]
        return normalize(sum)
    }
}
