package com.dtomic.core

import com.dtomic.util.MultithreadedRandomUtils


class TSP(
    val data: TSPData,
    private val maxEvaluations: Int
) {

    class Tour(
        // permutation of cities 1..(n-1); start city 0 is implicit and fixed
        val order: IntArray
    ) {
        var distance: Double = Double.POSITIVE_INFINITY
            internal set

        fun cloneTour(): Tour = Tour(order.copyOf()).also { it.distance = distance }
    }

    private var evaluations: Int = 0

    fun dimension(): Int = data.dimension
    fun getNumberOfEvaluations(): Int = evaluations
    fun getMaxEvaluations(): Int = maxEvaluations

    fun generateTour(): Tour {
        val n = data.dimension
        val arr = IntArray(n - 1)
        var k = 0
        for (i in 1 until n) arr[k++] = i

        // Fisher-Yates using RandomUtils
        for (i in arr.lastIndex downTo 1) {
            val j = MultithreadedRandomUtils.nextInt(i + 1)
            val tmp = arr[i]
            arr[i] = arr[j]
            arr[j] = tmp
        }

        return Tour(arr)
    }

    fun evaluate(t: Tour) {
        val n = data.dimension
        val p = t.order
        var d = 0.0

        // start (0) -> first
        d += data.distance(0, p[0])

        // edges
        for (i in 0 until p.size - 1) {
            d += data.distance(p[i], p[i + 1])
        }

        // last -> start
        d += data.distance(p[p.size - 1], 0)

        t.distance = d
        evaluations++
    }
}
