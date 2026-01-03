package com.dtomic.core.datasources

import java.io.InputStream

class ClasspathTspDataSource : TspDataSource {
    override fun open(path: String): InputStream? =
        this::class.java.classLoader?.getResourceAsStream(path)
}