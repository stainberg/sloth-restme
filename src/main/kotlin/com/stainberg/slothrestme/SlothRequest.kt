package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import okhttp3.RequestBody

/**
 * Created by Stainberg on 15/03/2018.
 */
open class SlothRequest {

    private val parameters = HashMap<String, String>()
    private val headers = HashMap<String, String>()
    private val attachments = arrayListOf<Attachment>()

    private var url = ""
    private var tag = ""
    private var jsonobject : Any? = null
    private var method = SlothRequestType.GET
    private var handler = SlothHandleType.main

    internal var success : (suspend SuccessResponseBlock.(Any) -> Unit)? = null

    internal var failed : (suspend FailedResponseBlock.(Int, String) -> Unit)? = null

    internal var completed : (suspend CompletedResponseBlock.() -> Unit)? = null

    var cls : Class<Any>? = null

    /**
     * set url
     */

    fun url(url: String) : SlothRequest {
        this.url = url
        this.tag = System.currentTimeMillis().toString()
        return this
    }

    /**
     * set params or header or attachment
     */

    fun tag(tag : String) : SlothRequest {
        this.tag = tag
        return this
    }

    fun param(key : String, value : String?) : SlothRequest {
        value?. let {
            this.parameters[key] = it
        }?: run {
            this.parameters.remove(key)
        }
        return this
    }

    fun header(key : String, value : String?) : SlothRequest {
        value?. let {
            this.headers[key] = it
        }?: run {
            this.headers.remove(key)
        }
        return this
    }

    fun jsonObject(jsonobject : Any?) : SlothRequest {
        this.jsonobject = jsonobject
        return this
    }

    fun addAttachment(attachment : Attachment) : SlothRequest {
        this.attachments.add(attachment)
        return this
    }

    /**
     * set result block
     */

    @Suppress("UNCHECKED_CAST")
    fun <T> onSuccess(c : Class<T>, block : suspend SuccessResponseBlock.(T) -> Unit) : SlothRequest {
        success = block as (suspend SuccessResponseBlock.(Any) -> Unit)
        cls = c as Class<Any>
        return this
    }

    fun onFailed(block : suspend FailedResponseBlock.(Int, String) -> Unit) : SlothRequest {
        failed = block
        return this
    }

    fun onCompleted(block : suspend CompletedResponseBlock.() -> Unit) : SlothRequest {
        completed = block
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

    fun handler() : SlothHandleType {
        return handler
    }

    fun params() : Map<String, String> {
        return parameters
    }

    fun headers() : Map<String, String> {
        return headers
    }

    fun method() : SlothRequestType {
        return method
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

    fun  get(handler : SlothHandleType? = SlothHandleType.main) {
        method = SlothRequestType.GET
        handler?. let {
            this.handler = it
        }
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest))
        }
    }

    fun  getSync(handler : SlothHandleType? = SlothHandleType.main) : Deferred<*> {
        method = SlothRequestType.GET
        handler?. let {
            this.handler = it
        }
        return SlothLogic.get(this, CoroutineStart.LAZY)
    }

    fun  post(handler : SlothHandleType? = SlothHandleType.main) {
        method = SlothRequestType.POST
        handler?. let {
            this.handler = it
        }
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest))
        }
    }

    fun postSync(handler : SlothHandleType? = SlothHandleType.main) : Deferred<*> {
        method = SlothRequestType.POST
        handler?. let {
            this.handler = it
        }
        return SlothLogic.post(this, CoroutineStart.LAZY)
    }

    fun  patch(handler : SlothHandleType? = SlothHandleType.main) {
        method = SlothRequestType.PATCH
        handler?. let {
            this.handler = it
        }
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest))
        }
    }

    fun patchSync(handler : SlothHandleType? = SlothHandleType.main) : Deferred<*> {
        method = SlothRequestType.PATCH
        handler?. let {
            this.handler = it
        }
        return SlothLogic.patch(this, CoroutineStart.LAZY)
    }

    fun  delete(handler : SlothHandleType? = SlothHandleType.main) {
        method = SlothRequestType.DELETE
        handler?. let {
            this.handler = it
        }
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest))
        }
    }

    fun deleteSync(handler : SlothHandleType? = SlothHandleType.main) : Deferred<*> {
        method = SlothRequestType.DELETE
        handler?. let {
            this.handler = it
        }
        return SlothLogic.delete(this, CoroutineStart.LAZY)
    }

    inner class Attachment {
        lateinit var key: String
        lateinit var filename: String
        lateinit var body: RequestBody
    }
}