package com.december172.daytiles.ui.planner

import com.december172.daytiles.data.local.CourseEntity

data class PositionedCourse(val course: CourseEntity, val lane: Int, val laneCount: Int)

/** Adjacent classes can share a column; overlapping classes remain individually selectable. */
fun layoutCoursesForDay(courses: List<CourseEntity>): List<PositionedCourse> {
    val sorted = courses.sortedWith(compareBy(CourseEntity::startMinute, CourseEntity::endMinute, CourseEntity::id))
    val result = mutableListOf<PositionedCourse>()
    var index = 0
    while (index < sorted.size) {
        val group = mutableListOf<CourseEntity>()
        var end = sorted[index].endMinute
        while (index < sorted.size && (group.isEmpty() || sorted[index].startMinute < end)) {
            val course = sorted[index++]
            group += course
            end = maxOf(end, course.endMinute)
        }
        val laneEnds = mutableListOf<Int>()
        val assigned = group.map { course ->
            val reusable = laneEnds.indexOfFirst { it <= course.startMinute }
            val lane = if (reusable >= 0) reusable else laneEnds.size.also { laneEnds += 0 }
            laneEnds[lane] = course.endMinute
            course to lane
        }
        result += assigned.map { (course, lane) -> PositionedCourse(course, lane, laneEnds.size) }
    }
    return result
}
