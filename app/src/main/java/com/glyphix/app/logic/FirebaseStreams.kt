package com.glyphix.app.logic

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

/** Reattach reads cancelled before authentication completes or after a transient failure. */
internal fun <T> Flow<T>.retryFirebaseReads(): Flow<T> = retryWhen { cause, attempt ->
    if (cause is CancellationException) false else {
        delay((1000L shl attempt.coerceAtMost(5).toInt()).coerceAtMost(30000L))
        true
    }
}
