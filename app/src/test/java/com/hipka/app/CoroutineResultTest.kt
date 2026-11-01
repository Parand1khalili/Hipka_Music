package com.hipka.app

import com.hipka.app.core.util.coroutineResult
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class CoroutineResultTest {
    @Test(expected = CancellationException::class)
    fun cancellationPropagatesInsteadOfBecomingAnErrorState() {
        coroutineResult<Unit> { throw CancellationException("screen left") }
    }

    @Test fun ordinaryFailureRemainsRecoverable() {
        val error = java.io.IOException("offline")
        assertSame(error, coroutineResult<Unit> { throw error }.exceptionOrNull())
        assertEquals(42, coroutineResult { 42 }.getOrThrow())
    }
}
