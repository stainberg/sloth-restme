package com.stainberg.slothrestme.cache

import java.io.*
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

internal class DiskLruCache private constructor(private val directory: File, private val appVersion: Int, private val valueCount: Int, private var maxSize: Long) : Closeable {
        /*
     * This cache uses a journal file named "journal". A typical journal file
     * looks like this:
     *     libcore.io.DiskLruCache
     *     1
     *     100
     *     2
     *
     *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
     *     DIRTY 335c4c6028171cfddfbaae1a9c313c52
     *     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
     *     REMOVE 335c4c6028171cfddfbaae1a9c313c52
     *     DIRTY 1ab96a171faeeee38496d8b330771a7a
     *     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
     *     READ 335c4c6028171cfddfbaae1a9c313c52
     *     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
     *
     * The first five lines of the journal form its header. They are the
     * constant string "libcore.io.DiskLruCache", the disk cache's version,
     * the application's version, the value count, and a blank line.
     *
     * Each of the subsequent lines in the file is a record of the state of a
     * cache entry. Each line contains space-separated values: a state, a key,
     * and optional state-specific values.
     *   o DIRTY lines track that an entry is actively being created or updated.
     *     Every successful DIRTY action should be followed by a CLEAN or REMOVE
     *     action. DIRTY lines without a matching CLEAN or REMOVE indicate that
     *     temporary files may need to be deleted.
     *   o CLEAN lines track a cache entry that has been successfully published
     *     and may be read. A publish line is followed by the lengths of each of
     *     its values.
     *   o READ lines track accesses for LRU.
     *   o REMOVE lines track entries that have been deleted.
     *
     * The journal file is appended to as cache operations occur. The journal may
     * occasionally be compacted by dropping redundant lines. A temporary file named
     * "journal.tmp" will be used during compaction; that file should be deleted if
     * it exists when the cache is opened.
     */

        /** Returns the directory where this cache stores its data.  */
    private val journalFile: File
    private val journalFileTmp: File
    private val journalFileBackup: File
    private var size: Long = 0
    private var journalWriter: Writer? = null
    private val lruEntries = LinkedHashMap<String, Entry>(0, 0.75f, true)
    private var redundantOpCount: Int = 0

    /**
     * To differentiate between old and current snapshots, each entry is given
     * a sequence number each time an edit is committed. A snapshot is stale if
     * its sequence number is not equal to its entry's sequence number.
     */
    private var nextSequenceNumber: Long = 0

