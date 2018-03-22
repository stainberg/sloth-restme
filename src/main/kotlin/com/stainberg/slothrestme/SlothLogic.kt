package com.stainberg.slothrestme

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
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

    internal suspend fun fetchRequest(request: SlothRequest, completedResponseBlock: CompletedResponseBlock) {
        val req = parse(request)
        var code = 0
        val success = request.success
        val failed = request.failed
        val completed = request.completed
        var result: SlothResponse? = null
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
                        if (request.cls != null) {
                            if(responseString.isNotEmpty()) {
                                SlothLogger.log("fetchRequest", responseString)
                                result = JSON.parseObject(responseString, request.cls)
                            }
                        }
                    }
                }
                resp.close()
            }?: run {
                code = SlothNetworkConstants.NO_RESPONSE
            }
        } catch (e : IOException) {
            code = SlothNetworkConstants.NETWORK_ERROR
        } catch (e : JSONException) {
            code = SlothNetworkConstants.PARSER_ERROR
        }
        if(code in 200..299) {
            success?. let {sc->
                result?. let {
                    sc(SuccessResponseBlock(request), it)
                }?: run {
                    failed?. let {fl->
                        fl(FailedResponseBlock(request), SlothNetworkConstants.NO_RESULT_SET)
                    }
                }
            }
        } else {
            failed?. let {fl->
                fl(FailedResponseBlock(request), code)
            }
        }
        completed?. let {cp->
            cp(completedResponseBlock)
        }
    }

    private fun parse(requestEntity: SlothRequest) : Request {
        val params = requestEntity.params()
        val headers = requestEntity.headers()
        val attachments = requestEntity.attachments()
        val builder = Request.Builder()
        builder.addHeader("Cache-Control", "no-cache")
        if (headers.isNotEmpty()) {
            val set = headers.entries
            for (maEntry in set) {
                builder.addHeader(maEntry.key, maEntry.value)
            }
        }
        if (requestEntity.method() == SlothRequestType.GET) {
            if (params.isNotEmpty()) {
                requestEntity.url += "?" + SlothHttpUtils.getNameValuePair(params)!!
            }
        } else if (requestEntity.method() == SlothRequestType.POST) {
            if (requestEntity.jsonObject() != null) {
                builder.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(requestEntity.jsonObject())))
            } else {
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
            if (requestEntity.jsonObject() != null) {
                builder.patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(requestEntity.jsonObject())))
            } else {
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
            if (requestEntity.jsonObject() != null) {
                builder.patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(requestEntity.jsonObject())))
            } else {
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
        builder.tag(SecurityMD5.ToMD5(requestEntity.url))
        builder.url(requestEntity.url)
        return builder.build()
    }

}