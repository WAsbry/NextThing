package com.nextthing.app.work

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkerFailurePolicyTest {

    @Test
    fun `first two failures retry`() {
        assertEquals(WorkerFailureAction.RETRY, WorkerFailurePolicy.decide(0))
        assertEquals(WorkerFailureAction.RETRY, WorkerFailurePolicy.decide(1))
    }

    @Test
    fun `third failure stops current run`() {
        assertEquals(WorkerFailureAction.FAIL_CURRENT_RUN, WorkerFailurePolicy.decide(2))
    }
}
