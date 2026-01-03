package com.dtomic.util

import java.util.Random

object RandomUtils {
    private var seed: Long = 123L
    private val rng = Random(seed)

    fun setSeed(newSeed: Long) {
        seed = newSeed
        rng.setSeed(seed)
    }

    fun setSeedFromTime() {
        setSeed(System.currentTimeMillis())
    }

    fun getSeed(): Long = seed

    fun nextDouble(): Double = rng.nextDouble()

    fun nextInt(boundExclusive: Int): Int = rng.nextInt(boundExclusive)
}