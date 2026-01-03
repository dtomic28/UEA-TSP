package com.dtomic.core

import kotlin.math.roundToInt
import kotlin.math.sqrt

class TSPData(
    val name: String,
    val dimension: Int,
    val coords: Array<Coord>?,        // for EUC_2D
    val weights: IntArray?            // for EXPLICIT (flattened n*n)
) {

    data class Coord(val x: Double, val y: Double)

    // Precomputed distance matrix
    private val dist: Array<DoubleArray> = Array(dimension) { DoubleArray(dimension) }

    init {
        println("Init call for TSPData $name")
        if (weights != null) {
            // EXPLICIT: just copy
            for (i in 0 until dimension)
                for (j in 0 until dimension)
                    dist[i][j] = weights[i * dimension + j].toDouble()
        } else {
            // EUC_2D: compute once
            val c = coords ?: error("No coords loaded (expected EUC_2D)")
            for (i in 0 until dimension) {
                for (j in 0 until dimension) {
                    val dx = c[i].x - c[j].x
                    val dy = c[i].y - c[j].y
                    dist[i][j] = sqrt(dx * dx + dy * dy).roundToInt().toDouble()
                }
            }
        }
        println("Finished init")
    }

    fun distance(a: Int, b: Int): Double = dist[a][b]
}
