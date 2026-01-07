package runner

import com.dtomic.algorithms.GA
import com.dtomic.core.TSP
import com.dtomic.core.TSPParser
import com.dtomic.core.datasources.FileSystemTspDataSource
import com.dtomic.util.MultithreadedRandomUtils
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

object ExperimentRunner {

    @JvmStatic
    fun main(args: Array<String>) {

        // Initialize RNG (reproducible if you pass a constant seed)
        MultithreadedRandomUtils.init()
        // MultithreadedRandomUtils.init(123456L)  // for reproducible experiments

        val instances = listOf(
            "bays29.tsp",
            "eil101.tsp",
            "a280.tsp",
            "pr1002.tsp",
            "dca1389.tsp"
        )

        val repeats = 30
        val popSize = 100
        val cr = 0.8
        val pm = 0.1

        // ----- Resolve project root safely -----
        var projectRoot = File(".").absoluteFile
        while (!File(projectRoot, "input").exists()) {
            projectRoot = projectRoot.parentFile
                ?: error("Cannot locate project root (input folder not found)")
        }

        val inputDir = File(projectRoot, "input")
        val resultsDir = File(projectRoot, "results")
        resultsDir.mkdirs()

        println("Project root: $projectRoot")
        println("Input dir: $inputDir")
        println("Results dir: $resultsDir")
        println("Seed used: ${MultithreadedRandomUtils.getSeed()}")

        val parser = TSPParser(FileSystemTspDataSource())
        val cpuCount = Runtime.getRuntime().availableProcessors()
        val pool = Executors.newFixedThreadPool(cpuCount)

        for (inst in instances) {
            val inputPath = File(inputDir, inst).absolutePath
            println("\nProcessing: $inputPath")

            val data = parser.parse(inputPath)
            val maxFes = 1000 * data.dimension

            val tasks = (0 until repeats).map { runIndex ->
                Callable {
                    MultithreadedRandomUtils.initForWorker(runIndex)

                    val tsp = TSP(data, maxFes)
                    val ga = GA(popSize, cr, pm)
                    ga.execute(tsp).distance.toFloat()
                }
            }

            val futures = pool.invokeAll(tasks)

            val bests = FloatArray(repeats)
            for (i in futures.indices) {
                bests[i] = futures[i].get()
                println("  Run ${i + 1}/$repeats completed")
            }

            val problem = inst.substringBefore(".")
            val outFile = File(resultsDir, "Perkmandlc_${problem}.txt")

            outFile.printWriter().use { pw ->
                for (v in bests) pw.println(v)
            }

            println("Saved: ${outFile.absolutePath}")
        }

        pool.shutdown()
        println("\nAll experiments finished.")
    }
}