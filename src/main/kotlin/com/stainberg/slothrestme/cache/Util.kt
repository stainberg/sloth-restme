package com.stainberg.slothrestme.cache

import java.io.*
import java.nio.charset.Charset


internal object Util {
    internal val US_ASCII = Charset.forName("US-ASCII")
    internal val UTF_8 = Charset.forName("UTF-8")

    @Throws(IOException::class)
    fun readFully(reader: Reader): String {
        reader.use {
            val writer = StringWriter()
            val buffer = CharArray(1024)
            var count = it.read(buffer)
            while (count != -1) {
                writer.write(buffer, 0, count)
                count = it.read(buffer)
            }
            return writer.toString()
        }
    }

    /**
     * Deletes the contents of `dir`. Throws an IOException if any file
     * could not be deleted, or if `dir` is not a readable directory.
     */
    @Throws(IOException::class)
    fun deleteContents(dir: File) {
        val files = dir.listFiles() ?: throw IOException("not a readable directory: $dir")
        for (file in files) {
            if (file.isDirectory) {
                deleteContents(file)
            }
            if (!file.delete()) {
                throw IOException("failed to delete file: $file")
            }
        }
    }

    fun closeQuietly(closeable: Closeable?) {
        closeable?. let {
            try {
                closeable.close()
            } catch (rethrown: RuntimeException) {
                throw rethrown
            } catch (ignored: Exception) {

            }
        }
    }
}