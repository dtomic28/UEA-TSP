package com.dtomic.util

import java.util.Random

object MultithreadedRandomUtils {

    private var masterSeed: Long = System.currentTimeMillis()

    private val localRng = ThreadLocal<Random>()

    fun init(seed: Long = System.currentTimeMillis()) {
        masterSeed = seed
        localRng.remove()
    }

    fun getSeed(): Long = masterSeed

    private fun rng(): Random {
        var r = localRng.get()
        if (r == null) {
            val threadId = Thread.currentThread().id
            val s = masterSeed xor (threadId shl 21) xor (threadId shl 35)
            r = Random(s)
            localRng.set(r)
        }
        return r
    }

    fun nextInt(bound: Int): Int = rng().nextInt(bound)

    fun nextDouble(): Double = rng().nextDouble()
}
