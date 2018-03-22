package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.Deferred

/**
 * Created by Stainberg on 15/03/2018.
 */

object SlothClient {

    private val fixParameters = HashMap<String, String>()
    private val fixHeaders = HashMap<String, String>()

    fun <T : SlothResponse> request(url : String) : SlothRequest<T> {
        return SlothStandaloneRequest<T>().url(url)
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


}