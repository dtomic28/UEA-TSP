package com.dtomic.visual

import com.dtomic.algorithms.GA
import com.dtomic.core.TSP
import com.dtomic.core.TSPParser
import com.dtomic.core.datasources.ClasspathTspDataSource
import com.dtomic.core.datasources.FileSystemTspDataSource
import com.dtomic.util.RandomUtils

fun main() {

    RandomUtils.setSeed(123);

    val parser = TSPParser(FileSystemTspDataSource())
    val data = parser.parse("input/a280.tsp")

    val panel = TspVisualizer(data)
    VisualizerWindow("TSP Visualization", panel)

    val ga = GA(
        popSize = 100,
        crossoverProb = 0.8,
        mutationProb = 0.1
    )

    val tsp = TSP(data, 1000 * data.dimension)

    ga.execute(tsp) { best ->
        panel.update(best)
        Thread.sleep(15)   // slow down so you can see evolution
    }
}
