package com.example.taskoday.domain.model

import java.time.LocalDate
import java.time.Period

fun ageFromBirthDate(
    birthDate: String,
    today: LocalDate = LocalDate.now(),
): Int? =
    runCatching {
        val parsed = LocalDate.parse(birthDate)
        if (parsed.isAfter(today)) null else Period.between(parsed, today).years
    }.getOrNull()
