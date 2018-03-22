package com.stainberg.slothrestme

import com.alibaba.fastjson.annotation.JSONField
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import okhttp3.RequestBody

/**
 * Created by Stainberg on 15/03/2018.
 */
open class SlothRequest {

    internal var url = ""
    private var tag = ""
    private var jsonobject : Any? = null
    private var memthod = SlothRequestType.GET
    private val parameters = HashMap<String, String>()
    private val headers = HashMap<String, String>()
    private val attachments = arrayListOf<Attachment>()

    @JSONField(serialize=false, deserialize=false) internal var success : (suspend SuccessResponseBlock.(SlothResponse) -> Unit)? = null

    @JSONField(serialize=false, deserialize=false) internal var failed : (suspend FailedResponseBlock.(Int) -> Unit)? = null

    @JSONField(serialize=false, deserialize=false) internal var completed : (suspend CompletedResponseBlock.() -> Unit)? = null

    internal var cls : Class<SlothResponse>? = null

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

    fun <T : SlothResponse> onSuccess(c : Class<T>, block : suspend SuccessResponseBlock.(SlothResponse) -> Unit) : SlothRequest {
        success = block
        cls = c as Class<SlothResponse>
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
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest))
        }
    }

    fun  getSync() : Deferred<*> {
        memthod = SlothRequestType.GET
        return SlothLogic.get(this, CoroutineStart.LAZY)
    }

    fun  post() {
        memthod = SlothRequestType.POST
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest))
        }
    }

    fun postSync() : Deferred<*> {
        memthod = SlothRequestType.POST
        return SlothLogic.post(this, CoroutineStart.LAZY)
    }

    fun  patch() {
        memthod = SlothRequestType.PATCH
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest))
        }
    }

    fun patchSync() : Deferred<*> {
        memthod = SlothRequestType.PATCH
        return SlothLogic.patch(this, CoroutineStart.LAZY)
    }

    fun  delete() {
        memthod = SlothRequestType.DELETE
        launch {
            SlothLogic.fetchRequest(this@SlothRequest, CompletedResponseBlock(this@SlothRequest))
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