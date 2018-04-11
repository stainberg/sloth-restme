package com.stainberg.slothrestme

import android.content.Context
import android.util.SparseArray
import kotlinx.coroutines.experimental.Deferred

/**
 * Created by Stainberg on 15/03/2018.
 */

object SlothClient {

    internal val fixParameters = mutableMapOf<String, String>()
    internal val fixHeaders = mutableMapOf<String, String>()
    internal val codeHandlers = SparseArray<CodeHandlerBlock.(SlothRequest) -> Unit>()
    internal var cache : SlothCache? = null

    fun request(url : String) : SlothRequest {
        return SlothStandaloneRequest().url(url)
    }

    fun requestSet(vararg jobs : Deferred<*>) : SlothRequestSet {
        return SlothRequestSet(*jobs)
    }

    /**
     * set fix params or headers
     */

    fun fixParam(key : String, value : String?) {
        value?. let {
            fixParameters[key] = it
        }?: run {
            fixParameters.remove(key)
        }
    }

    fun fixHeader(key : String, value : String?) {
        value?. let {
            fixHeaders[key] = it
        }?: run {
            fixHeaders.remove(key)
        }
    }

    fun initCache(c : Context, maxSizeM : Int = 50) {
        cache = SlothCache(c.externalCacheDir, maxSizeM.toLong() * 1024 * 1024)
    }

    fun subscribeHttpCodeHandler(code : Int, block : CodeHandlerBlock.(SlothRequest) -> Unit = {}) {
        codeHandlers.put(code, block)
    }

    fun unSubscribeHttpCodeHandler(code : Int) {
        codeHandlers.remove(code)
    }

}