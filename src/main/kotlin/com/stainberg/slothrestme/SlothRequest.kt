package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.Deferred

/**
 * Created by Stainberg on 15/03/2018.
 */
open class SlothRequest {

    private var url : String = ""

    private val parameters = HashMap<String, String>()
    private val headermeters = HashMap<String, String>()

    fun <T : SlothResponse> get(cls : Class<T>, success : suspend ResponseBlock.(T) -> Unit) : Deferred<*> {
        return SlothLogic.get(this, cls, success)
    }

    fun <T : SlothResponse> post(cls : Class<T>, success : suspend ResponseBlock.(T) -> Unit) : Deferred<*> {
        return SlothLogic.post(this, cls, success)
    }

    fun <T : SlothResponse> patch(cls : Class<T>, success : suspend ResponseBlock.(T) -> Unit) : Deferred<*> {
        return SlothLogic.patch(this, cls, success)
    }

    fun <T : SlothResponse> delete(cls : Class<T>, success : suspend ResponseBlock.(T) -> Unit) : Deferred<*> {
        return SlothLogic.delete(this, cls, success)
    }

    fun url(_url: String) : SlothRequest {
        url = _url
        return this
    }

    fun url() : String {
        return url
    }

    fun addParam(key : String, value : String) : SlothRequest {
        parameters[key] = value
        return this
    }

    fun removeParam(key : String) : SlothRequest {
        parameters.remove(key)
        return this
    }

    fun addHeader(key : String, value : String) : SlothRequest {
        headermeters[key] = value
        return this
    }

    fun removeHeader(key : String) : SlothRequest {
        headermeters.remove(key)
        return this
    }


}