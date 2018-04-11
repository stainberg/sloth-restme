package com.stainberg.slothrestme

import com.stainberg.slothrestme.cache.DiskLruCache
import okio.ByteString
import java.io.File



internal class SlothCache(directory: File, maxSize: Long) {

    private val VERSION = 201804
    private val ENTRY_METADATA = 0
    private val ENTRY_BODY = 1
    private val ENTRY_COUNT = 1

    private var cache: DiskLruCache

    init {
        this.cache = DiskLruCache.open(directory, VERSION, ENTRY_COUNT, maxSize)
    }

    private fun key(request: SlothRequest): String {
        val k = request.url() + SlothGson.toJson(request.params())
        return ByteString.encodeUtf8(k).md5().hex()
    }

    fun get(request : SlothRequest) : String? {
        val key = key(request)
        val snapshot = cache[key]
        snapshot?. let {
            val result = it.getString(0)
            it.close()
            return result
        }
        return null
    }

    fun put(request : SlothRequest, response : String) {
        val key = key(request)
        val creator = cache.edit(key)
        creator?. let {
            it[0] = response
            it.commit()
        }
    }

    fun remove(request : SlothRequest) {
        val key = key(request)
        cache.remove(key)
    }
}