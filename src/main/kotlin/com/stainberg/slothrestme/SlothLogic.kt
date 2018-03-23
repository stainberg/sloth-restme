package com.stainberg.slothrestme

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.experimental.*
import okhttp3.*
import java.io.IOException

/**
 * Created by Stainberg on 20/03/2018.
 */
internal object SlothLogic {

    fun get(request: SlothRequest, start: CoroutineStart) : Deferred<*> {
        val block = CompletedResponseBlock(request)
        val task = async(CommonPool, start, block = {
            fetchRequest(request, block)
        })
        block.initTask(task)
        return task
    }

    fun post(request: SlothRequest, start: CoroutineStart) : Deferred<*> {
        val block = CompletedResponseBlock(request)
        val task = async(CommonPool, start, block = {
            fetchRequest(request, block)
        })
        block.initTask(task)
        return task
    }

    fun patch(request: SlothRequest, start: CoroutineStart) : Deferred<*> {
        val block = CompletedResponseBlock(request)
        val task = async(CommonPool, start, block = {
            fetchRequest(request, block)
        })
        block.initTask(task)
        return task
    }

    fun delete(request: SlothRequest, start: CoroutineStart) : Deferred<*> {
        val block = CompletedResponseBlock(request)
        val task = async(CommonPool, start, block = {
            fetchRequest(request, block)
        })
        block.initTask(task)
        return task
    }

    suspend fun fetchRequest(request: SlothRequest, completedResponseBlock: CompletedResponseBlock) {
        val req = parse(request)
        var code = 0
        var message = ""
        val success = request.success
        val failed = request.failed
        val completed = request.completed
        var result: Any? = null
        try {
            val response = SlothHttpClient.httpClient.newCall(req).execute()
            var responseString: String
            SlothLogger.printcUrl(request)
            SlothLogger.log("params", request.params())
            SlothLogger.log("headers", request.headers())
            response?.let { resp ->
                code = resp.code()
                if (resp.isSuccessful) {
                    resp.body()?.let { body ->
                        responseString = body.string()
                        request.cls?. let {
                            if(responseString.isNotEmpty()) {
                                SlothLogger.log("fetchRequest", responseString)
                                result = SlothGson.fromJson(responseString, it)
                            }
                        }
                    }
                }
                resp.close()
            }?: run {
                code = SlothNetworkConstants.NO_RESPONSE
                message = "no response"
            }
        } catch (e : IOException) {
            code = SlothNetworkConstants.NETWORK_ERROR
            e.message?. let {
                message = it
            }
        } catch (e : Exception) {
            code = SlothNetworkConstants.PARSER_ERROR
            e.message?. let {
                message = it
            }
        }
        if(code in 200..299) {
            if(success != null) {
                result?. let {
                    if(request.handler() == SlothHandleType.main) {
                        handler.post {
                            runBlocking {
                                success(SuccessResponseBlock(request), it)
                            }
                        }
                    } else {
                        success(SuccessResponseBlock(request), it)
                    }
                }?: run {
                    failed?. let {fl->
                        if(request.handler() == SlothHandleType.main) {
                            handler.post {
                                runBlocking {
                                    fl(FailedResponseBlock(request), SlothNetworkConstants.NO_RESULT_SET, "no result set")
                                }
                            }
                        } else {
                            fl(FailedResponseBlock(request), SlothNetworkConstants.NO_RESULT_SET, "no result set")
                        }
                    }
                }
            }
        } else {
            if(failed != null) {
                if(request.handler() == SlothHandleType.main) {
                    handler.post {
                        runBlocking {
                            failed(FailedResponseBlock(request), code, message)
                        }
                    }
                } else {
                    failed(FailedResponseBlock(request), code, message)
                }
            }
        }
        if(completed != null) {
            if(request.handler() == SlothHandleType.main) {
                handler.post {
                    runBlocking {
                        println(Thread.currentThread().id)
                        completed(completedResponseBlock)
                    }
                }
            } else {
                completed(completedResponseBlock)
            }
        }
    }

    private fun parse(requestEntity: SlothRequest) : Request {
        val params = requestEntity.params()
        val headers = requestEntity.headers()
        val attachments = requestEntity.attachments()
        val builder = Request.Builder()
        var url = requestEntity.url()
        builder.addHeader("Cache-Control", "no-cache")
        if (headers.isNotEmpty()) {
            val set = headers.entries
            for (maEntry in set) {
                builder.addHeader(maEntry.key, maEntry.value)
            }
        }
        if (requestEntity.method() == SlothRequestType.GET) {
            if (params.isNotEmpty()) {
                url += "?" + SlothHttpUtils.getNameValuePair(params)!!
            }
        } else if (requestEntity.method() == SlothRequestType.POST) {
            requestEntity.jsonObject()?. let {
                builder.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), SlothGson.toJson(it)))
            }?: run {
                val body: RequestBody
                if (attachments.size == 0) {
                    val bodyBuilder = FormBody.Builder()
                    for (maEntry in params.entries) {
                        bodyBuilder.add(maEntry.key, maEntry.value)
                    }
                    body = bodyBuilder.build()
                } else {
                    val bodyBuilder = MultipartBody.Builder()
                    bodyBuilder.setType(MultipartBody.FORM)
                    for (maEntry in params.entries) {
                        bodyBuilder.addFormDataPart(maEntry.key, maEntry.value)
                    }
                    for (request in attachments) {
                        bodyBuilder.addFormDataPart(request.key, request.filename, request.body)
                    }
                    body = bodyBuilder.build()
                }
                builder.post(body)
            }
        } else if (requestEntity.method() == SlothRequestType.PATCH) {
            requestEntity.jsonObject()?. let {
                builder.patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), SlothGson.toJson(it)))
            }?: run {
                val body: RequestBody
                if (attachments.size == 0) {
                    val bodyBuilder = FormBody.Builder()
                    for (maEntry in params.entries) {
                        bodyBuilder.add(maEntry.key, maEntry.value)
                    }
                    body = bodyBuilder.build()
                } else {
                    val bodyBuilder = MultipartBody.Builder()
                    bodyBuilder.setType(MultipartBody.FORM)
                    for (maEntry in params.entries) {
                        bodyBuilder.addFormDataPart(maEntry.key, maEntry.value)
                    }
                    for (request in attachments) {
                        bodyBuilder.addFormDataPart(request.key, request.filename, request.body)
                    }
                    body = bodyBuilder.build()
                }
                builder.patch(body)
            }
        } else if (requestEntity.method() == SlothRequestType.DELETE) {
            requestEntity.jsonObject()?. let {
                builder.patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), SlothGson.toJson(it)))
            }?: run {
                val body: RequestBody
                if (attachments.size == 0) {
                    val bodyBuilder = FormBody.Builder()
                    for (maEntry in params.entries) {
                        bodyBuilder.add(maEntry.key, maEntry.value)
                    }
                    body = bodyBuilder.build()
                } else {
                    val bodyBuilder = MultipartBody.Builder()
                    bodyBuilder.setType(MultipartBody.FORM)
                    for (maEntry in params.entries) {
                        bodyBuilder.addFormDataPart(maEntry.key, maEntry.value)
                    }
                    for (request in attachments) {
                        bodyBuilder.addFormDataPart(request.key, request.filename, request.body)
                    }
                    body = bodyBuilder.build()
                }
                builder.delete(body)
            }
        }
        builder.tag(SecurityMD5.ToMD5(url))
        builder.url(url)
        return builder.build()
    }

    private val handler = Handler(Looper.getMainLooper())


}