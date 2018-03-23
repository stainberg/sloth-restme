package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.Deferred

/**
 * Created by Stainberg on 15/03/2018.
 */
class CompletedResponseBlock(override var request: SlothRequest) : ResponseBlock {

    private lateinit var task: Deferred<*>

    internal fun initTask(job: Deferred<*>) {
        task = job
    }

    fun cancelRemainderTasks() {
        task.cancel(CancelException("CancelException"))
    }

}