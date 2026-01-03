package com.dtomic.visual

import com.dtomic.core.TSP
import com.dtomic.core.TSPData
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class TspVisualizer(private val data: TSPData) : JPanel() {

    @Volatile
    private var bestTour: TSP.Tour? = null

    fun update(tour: TSP.Tour) {
        bestTour = tour.cloneTour()
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val coords = data.coords ?: return
        val tour = bestTour ?: return

        val w = width - 40
        val h = height - 40

        val minX = coords.minOf { it.x }
        val maxX = coords.maxOf { it.x }
        val minY = coords.minOf { it.y }
        val maxY = coords.maxOf { it.y }

        fun sx(x: Double) = ((x - minX) / (maxX - minX) * w + 20).toInt()
        fun sy(y: Double) = ((y - minY) / (maxY - minY) * h + 20).toInt()

        g.color = Color(30, 30, 30)

        val order = tour.order
        for (i in order.indices) {
            val a = coords[order[i]]
            val b = coords[order[(i + 1) % order.size]]
            g.drawLine(sx(a.x), sy(a.y), sx(b.x), sy(b.y))
        }

        g.color = Color.RED
        for (c in coords) {
            g.fillOval(sx(c.x) - 4, sy(c.y) - 4, 8, 8)
        }

        println("Tour length: ${order.size}")
        println("Unique cities: ${order.toSet().size}")
    }
}
