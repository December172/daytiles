package com.december172.daytiles.domain.model

fun parseTaskTags(value: String): List<String> =
    value.split(',').map(String::trim).filter(String::isNotEmpty).distinct()

fun normalizeTaskTags(value: String): String = parseTaskTags(value).joinToString(",")
