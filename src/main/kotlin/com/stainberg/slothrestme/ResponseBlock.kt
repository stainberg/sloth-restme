package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.Deferred

/**
 * Created by Stainberg on 15/03/2018.
 */
abstract class ResponseBlock {

    lateinit var request : SlothRequest

    private var task: Deferred<*>? = null

    fun initTask(job: Deferred<*>) {
        task = job

    }

    fun cancelRemainderTasks() {
        task?.cancel(CancelException("CancelException"))
    }

}