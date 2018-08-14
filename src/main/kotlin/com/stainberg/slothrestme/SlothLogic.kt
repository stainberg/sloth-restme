package com.stainberg.slothrestme

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.experimental.*
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.experimental.coroutineContext

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

    fun fetchRequest(request: SlothRequest, completedResponseBlock: CompletedResponseBlock) {
        val req = parse(request)
        var code = 0
        var message = ""
        val success = request.success
        val failed = request.failed
        val completed = request.completed
        val local = request.local
        var result: Any? = null
        SlothClient.cache?. let {cache ->
            val resultStr = cache.get(request)
            resultStr?. let {
                request.cls?. let {
                    SlothLogger.log("fetchLocalRequest", resultStr)
                    if(it.simpleName == String::class.java.simpleName) {
                        local?.let {
                            if (request.handler() == SlothHandleType.main) {
                            handler.post {
                                    local(LocalResponseBlock(request), resultStr)
                                }
                            } else {
                                local(LocalResponseBlock(request), resultStr)
                            }
                        }
                    } else {
                        val tmpResult = SlothGson.fromJson(resultStr, it)
                        tmpResult?. let {
                            local?.let {
                                if (request.handler() == SlothHandleType.main) {
                                handler.post {
                                        local(LocalResponseBlock(request), tmpResult)
                                    }
                                } else {
                                    local(LocalResponseBlock(request), tmpResult)
                                }
                            }
                        }
                    }
                }
            }
        }
        val client = SlothHttpClient.customClient?.httpClient?:SlothHttpClient.httpClient
        var responseString = ""
        try {
            val response = client.newCall(req).execute()
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
                                if(it.simpleName != String::class.java.simpleName) {
                                    result = SlothGson.fromJson(responseString, it)
                                }
                                SlothClient.cache?. let {
                                    if(request.cache) {
                                        it.put(request, responseString)
                                    }
                                }
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
                            success(SuccessResponseBlock(request), it)
                        }
                    } else {
                        success(SuccessResponseBlock(request), it)
                    }
                }?: run {
                    if(request.handler() == SlothHandleType.main) {
                    handler.post {
                            success(SuccessResponseBlock(request), responseString)
                        }
                    } else {
                        success(SuccessResponseBlock(request), responseString)
                    }
                }
            }
        } else {
            if(failed != null) {
                if(request.handler() == SlothHandleType.main) {
                    handler.post {
                        failed(FailedResponseBlock(request), code, message)
                    }
                } else {
                    failed(FailedResponseBlock(request), code, message)
                }
            }
        }
        SlothClient.codeHandlers[code]?. let {
            launch {
                SlothClient.codeHandlers[code](CodeHandlerBlock(), request)
            }
        }
        if(completed != null) {
            completed(completedResponseBlock)
        }
    }

    private fun parse(requestEntity: SlothRequest) : Request {
        val params = requestEntity.slothParams
        val headers = requestEntity.slothHeaders
        val attachments = requestEntity.attachments
        val builder = Request.Builder()
        var url = requestEntity.url()
        if(SlothClient.fixHeaders.isNotEmpty()) {
            headers.putAll(SlothClient.fixHeaders)
        }
        if(SlothClient.fixParameters.isNotEmpty()) {
            params.putAll(SlothClient.fixParameters)
        }
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