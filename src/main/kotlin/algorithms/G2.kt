package com.dtomic.algorithms

class G2 {
}

/*
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
                    val (c1, c2) = pmx(p1, p2, problem.dimension(), guard, offspring.size)
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
        parent1: TSP.Tour,
        parent2: TSP.Tour,
        dimension: Int,
        gen: Int,
        offspringSize: Int
    ): Pair<TSP.Tour, TSP.Tour> {

        val p1 = parent1.order
        val p2 = parent2.order
        val n = p1.size // = dimension - 1

        val child1 = IntArray(n) { -1 }
        val child2 = IntArray(n) { -1 }

        // Random crossover segment
        var cut1 = MultithreadedRandomUtils.nextInt(n)
        var cut2 = MultithreadedRandomUtils.nextInt(n)
        if (cut1 > cut2) {
            val t = cut1; cut1 = cut2; cut2 = t
        }
        if (cut1 == cut2) cut2 = minOf(n - 1, cut1 + 1)

        if (DEBUG && gen == 1 && offspringSize < 2) {
            println("[PMX] gen=$gen cut1=$cut1 cut2=$cut2 n=$n dim=$dimension")
        }

        // Mapping tables for conflict resolution
        val map12 = IntArray(dimension) { -1 }
        val map21 = IntArray(dimension) { -1 }

        // Track used cities
        val inChild1 = BooleanArray(dimension)
        val inChild2 = BooleanArray(dimension)

        // Copy crossover segment + build mapping
        for (i in cut1..cut2) {
            val a = p1[i]
            val b = p2[i]

            child1[i] = a
            child2[i] = b

            // Only record mapping if it actually maps to a different value.
            // Otherwise we'd create self-loops like map[x]=x which can infinite-loop resolve().
            if (a != b) {
                map12[a] = b
                map21[b] = a
            }

            inChild1[a] = true
            inChild2[b] = true
        }

        // Resolve mapping conflicts
        fun resolve(map: IntArray, v: Int): Int {
            var x = v
            var steps = 0
            while (true) {
                val next = map[x]
                if (next == -1) return x

                // Self-loop guard (map[x] == x) -> treat as "no mapping"
                if (next == x) return x

                // Path compression
                val next2 = map[next]
                if (next2 != -1 && next2 != next) map[x] = next2

                x = next

                steps++
                if (steps > dimension + 5) {
                    error("[PMX] resolve() seems stuck: start=$v current=$x steps=$steps gen=$gen cut1=$cut1 cut2=$cut2")
                }
            }
        }

        // Fill remaining positions
        for (i in 0 until n) {
            if (i in cut1..cut2) continue

            var g1 = p2[i]
            if (inChild1[g1]) g1 = resolve(map21, g1)
            child1[i] = g1
            inChild1[g1] = true

            var g2 = p1[i]
            if (inChild2[g2]) g2 = resolve(map12, g2)
            child2[i] = g2
            inChild2[g2] = true
        }

        // Quick sanity: no -1 left
        if (DEBUG && gen == 1 && offspringSize < 2) {
            val missing1 = child1.count { it == -1 }
            val missing2 = child2.count { it == -1 }
            if (missing1 != 0 || missing2 != 0) {
                error("[PMX] child contains -1 values! missing1=$missing1 missing2=$missing2 gen=$gen cut1=$cut1 cut2=$cut2")
            }
        }

        return TSP.Tour(child1) to TSP.Tour(child2)
    }
}
*/