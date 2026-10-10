package com.december172.daytiles.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

class DateConverters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
}

