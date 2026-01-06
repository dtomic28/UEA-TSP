package com.dtomic.algorithms

import com.dtomic.core.TSP
import com.dtomic.util.MultithreadedRandomUtils

class GA(
    private val popSize: Int,
    private val crossoverProb: Double,
    private val mutationProb: Double
) {

    private val DEBUG = false

    fun execute(problem: TSP, onNewBest: ((TSP.Tour) -> Unit)? = null): TSP.Tour
    {
        val population = ArrayList<TSP.Tour>(popSize)
        val offspring = ArrayList<TSP.Tour>(popSize)

        var best: TSP.Tour? = null

        if (DEBUG) {
            println("[GA] START execute() popSize=$popSize cr=$crossoverProb pm=$mutationProb")
            println("[GA] maxEvals=${problem.getMaxEvaluations()} dimension=${problem.dimension()}")
        }

        // init
        if (DEBUG) println("[GA] init population...")
        repeat(popSize) { idx ->
            val t = problem.generateTour()
            problem.evaluate(t)
            population.add(t)
            if (best == null || t.distance < best!!.distance) best = t.cloneTour()

            if (DEBUG && (idx == 0 || idx == popSize - 1)) {
                println("[GA] init idx=$idx evals=${problem.getNumberOfEvaluations()} best=${best!!.distance}")
            }
        }

        var guard = 0
        while (problem.getNumberOfEvaluations() < problem.getMaxEvaluations()) {
            guard++

            if (DEBUG && (guard == 1 || guard % 10 == 0)) {
                println("[GA] GEN=$guard evals=${problem.getNumberOfEvaluations()} best=${best!!.distance}")
            }

            offspring.clear()

            // elitism keep best individual from prev generation
            if (DEBUG && guard == 1) println("[GA] elitism...")
            val elite = population.minBy { it.distance }.cloneTour()
            offspring.add(elite)

            // build offspring
            if (DEBUG && guard == 1) println("[GA] building offspring...")
            var innerGuard = 0
            while (offspring.size < popSize) {
                innerGuard++
                if (DEBUG && innerGuard % 5000 == 0) {
                    println("[GA]  offspringLoop innerGuard=$innerGuard offspringSize=${offspring.size}/${popSize}")
                }

                // Tournament selection (choose 2 parents)
                val p1 = tournament2(population)
                val p2 = tournament2(population)


                val doCross = MultithreadedRandomUtils.nextDouble() < crossoverProb
                if (doCross) {
                    if (DEBUG && guard == 1 && offspring.size < 3) {
                        println("[GA]  crossover at offspringSize=${offspring.size}")
                    }

                    // PMX crossover
                    val (c1, c2) = pmx(p1, p2)
                    offspring.add(c1)
                    if (offspring.size < popSize) offspring.add(c2)

                } else {
                    offspring.add(p1.cloneTour())
                    if (offspring.size < popSize) offspring.add(p2.cloneTour())
                }

                // If we *ever* get stuck here, we will find out.
                if (innerGuard > 5_000_000) {
                    error("[GA] OFFSPRING LOOP STUCK! innerGuard=$innerGuard offspringSize=${offspring.size}/${popSize}")
                }
            }

            // mutation + evaluation
            if (DEBUG && guard == 1) println("[GA] evaluating offspring...")
            for (i in 0 until offspring.size) {
                if (problem.getNumberOfEvaluations() >= problem.getMaxEvaluations()) break

                val t = offspring[i]

                if (MultithreadedRandomUtils.nextDouble() < mutationProb) {
                    swapMutation(t)
                }

                problem.evaluate(t)

                if (best == null || t.distance < best!!.distance) {
                    best = t.cloneTour()
                    onNewBest?.invoke(best!!)
                    if (DEBUG) {
                        println("[GA]  NEW BEST gen=$guard evals=${problem.getNumberOfEvaluations()} best=${best!!.distance}")
                    }
                }

                if (DEBUG && guard == 1 && (i == 0 || i == offspring.size - 1)) {
                    println("[GA]  eval i=$i evals=${problem.getNumberOfEvaluations()} dist=${t.distance}")
                }
            }

            population.clear()
            population.addAll(offspring)

            if (guard > 5_000_000) {
                error("[GA] MAIN LOOP STUCK! guard=$guard evals=${problem.getNumberOfEvaluations()}/${problem.getMaxEvaluations()}")
            }
        }

        if (DEBUG) {
            println("[GA] DONE evals=${problem.getNumberOfEvaluations()} best=${best!!.distance}")
        }

        return best!!
    }

    /**
     * Tournament selection with 2 individuals.
     * Randomly picks two and returns the better one.
     */
    private fun tournament2(pop: List<TSP.Tour>): TSP.Tour {
        val i = MultithreadedRandomUtils.nextInt(pop.size)
        var j = MultithreadedRandomUtils.nextInt(pop.size)

        while (j == i) j = MultithreadedRandomUtils.nextInt(pop.size)
        val a = pop[i]
        val b = pop[j]
        return if (a.distance <= b.distance) a else b
    }

    /**
     * Swap mutation: swaps two cities in the tour.
     */
    private fun swapMutation(t: TSP.Tour) {
        val p = t.order
        if (p.size < 2) return

        val i = MultithreadedRandomUtils.nextInt(p.size)
        var j = MultithreadedRandomUtils.nextInt(p.size)
        while (j == i) j = MultithreadedRandomUtils.nextInt(p.size)

        val tmp = p[i]
        p[i] = p[j]
        p[j] = tmp
    }

    /**
     * PMX + debugging guards
     */
    private fun pmx(
        p1: TSP.Tour,
        p2: TSP.Tour,
    ): Pair<TSP.Tour, TSP.Tour> {

        val a = p1.order
        val b = p2.order
        val n = a.size

        val c1 = IntArray(n) { -1 }
        val c2 = IntArray(n) { -1 }

        var cut1 = MultithreadedRandomUtils.nextInt(n)
        var cut2 = MultithreadedRandomUtils.nextInt(n)
        if (cut1 > cut2) cut1 = cut2.also { cut2 = cut1 }
        if (cut1 == cut2) cut2 = (cut1 + 1).coerceAtMost(n - 1)

        // mapping between swapped segments
        val map12 = HashMap<Int, Int>((cut2 - cut1 + 1) * 2)
        val map21 = HashMap<Int, Int>((cut2 - cut1 + 1) * 2)

        // Copy segment + build mapping
        for (i in cut1..cut2) {
            val x = a[i]
            val y = b[i]
            c1[i] = x
            c2[i] = y
            map12[y] = x
            map21[x] = y
        }

        fun resolve(map: HashMap<Int, Int>, v0: Int): Int {
            var v = v0
            while (true) {
                val next = map[v] ?: return v
                v = next
            }
        }

        // Fill remaining positions
        for (i in 0 until n) {
            if (c1[i] == -1) c1[i] = resolve(map21, b[i])
            if (c2[i] == -1) c2[i] = resolve(map12, a[i])
        }

        return TSP.Tour(c1) to TSP.Tour(c2)
    }

}
