package com.dtomic.core

import com.dtomic.core.datasources.TspDataSource

/**
 * Parser for TSPLIB `.tsp` files.
 * Supports EUC_2D coordinate format and EXPLICIT weight matrices.
 */
class TSPParser(private val source: TspDataSource) {

    // Supported distance types
    enum class EdgeWeightType { EUC_2D, EXPLICIT }

    // Supported formats for EXPLICIT matrices
    enum class EdgeWeightFormat { FULL_MATRIX, UPPER_ROW, LOWER_ROW, UPPER_DIAG_ROW, LOWER_DIAG_ROW }

    /**
     * Parses a TSP file and constructs a TSPData object.
     */
    fun parse(path: String): TSPData {

        // Open file from given data source
        val input = source.open(path) ?: error("TSP file not found: $path")

        // Read entire file into memory
        val lines = input.bufferedReader().use { it.readLines() }

        // Read basic problem metadata
        val name = headerValue(lines, "NAME") ?: path
        val dim = (headerValue(lines, "DIMENSION") ?: error("DIMENSION missing")).trim().toInt()

        // Determine edge weight type (distance representation)
        val ewTypeStr = (headerValue(lines, "EDGE_WEIGHT_TYPE") ?: "EUC_2D").trim()
        val ewType = when (ewTypeStr) {
            "EUC_2D" -> EdgeWeightType.EUC_2D
            "EXPLICIT" -> EdgeWeightType.EXPLICIT
            else -> error("Unsupported EDGE_WEIGHT_TYPE=$ewTypeStr")
        }

        // Determine matrix storage format (only used for EXPLICIT)
        val ewFormatStr = (headerValue(lines, "EDGE_WEIGHT_FORMAT") ?: "FULL_MATRIX").trim()
        val ewFormat = when (ewFormatStr) {
            "FULL_MATRIX" -> EdgeWeightFormat.FULL_MATRIX
            "UPPER_ROW" -> EdgeWeightFormat.UPPER_ROW
            "LOWER_ROW" -> EdgeWeightFormat.LOWER_ROW
            "UPPER_DIAG_ROW" -> EdgeWeightFormat.UPPER_DIAG_ROW
            "LOWER_DIAG_ROW" -> EdgeWeightFormat.LOWER_DIAG_ROW
            else -> error("Unsupported EDGE_WEIGHT_FORMAT=$ewFormatStr")
        }

        // Dispatch to correct parser
        return when (ewType) {
            EdgeWeightType.EUC_2D -> parseEuc2D(name, dim, lines)
            EdgeWeightType.EXPLICIT -> parseExplicit(name, dim, ewFormat, lines)
        }
    }

    /**
     * Parses coordinate-based problems (EUC_2D).
     */
    private fun parseEuc2D(name: String, dim: Int, lines: List<String>): TSPData {

        // Locate coordinate section
        val idx = findLineIndex(lines, "NODE_COORD_SECTION")
            ?: error("NODE_COORD_SECTION missing")

        val coords = Array(dim) { TSPData.Coord(0.0, 0.0) }
        var count = 0

        // Read coordinates
        var i = idx + 1
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line == "EOF" || line.endsWith("_SECTION") || line.isEmpty()) break

            val parts = line.split(Regex("\\s+"))
            if (parts.size >= 3) {
                val id = parts[0].toInt() - 1   // TSPLIB is 1-indexed
                val x = parts[1].toDouble()
                val y = parts[2].toDouble()
                coords[id] = TSPData.Coord(x, y)
                count++
            }
            i++
        }

        require(count == dim) { "Expected $dim coords, got $count" }

        return TSPData(name, dim, coords, null)
    }

    /**
     * Parses explicit distance matrices.
     */
    private fun parseExplicit(
        name: String,
        dim: Int,
        format: EdgeWeightFormat,
        lines: List<String>
    ): TSPData {

        val idx = findLineIndex(lines, "EDGE_WEIGHT_SECTION")
            ?: error("EDGE_WEIGHT_SECTION missing")

        // Read all matrix values
        val values = readAllIntsAfter(lines, idx + 1)

        val w = IntArray(dim * dim)

        when (format) {

            // Complete matrix stored row-by-row
            EdgeWeightFormat.FULL_MATRIX -> {
                var k = 0
                for (r in 0 until dim)
                    for (c in 0 until dim)
                        w[r * dim + c] = values[k++]
            }

            // Upper triangle (without diagonal)
            EdgeWeightFormat.UPPER_ROW -> {
                var k = 0
                for (r in 0 until dim) w[r * dim + r] = 0
                for (r in 0 until dim)
                    for (c in r + 1 until dim) {
                        val v = values[k++]
                        w[r * dim + c] = v
                        w[c * dim + r] = v
                    }
            }

            // Lower triangle (without diagonal)
            EdgeWeightFormat.LOWER_ROW -> {
                var k = 0
                for (r in 0 until dim) w[r * dim + r] = 0
                for (r in 0 until dim)
                    for (c in 0 until r) {
                        val v = values[k++]
                        w[r * dim + c] = v
                        w[c * dim + r] = v
                    }
            }

            // Upper triangle including diagonal
            EdgeWeightFormat.UPPER_DIAG_ROW -> {
                var k = 0
                for (r in 0 until dim)
                    for (c in r until dim) {
                        val v = values[k++]
                        w[r * dim + c] = v
                        w[c * dim + r] = v
                    }
            }

            // Lower triangle including diagonal
            EdgeWeightFormat.LOWER_DIAG_ROW -> {
                var k = 0
                for (r in 0 until dim)
                    for (c in 0..r) {
                        val v = values[k++]
                        w[r * dim + c] = v
                        w[c * dim + r] = v
                    }
            }
        }

        return TSPData(name, dim, null, w)
    }

    /** Extracts a header value by key */
    private fun headerValue(lines: List<String>, key: String): String? {
        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith(key)) {
                val parts = line.split(":")
                if (parts.size >= 2) return parts[1].trim()
            }
        }
        return null
    }

    /** Finds the index of a section header */
    private fun findLineIndex(lines: List<String>, exact: String): Int? {
        for (i in lines.indices)
            if (lines[i].trim() == exact) return i
        return null
    }

    /** Reads all integers from file starting at a given line */
    private fun readAllIntsAfter(lines: List<String>, startIndex: Int): List<Int> {
        val out = ArrayList<Int>()
        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            if (line == "EOF" || line.endsWith("_SECTION")) break
            if (line.isEmpty()) continue
            line.split(Regex("\\s+")).forEach { tok ->
                if (tok.isNotEmpty()) out.add(tok.toInt())
            }
        }
        return out
    }
}