    /** This cache uses a single background thread to evict entries.  */
    internal val executorService = ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, LinkedBlockingQueue())
    private val cleanupCallable = Callable<Void> {
        synchronized(this@DiskLruCache) {
            if (journalWriter == null) {
                return@Callable null // Closed.
            }
            trimToSize()
            if (journalRebuildRequired()) {
                rebuildJournal()
                redundantOpCount = 0
            }
        }
        null
    }

    /** Returns true if this cache has been closed.  */
    val isClosed: Boolean
        @Synchronized get() = journalWriter == null

    init {
        this.journalFile = File(directory, JOURNAL_FILE)
        this.journalFileTmp = File(directory, JOURNAL_FILE_TEMP)
        this.journalFileBackup = File(directory, JOURNAL_FILE_BACKUP)
    }

    @Throws(IOException::class)
    private fun readJournal() {
        val reader = StrictLineReader(FileInputStream(journalFile), Util.US_ASCII)
        try {
            val magic = reader.readLine()
            val version = reader.readLine()
            val appVersionString = reader.readLine()
            val valueCountString = reader.readLine()
            val blank = reader.readLine()
            if (MAGIC != magic
                    || VERSION_1 != version
                    || Integer.toString(appVersion) != appVersionString
                    || Integer.toString(valueCount) != valueCountString
                    || "" != blank) {
                throw IOException("unexpected journal header: [" + magic + ", " + version + ", "
                        + valueCountString + ", " + blank + "]")
            }

            var lineCount = 0
            while (true) {
                try {
                    readJournalLine(reader.readLine())
                    lineCount++
                } catch (endOfJournal: EOFException) {
                    break
                }

            }
            redundantOpCount = lineCount - lruEntries.size

            // If we ended on a truncated line, rebuild the journal before appending to it.
            if (reader.hasUnterminatedLine()) {
                rebuildJournal()
            } else {
                journalWriter = BufferedWriter(OutputStreamWriter(
                        FileOutputStream(journalFile, true), Util.US_ASCII))
            }
        } finally {
            Util.closeQuietly(reader)
        }
    }

    @Throws(IOException::class)
    private fun readJournalLine(line: String) {
        val firstSpace = line.indexOf(' ')
        if (firstSpace == -1) {
            throw IOException("unexpected journal line: $line")
        }

        val keyBegin = firstSpace + 1
        val secondSpace = line.indexOf(' ', keyBegin)
        val key: String
        if (secondSpace == -1) {
            key = line.substring(keyBegin)
            if (firstSpace == REMOVE.length && line.startsWith(REMOVE)) {
                lruEntries.remove(key)
                return
            }
        } else {
            key = line.substring(keyBegin, secondSpace)
        }

        var entry: Entry? = lruEntries[key]
        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        }

        if (secondSpace != -1 && firstSpace == CLEAN.length && line.startsWith(CLEAN)) {
            val parts = line.substring(secondSpace + 1).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            entry.readable = true
            entry.currentEditor = null
            entry.setLengths(parts)
        } else if (secondSpace == -1 && firstSpace == DIRTY.length && line.startsWith(DIRTY)) {
            entry.currentEditor = Editor(entry)
        } else if (secondSpace == -1 && firstSpace == READ.length && line.startsWith(READ)) {
            // This work was already done by calling lruEntries.get().
        } else {
            throw IOException("unexpected journal line: $line")
        }
    }

    /**
     * Computes the initial size and collects garbage as a part of opening the
     * cache. Dirty entries are assumed to be inconsistent and will be deleted.
     */
    @Throws(IOException::class)
    private fun processJournal() {
        deleteIfExists(journalFileTmp)
        val i = lruEntries.values.iterator()
        while (i.hasNext()) {
            val entry = i.next()
            if (entry.currentEditor == null) {
                for (t in 0 until valueCount) {
                    size += entry.lengths[t]
                }
            } else {
                entry.currentEditor = null
                for (t in 0 until valueCount) {
                    deleteIfExists(entry.getCleanFile(t))
                    deleteIfExists(entry.getDirtyFile(t))
                }
                i.remove()
            }
        }
    }

    /**
     * Creates a new journal that omits redundant information. This replaces the
     * current journal if it exists.
     */
    @Synchronized
    @Throws(IOException::class)
    private fun rebuildJournal() {
        if (journalWriter != null) {
            journalWriter!!.close()
        }

        val writer = BufferedWriter(
                OutputStreamWriter(FileOutputStream(journalFileTmp), Util.US_ASCII))
        try {
            writer.write(MAGIC)
            writer.write("\n")
            writer.write(VERSION_1)
            writer.write("\n")
            writer.write(Integer.toString(appVersion))
            writer.write("\n")
            writer.write(Integer.toString(valueCount))
            writer.write("\n")
            writer.write("\n")

            for (entry in lruEntries.values) {
                if (entry.currentEditor != null) {
                    writer.write(DIRTY + ' '.toString() + entry.key + '\n'.toString())
                } else {
                    writer.write(CLEAN + ' '.toString() + entry.key + entry.getLengths() + '\n'.toString())
                }
            }
        } finally {
            writer.close()
        }

        if (journalFile.exists()) {
            renameTo(journalFile, journalFileBackup, true)
        }
        renameTo(journalFileTmp, journalFile, false)
        journalFileBackup.delete()

        journalWriter = BufferedWriter(
                OutputStreamWriter(FileOutputStream(journalFile, true), Util.US_ASCII))
    }

    /**
     * Returns a snapshot of the entry named `key`, or null if it doesn't
     * exist is not currently readable. If a value is returned, it is moved to
     * the head of the LRU queue.
     */
    @Synchronized
    @Throws(IOException::class)
    operator fun get(key: String): Snapshot? {
        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key] ?: return null

        if (!entry.readable) {
            return null
        }

        // Open all streams eagerly to guarantee that we see a single published
        // snapshot. If we opened streams lazily then the streams could come
        // from different edits.
        val ins = arrayOfNulls<InputStream>(valueCount)
        try {
            for (i in 0 until valueCount) {
                ins[i] = FileInputStream(entry.getCleanFile(i))
            }
        } catch (e: FileNotFoundException) {
            // A file must have been deleted manually!
            for (i in 0 until valueCount) {
                if (ins[i] != null) {
                    Util.closeQuietly(ins[i])
                } else {
                    break
                }
            }
            return null
        }

        redundantOpCount++
        journalWriter!!.append(READ + ' '.toString() + key + '\n'.toString())
        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
        ins.let {
            return Snapshot(key, entry.sequenceNumber, ins, entry.lengths)
        }
    }

    /**
     * Returns an editor for the entry named `key`, or null if another
     * edit is in progress.
     */
    @Throws(IOException::class)
    internal fun edit(key: String): Editor? {
        return edit(key, ANY_SEQUENCE_NUMBER)
    }

    @Synchronized
    @Throws(IOException::class)
    private fun edit(key: String, expectedSequenceNumber: Long): Editor? {
        checkNotClosed()
        validateKey(key)
        var entry: Entry? = lruEntries[key]
        if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && (entry == null || entry.sequenceNumber != expectedSequenceNumber)) {
            return null // Snapshot is stale.
        }
        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        } else if (entry.currentEditor != null) {
            return null // Another edit is in progress.
        }

        val editor = Editor(entry)
        entry.currentEditor = editor

        // Flush the journal before creating files to prevent file leaks.
        journalWriter!!.write(DIRTY + ' '.toString() + key + '\n'.toString())
        journalWriter!!.flush()
        return editor
    }

    /**
     * Returns the maximum number of bytes that this cache should use to store
     * its data.
     */
    @Synchronized
    fun getMaxSize(): Long {
        return maxSize
    }

    /**
     * Changes the maximum number of bytes the cache can store and queues a job
     * to trim the existing store, if necessary.
     */
    @Synchronized
    fun setMaxSize(maxSize: Long) {
        this.maxSize = maxSize
        executorService.submit(cleanupCallable)
    }

    /**
     * Returns the number of bytes currently being used to store the values in
     * this cache. This may be greater than the max size if a background
     * deletion is pending.
     */
    @Synchronized
    fun size(): Long {
        return size
    }

    @Synchronized
    @Throws(IOException::class)
    private fun completeEdit(editor: Editor, success: Boolean) {
        val entry = editor.entry
        if (entry.currentEditor != editor) {
            throw IllegalStateException()
        }

        // If this edit is creating the entry for the first time, every index must have a value.
        if (success && !entry.readable) {
            for (i in 0 until valueCount) {
                if (!editor.written!![i]) {
                    editor.abort()
                    throw IllegalStateException("Newly created entry didn't create value for index $i")
                }
                if (!entry.getDirtyFile(i).exists()) {
                    editor.abort()
                    return
                }
            }
        }

        for (i in 0 until valueCount) {
            val dirty = entry.getDirtyFile(i)
            if (success) {
                if (dirty.exists()) {
                    val clean = entry.getCleanFile(i)
                    dirty.renameTo(clean)
                    val oldLength = entry.lengths[i]
                    val newLength = clean.length()
                    entry.lengths[i] = newLength
                    size = size - oldLength + newLength
                }
            } else {
                deleteIfExists(dirty)
            }
        }

        redundantOpCount++
        entry.currentEditor = null
        if (entry.readable or success) {
            entry.readable = true
            journalWriter!!.write(CLEAN + ' '.toString() + entry.key + entry.getLengths() + '\n'.toString())
            if (success) {
                entry.sequenceNumber = nextSequenceNumber++
            }
        } else {
            lruEntries.remove(entry.key)
            journalWriter!!.write(REMOVE + ' '.toString() + entry.key + '\n'.toString())
        }
        journalWriter!!.flush()

        if (size > maxSize || journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }
    }

    /**
     * We only rebuild the journal when it will halve the size of the journal
     * and eliminate at least 2000 ops.
     */
    private fun journalRebuildRequired(): Boolean {
        val redundantOpCompactThreshold = 2000
        return (redundantOpCount >= redundantOpCompactThreshold //
                && redundantOpCount >= lruEntries.size)
    }

    /**
     * Drops the entry for `key` if it exists and can be removed. Entries
     * actively being edited cannot be removed.
     *
     * @return true if an entry was removed.
     */
    @Synchronized
    @Throws(IOException::class)
    fun remove(key: String): Boolean {
        checkNotClosed()
        validateKey(key)
        val entry = lruEntries[key]
        if (entry == null || entry.currentEditor != null) {
            return false
        }

        for (i in 0 until valueCount) {
            val file = entry.getCleanFile(i)
            if (file.exists() && !file.delete()) {
                throw IOException("failed to delete $file")
            }
            size -= entry.lengths[i]
            entry.lengths[i] = 0
        }

        redundantOpCount++
        journalWriter!!.append(REMOVE + ' '.toString() + key + '\n'.toString())
        lruEntries.remove(key)

        if (journalRebuildRequired()) {
            executorService.submit(cleanupCallable)
        }

        return true
    }

    private fun checkNotClosed() {
        if (journalWriter == null) {
            throw IllegalStateException("cache is closed")
        }
    }

    /** Force buffered operations to the filesystem.  */
    @Synchronized
    @Throws(IOException::class)
    fun flush() {
        checkNotClosed()
        trimToSize()
        journalWriter!!.flush()
    }

    /** Closes this cache. Stored values will remain on the filesystem.  */
    @Synchronized
    @Throws(IOException::class)
    override fun close() {
        if (journalWriter == null) {
            return  // Already closed.
        }
        for (entry in ArrayList(lruEntries.values)) {
            entry.currentEditor?.abort()
        }
        trimToSize()
        journalWriter!!.close()
        journalWriter = null
    }

    @Throws(IOException::class)
    private fun trimToSize() {
        while (size > maxSize) {
            val toEvict = lruEntries.entries.iterator().next()
            remove(toEvict.key)
        }
    }

    /**
     * Closes the cache and deletes all of its stored values. This will delete
     * all files in the cache directory including files that weren't created by
     * the cache.
     */
    @Throws(IOException::class)
    fun delete() {
        close()
        Util.deleteContents(directory)
    }

    private fun validateKey(key: String) {
        val matcher = LEGAL_KEY_PATTERN.matcher(key)
        if (!matcher.matches()) {
            throw IllegalArgumentException("keys must match regex "
                    + STRING_KEY_PATTERN + ": \"" + key + "\"")
        }
    }

    /** A snapshot of the values for an entry.  */
    inner class Snapshot constructor(val key: String, val sequenceNumber: Long, val ins: Array<InputStream?>, val lengths: LongArray) : Closeable {

        /**
         * Returns an editor for this snapshot's entry, or null if either the
         * entry has changed since this snapshot was created or if another edit
         * is in progress.
         */
        @Throws(IOException::class)
        internal fun edit(): Editor? {
            return this@DiskLruCache.edit(key, sequenceNumber)
        }

        /** Returns the unbuffered stream with the value for `index`.  */
        fun getInputStream(index: Int): InputStream? {
            return ins[index]
        }

        /** Returns the string value for `index`.  */
        @Throws(IOException::class)
        fun getString(index: Int): String {
            return inputStreamToString(getInputStream(index))
        }

        /** Returns the byte length of the value for `index`.  */
        fun getLength(index: Int): Long {
            return lengths[index]
        }

        override fun close() {
            for (`in` in ins) {
                Util.closeQuietly(`in`)
            }
        }
    }

    /** Edits the values for an entry.  */
    internal inner class Editor constructor(val entry: Entry) {
        internal val written: BooleanArray?
        internal var hasErrors: Boolean = false
        internal var committed: Boolean = false

        init {
            this.written = if (entry.readable) null else BooleanArray(valueCount)
        }

        /**
         * Returns an unbuffered input stream to read the last committed value,
         * or null if no value has been committed.
         */
        @Throws(IOException::class)
        fun newInputStream(index: Int): InputStream? {
            synchronized(this@DiskLruCache) {
                if (entry.currentEditor != this) {
                    throw IllegalStateException()
                }
                if (!entry.readable) {
                    return null
                }
                try {
                    return FileInputStream(entry.getCleanFile(index))
                } catch (e: FileNotFoundException) {
                    return null
                }

            }
        }

        /**
         * Returns the last committed value as a string, or null if no value
         * has been committed.
         */
        @Throws(IOException::class)
        fun getString(index: Int): String? {
            val `in` = newInputStream(index)
            return if (`in` != null) inputStreamToString(`in`) else null
        }

        /**
         * Returns a new unbuffered output stream to write the value at
         * `index`. If the underlying output stream encounters errors
         * when writing to the filesystem, this edit will be aborted when
         * [.commit] is called. The returned output stream does not throw
         * IOExceptions.
         */
        @Throws(IOException::class)
        fun newOutputStream(index: Int): OutputStream {
            if (index < 0 || index >= valueCount) {
                throw IllegalArgumentException("Expected index " + index + " to "
                        + "be greater than 0 and less than the maximum value count "
                        + "of " + valueCount)
            }
            synchronized(this@DiskLruCache) {
                if (entry.currentEditor != this) {
                    throw IllegalStateException()
                }
                if (!entry.readable) {
                    written?. let {
                        written[index] = true
                    }
                }
                val dirtyFile = entry.getDirtyFile(index)
                var outputStream: FileOutputStream
                try {
                    outputStream = FileOutputStream(dirtyFile)
                } catch (e: FileNotFoundException) {
                    // Attempt to recreate the cache directory.
                    directory.mkdirs()
                    try {
                        outputStream = FileOutputStream(dirtyFile)
                    } catch (e2: FileNotFoundException) {
                        // We are unable to recover. Silently eat the writes.
                        return NULL_OUTPUT_STREAM
                    }

                }

                return FaultHidingOutputStream(outputStream)
            }
        }

        /** Sets the value at `index` to `value`.  */
        @Throws(IOException::class)
        operator fun set(index: Int, value: String) {
            var writer: Writer? = null
            try {
                writer = OutputStreamWriter(newOutputStream(index), Util.UTF_8)
                writer.write(value)
            } finally {
                Util.closeQuietly(writer)
            }
        }

        /**
         * Commits this edit so it is visible to readers.  This releases the
         * edit lock so another edit may be started on the same key.
         */
        @Throws(IOException::class)
        fun commit() {
            if (hasErrors) {
                completeEdit(this, false)
                remove(entry.key) // The previous entry is stale.
            } else {
                completeEdit(this, true)
            }
            committed = true
        }

        /**
         * Aborts this edit. This releases the edit lock so another edit may be
         * started on the same key.
         */
        @Throws(IOException::class)
        fun abort() {
            completeEdit(this, false)
        }

        fun abortUnlessCommitted() {
            if (!committed) {
                try {
                    abort()
                } catch (ignored: IOException) {
                }

            }
        }

        internal inner class FaultHidingOutputStream constructor(out: OutputStream) : FilterOutputStream(out) {

            override fun write(oneByte: Int) {
                try {
                    out.write(oneByte)
                } catch (e: IOException) {
                    hasErrors = true
                }

            }

            override fun write(buffer: ByteArray, offset: Int, length: Int) {
                try {
                    out.write(buffer, offset, length)
                } catch (e: IOException) {
                    hasErrors = true
                }

            }

            override fun close() {
                try {
                    out.close()
                } catch (e: IOException) {
                    hasErrors = true
                }

            }

            override fun flush() {
                try {
                    out.flush()
                } catch (e: IOException) {
                    hasErrors = true
                }

            }
        }
    }

    internal inner class Entry constructor(val key: String) {

        /** Lengths of this entry's files.  */
        internal var lengths: LongArray

        /** True if this entry has ever been published.  */
        internal var readable: Boolean = false

        /** The ongoing edit or null if this entry is not being edited.  */
        internal var currentEditor: Editor? = null

        /** The sequence number of the most recently committed edit to this entry.  */
        internal var sequenceNumber: Long = 0

        init {
            this.lengths = LongArray(valueCount)
        }

        @Throws(IOException::class)
        fun getLengths(): String {
            val result = StringBuilder()
            for (size in lengths) {
                result.append(' ').append(size)
            }
            return result.toString()
        }

        /** Set lengths using decimal numbers like "10123".  */
        @Throws(IOException::class)
        internal fun setLengths(strings: Array<String>) {
            if (strings.size != valueCount) {
                throw invalidLengths(strings)
            }

            try {
                for (i in strings.indices) {
                    lengths[i] = java.lang.Long.parseLong(strings[i])
                }
            } catch (e: NumberFormatException) {
                throw invalidLengths(strings)
            }

        }

        @Throws(IOException::class)
        private fun invalidLengths(strings: Array<String>): IOException {
            throw IOException("unexpected journal line: " + java.util.Arrays.toString(strings))
        }

        fun getCleanFile(i: Int): File {
            return File(directory, "$key.$i")
        }

        fun getDirtyFile(i: Int): File {
            return File(directory, "$key.$i.tmp")
        }
    }

    companion object {
        internal val JOURNAL_FILE = "journal"
        internal val JOURNAL_FILE_TEMP = "journal.tmp"
        internal val JOURNAL_FILE_BACKUP = "journal.bkp"
        internal val MAGIC = "libcore.io.DiskLruCache"
        internal val VERSION_1 = "1"
        internal val ANY_SEQUENCE_NUMBER: Long = -1
        internal val STRING_KEY_PATTERN = "[a-z0-9_-]{1,120}"
        internal val LEGAL_KEY_PATTERN = Pattern.compile(STRING_KEY_PATTERN)
        private val CLEAN = "CLEAN"
        private val DIRTY = "DIRTY"
        private val REMOVE = "REMOVE"
        private val READ = "READ"

        /**
         * Opens the cache in `directory`, creating a cache if none exists
         * there.
         *
         * @param directory a writable directory
         * @param valueCount the number of values per cache entry. Must be positive.
         * @param maxSize the maximum number of bytes this cache should use to store
         * @throws IOException if reading or writing the cache directory fails
         */
        @Throws(IOException::class)
        fun open(directory: File, appVersion: Int, valueCount: Int, maxSize: Long): DiskLruCache {
            if (maxSize <= 0) {
                throw IllegalArgumentException("maxSize <= 0")
            }
            if (valueCount <= 0) {
                throw IllegalArgumentException("valueCount <= 0")
            }

            // If a bkp file exists, use it instead.
            val backupFile = File(directory, JOURNAL_FILE_BACKUP)
            if (backupFile.exists()) {
                val journalFile = File(directory, JOURNAL_FILE)
                // If journal file also exists just delete backup file.
                if (journalFile.exists()) {
                    backupFile.delete()
                } else {
                    renameTo(backupFile, journalFile, false)
                }
            }

            // Prefer to pick up where we left off.
            var cache = DiskLruCache(directory, appVersion, valueCount, maxSize)
            if (cache.journalFile.exists()) {
                try {
                    cache.readJournal()
                    cache.processJournal()
                    return cache
                } catch (journalIsCorrupt: IOException) {
                    println("DiskLruCache "
                            + directory
                            + " is corrupt: "
                            + journalIsCorrupt.message
                            + ", removing")
                    cache.delete()
                }

            }

            // Create a new empty cache.
            directory.mkdirs()
            cache = DiskLruCache(directory, appVersion, valueCount, maxSize)
            cache.rebuildJournal()
            return cache
        }

        @Throws(IOException::class)
        private fun deleteIfExists(file: File) {
            if (file.exists() && !file.delete()) {
                throw IOException()
            }
        }

        @Throws(IOException::class)
        private fun renameTo(from: File, to: File, deleteDestination: Boolean) {
            if (deleteDestination) {
                deleteIfExists(to)
            }
            if (!from.renameTo(to)) {
                throw IOException()
            }
        }

        @Throws(IOException::class)
        private fun inputStreamToString(`in`: InputStream?): String {
            `in`?. let {
                return Util.readFully(InputStreamReader(`in`, Util.UTF_8))
            }?: return ""
        }

        private val NULL_OUTPUT_STREAM = object : OutputStream() {
            @Throws(IOException::class)
            override fun write(b: Int) {
                // Eat all writes silently. Nom nom.
            }
        }
    }
}
