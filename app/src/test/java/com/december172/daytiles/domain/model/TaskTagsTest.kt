package com.december172.daytiles.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskTagsTest {
    @Test
    fun parsesTrimsAndDeduplicatesTags() {
        assertEquals(listOf("Home", "Errands"), parseTaskTags(" Home, ,Errands,Home "))
    }

    @Test
    fun normalizesTagsForStorage() {
        assertEquals("Home,Errands", normalizeTaskTags(" Home, ,Errands,Home "))
    }
}
