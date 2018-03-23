package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import okhttp3.RequestBody

/**
 * Created by Stainberg on 15/03/2018.
 */
open class SlothRequest {

    var url = ""
    private var tag = ""
    private var jsonobject : Any? = null
    private var memthod = SlothRequestType.GET
    private val parameters = HashMap<String, String>()
    private val headers = HashMap<String, String>()
    private val attachments = arrayListOf<Attachment>()

    internal var success : (suspend SuccessResponseBlock.(Any) -> Unit)? = null

    internal var failed : (suspend FailedResponseBlock.(Int) -> Unit)? = null

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

    fun onFailed(block : suspend FailedResponseBlock.(Int) -> Unit) : SlothRequest {
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

    fun  get() {
        memthod = SlothRequestType.GET
        val thread = Thread.currentThread()
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest), thread)
        }
    }

    fun  getSync() : Deferred<*> {
        memthod = SlothRequestType.GET
        return SlothLogic.get(this, CoroutineStart.LAZY)
    }

    fun  post() {
        memthod = SlothRequestType.POST
        val thread = Thread.currentThread()
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest), thread)
        }
    }

    fun postSync() : Deferred<*> {
        memthod = SlothRequestType.POST
        return SlothLogic.post(this, CoroutineStart.LAZY)
    }

    fun  patch() {
        memthod = SlothRequestType.PATCH
        val thread = Thread.currentThread()
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest), thread)
        }
    }

    fun patchSync() : Deferred<*> {
        memthod = SlothRequestType.PATCH
        return SlothLogic.patch(this, CoroutineStart.LAZY)
    }

    fun  delete() {
        memthod = SlothRequestType.DELETE
        val thread = Thread.currentThread()
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest), thread)
        }
    }

    fun deleteSync() : Deferred<*> {
        memthod = SlothRequestType.DELETE
        return SlothLogic.delete(this, CoroutineStart.LAZY)
    }

    inner class Attachment {
        lateinit var key: String
        lateinit var filename: String
        lateinit var body: RequestBody
    }
}