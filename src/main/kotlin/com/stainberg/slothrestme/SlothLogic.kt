package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.*

/**
 * Created by Stainberg on 20/03/2018.
 */
internal object SlothLogic {

    fun <T : SlothResponse> get(request: SlothRequest, cls : Class<T>, success : suspend ResponseBlock.(T) -> Unit) : Deferred<*> {
        val block = StandaloneResponseBlock()
        val clazz = cls.newInstance()
        val task = async(CommonPool, CoroutineStart.LAZY, block = {
            fetchRequest(request, block, clazz, success)
        })
        block.initTask(task)
        return task
    }

    fun <T : SlothResponse> post(request: SlothRequest, cls : Class<T>, success : suspend ResponseBlock.(T) -> Unit) : Deferred<*> {
        val block = StandaloneResponseBlock()
        val clazz = cls.newInstance()
        val task = async(CommonPool, CoroutineStart.LAZY, block = {
            fetchRequest(request, block, clazz, success)
        })
        block.initTask(task)
        return task
    }

    fun <T : SlothResponse> patch(request: SlothRequest, cls : Class<T>, success : suspend ResponseBlock.(T) -> Unit) : Deferred<*> {
        val block = StandaloneResponseBlock()
        val clazz = cls.newInstance()
        val task = async(CommonPool, CoroutineStart.LAZY, block = {
            fetchRequest(request, block, clazz, success)
        })
        block.initTask(task)
        return task
    }

    fun <T : SlothResponse> delete(request: SlothRequest, cls : Class<T>, success : suspend ResponseBlock.(T) -> Unit) : Deferred<*> {
        val block = StandaloneResponseBlock()
        val clazz = cls.newInstance()
        val task = async(CommonPool, CoroutineStart.LAZY, block = {
            fetchRequest(request, block, clazz, success)
        })
        block.initTask(task)
        return task
    }

    private suspend fun <T : SlothResponse> fetchRequest(request: SlothRequest, block : ResponseBlock, clazz : T, success : suspend ResponseBlock.(T) -> Unit) {
        println("start load url = ${request.url()}")
        block.request = request
        delay(5000)
        success(block, clazz)
        println("end load")
    }

}