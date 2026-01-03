package com.dtomic.core.datasources

import java.io.File
import java.io.InputStream

class FileSystemTspDataSource : TspDataSource {
    override fun open(path: String): InputStream? {
        val f = File(path)
        return if (f.exists()) f.inputStream() else null
    }
}
