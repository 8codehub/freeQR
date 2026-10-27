package qrstudio.platform

expect interface ImageSaver { suspend fun save(png: ByteArray,fileName: String) }
