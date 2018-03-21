package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.Deferred

/**
 * Created by Stainberg on 15/03/2018.
 */

object SlothClient {

    fun request(url : String) : SlothRequest {
        return SlothStandaloneRequest().url(url)
    }

    fun requestSet(vararg jobs : Deferred<*>) : SlothRequestSet {
        return SlothRequestSet(*jobs)
    }


}