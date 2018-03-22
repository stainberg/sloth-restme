package com.stainberg.slothrestme

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch

/**
 * Created by Stainberg on 15/03/2018.
 */
class SlothRequestSet(vararg jobs: Deferred<*>) {

    private val tasks = jobs

    fun start(setFinished : suspend SlothSetBlock.() -> Unit = {}) {
        launch(CommonPool, CoroutineStart.DEFAULT, block = {
            var cancel = false
            tasks.forEach {task->
                if(cancel) {
                    task.cancel(CancelException("CancelException"))
                } else {
                    try {
                        task.await()
                    } catch (e : CancelException) {
                        SlothLogger.log("SlothRequestSet", e.message)
                        cancel = true
                    }
                }
            }
            setFinished(StandaloneSetBlock())
        })
    }

}