package com.december172.daytiles.ui

import com.december172.daytiles.data.local.CourseEntity
import com.december172.daytiles.ui.planner.layoutCoursesForDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseTimetableLayoutTest {
    private fun course(id: Long, start: Int, end: Int) = CourseEntity(id, "Course $id", 1, start, end, "")

    @Test
    fun adjacentClassesKeepTheFullColumnWidth() {
        val result = layoutCoursesForDay(listOf(course(2, 600, 660), course(1, 540, 600)))
        assertEquals(listOf(1L, 2L), result.map { it.course.id })
        assertTrue(result.all { it.lane == 0 && it.laneCount == 1 })
    }

    @Test
    fun overlappingClassesGetSeparateSelectableLanes() {
        val result = layoutCoursesForDay(listOf(course(1, 540, 660), course(2, 570, 630), course(3, 600, 690)))
        assertEquals(listOf(0, 1, 2), result.map { it.lane })
        assertTrue(result.all { it.laneCount == 3 })
    }

    @Test
    fun transitiveOverlapsReuseEndedLanesAndLeaveLaterGroupsFullWidth() {
        val result = layoutCoursesForDay(listOf(course(1, 540, 600), course(2, 570, 660), course(3, 630, 690), course(4, 720, 780)))
        assertEquals(listOf(0, 1, 0, 0), result.map { it.lane })
        assertEquals(listOf(2, 2, 2, 1), result.map { it.laneCount })
    }
}
