package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.Deferred
import okhttp3.RequestBody

/**
 * Created by Stainberg on 15/03/2018.
 */
open class SlothRequest {

    internal var url = ""
    internal var tag = ""
    private var jsonobject : Any? = null
    private var memthod = SlothRequestType.GET
    private val parameters = HashMap<String, String>()
    private val headers = HashMap<String, String>()
    private val attachments = arrayListOf<Attachment>()


    /**
     * set url
     */

    fun url(_url: String) : SlothRequest {
        url = _url
        tag = System.currentTimeMillis().toString()
        return this
    }

    /**
     * set params or header or attachment
     */

    fun param(key : String, value : String?) : SlothRequest {
        value?. let {
            parameters[key] = it
        }?: run {
            parameters.remove(key)
        }
        return this
    }

    fun header(key : String, value : String?) : SlothRequest {
        value?. let {
            headers[key] = it
        }?: run {
            headers.remove(key)
        }
        return this
    }

    fun jsonObject(json : Any?) : SlothRequest {
        jsonobject = json
        return this
    }

    fun addAttachment(att : Attachment) : SlothRequest {
        attachments.add(att)
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

    fun <T : SlothResponse> get(cls : Class<T>?, success : suspend ResponseBlock.(T?, Int) -> Unit) : Deferred<*> {
        memthod = SlothRequestType.GET
        return SlothLogic.get(this, cls, success)
    }

    fun <T : SlothResponse> post(cls : Class<T>?, success : suspend ResponseBlock.(T?, Int) -> Unit) : Deferred<*> {
        memthod = SlothRequestType.POST
        return SlothLogic.post(this, cls, success)
    }

    fun <T : SlothResponse> patch(cls : Class<T>?, success : suspend ResponseBlock.(T?, Int) -> Unit) : Deferred<*> {
        memthod = SlothRequestType.PATCH
        return SlothLogic.patch(this, cls, success)
    }

    fun <T : SlothResponse> delete(cls : Class<T>?, success : suspend ResponseBlock.(T?, Int) -> Unit) : Deferred<*> {
        memthod = SlothRequestType.DELETE
        return SlothLogic.delete(this, cls, success)
    }

    inner class Attachment {
        lateinit var key: String
        lateinit var filename: String
        lateinit var body: RequestBody
    }
}