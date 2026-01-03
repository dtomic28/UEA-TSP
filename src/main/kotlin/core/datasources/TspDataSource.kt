package com.dtomic.core.datasources

import java.io.InputStream

interface TspDataSource {
    fun open(path: String): InputStream?
}