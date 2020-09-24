package io.getstream.chat.android.livedata.utils

import android.os.Looper
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import io.getstream.chat.android.client.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException

interface Call2<T> {

    @WorkerThread
    fun execute(): Result<T>

    @UiThread
    fun enqueue(callback: (Result<T>) -> Unit = {})

    fun cancel()
    suspend fun invoke(): Result<T>
}

class CallImpl2<T>(var runnable: suspend () -> Result<T>, var scope: CoroutineScope = GlobalScope, var mainThreadAllowed: Boolean = false) :
    Call2<T> {
    var canceled: Boolean = false

    override fun cancel() {
        canceled = true
    }

    override fun execute(): Result<T> {
        val isMainThread = Looper.getMainLooper().thread == Thread.currentThread()
        if (isMainThread && !mainThreadAllowed) {
            throw IOException("This use case shouldn't be run on the main thread. Be sure to use enqueue instead of execute or run the execute call in the IO thread.")
        }
        return runBlocking(scope.coroutineContext) { runnable() }
    }

    override suspend fun invoke(): Result<T> = withContext(scope.coroutineContext) {
        runnable()
    }

    override fun enqueue(callback: (Result<T>) -> Unit) {
        scope.launch(scope.coroutineContext) {
            if (!canceled) {
                val result = runnable()
                callback(result)
            }
        }
    }
}
