package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.Deferred
import okhttp3.RequestBody

/**
 * Created by Stainberg on 15/03/2018.
 */
open class SlothRequest<T : SlothResponse> {

    internal var url = ""
    internal var tag = ""
    private var jsonobject : Any? = null
    private var memthod = SlothRequestType.GET
    private val parameters = HashMap<String, String>()
    private val headers = HashMap<String, String>()
    private val attachments = arrayListOf<Attachment>()
    internal var success : (suspend ResponseBlock<T>.(T?, Int) -> Unit)? = null
    internal var cls : Class<T>? = null

    /**
     * set url
     */

    fun url(_url: String) : SlothRequest<T> {
        url = _url
        tag = System.currentTimeMillis().toString()
        return this
    }

    /**
     * set params or header or attachment
     */

    fun param(key : String, value : String?) : SlothRequest<T> {
        value?. let {
            parameters[key] = it
        }?: run {
            parameters.remove(key)
        }
        return this
    }

    fun header(key : String, value : String?) : SlothRequest<T> {
        value?. let {
            headers[key] = it
        }?: run {
            headers.remove(key)
        }
        return this
    }

    fun jsonObject(json : Any?) : SlothRequest<T> {
        jsonobject = json
        return this
    }

    fun addAttachment(att : Attachment) : SlothRequest<T> {
        attachments.add(att)
        return this
    }

    fun onSuccess(c : Class<T>, block : suspend ResponseBlock<T>.(T?, Int) -> Unit) : SlothRequest<T> {
        success = block
        cls = c
        return this
    }

    fun onFailed(block : suspend ResponseBlock<T>.(T?, Int) -> Unit) : SlothRequest<T> {
        return this
    }

    fun onCompleted(block : suspend ResponseBlock<T>.(T?, Int) -> Unit) : SlothRequest<T> {
        return this
    }

    /**
     * read values
     */

    fun url() : String {
        return url
    }

    fun tag() : String {
        return tag
    }

    fun params() : Map<String, String> {
        return parameters
    }

    fun headers() : Map<String, String> {
        return headers
    }

    fun method() : SlothRequestType {
        return memthod
    }

    fun jsonObject() : Any? {
        return jsonobject
    }

    fun attachments() : MutableList<Attachment> {
        return attachments
    }

    /**
     * define function
     */

    fun  get() : Deferred<*> {
        memthod = SlothRequestType.GET
        return SlothLogic.get(this)
    }

    fun post() : Deferred<*> {
        memthod = SlothRequestType.POST
        return SlothLogic.post(this)
    }

    fun patch() : Deferred<*> {
        memthod = SlothRequestType.PATCH
        return SlothLogic.patch(this)
    }

    fun delete() : Deferred<*> {
        memthod = SlothRequestType.DELETE
        return SlothLogic.delete(this)
    }

    inner class Attachment {
        lateinit var key: String
        lateinit var filename: String
        lateinit var body: RequestBody
    }
}