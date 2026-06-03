package com.lifeforge.data.mapper

import com.lifeforge.data.model.dto.ExpenseScheduleDto
import com.lifeforge.data.model.dto.ExpenseScheduleRequestDto
import com.lifeforge.data.model.dto.IncomeScheduleDto
import com.lifeforge.data.model.dto.IncomeScheduleRequestDto
import com.lifeforge.domain.model.ExpenseCategory
import com.lifeforge.domain.model.ExpenseSchedule
import com.lifeforge.domain.model.ExpenseScheduleParams
import com.lifeforge.domain.model.IncomeSchedule
import com.lifeforge.domain.model.IncomeScheduleParams
import com.lifeforge.domain.model.IncomeType
import com.lifeforge.domain.model.RecurrenceType
import java.math.BigDecimal
import java.time.Instant

/**
 * Mappers dos schedules recorrentes (Sprint 6). Network-only: DTO<->Domain e
 * Params->RequestDto. Sem Entity (schedules não são cacheados em Room).
 */

fun IncomeScheduleDto.toDomain(): IncomeSchedule = IncomeSchedule(
    id = id,
    userId = userId,
    source = source,
    amountPerOccurrence = BigDecimal(amountPerOccurrence),
    incomeType = IncomeType.valueOf(incomeType),
    recurrence = RecurrenceType.valueOf(recurrence),
    startDate = Instant.parse(startDate),
    endDate = endDate?.let { Instant.parse(it) },
    installmentsTotal = installmentsTotal,
    createdAt = Instant.parse(createdAt),
    generatedCount = generatedCount,
)

fun IncomeScheduleParams.toRequestDto(): IncomeScheduleRequestDto = IncomeScheduleRequestDto(
    source = source,
    amountPerOccurrence = amountPerOccurrence.toPlainString(),
    incomeType = incomeType.name,
    recurrence = recurrence.name,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    installmentsTotal = installmentsTotal,
)

fun ExpenseScheduleDto.toDomain(): ExpenseSchedule = ExpenseSchedule(
    id = id,
    userId = userId,
    description = description,
    amountPerOccurrence = BigDecimal(amountPerOccurrence),
    category = ExpenseCategory.valueOf(category),
    recurrence = RecurrenceType.valueOf(recurrence),
    startDate = Instant.parse(startDate),
    endDate = endDate?.let { Instant.parse(it) },
    installmentsTotal = installmentsTotal,
    createdAt = Instant.parse(createdAt),
    generatedCount = generatedCount,
)

fun ExpenseScheduleParams.toRequestDto(): ExpenseScheduleRequestDto = ExpenseScheduleRequestDto(
    description = description,
    amountPerOccurrence = amountPerOccurrence.toPlainString(),
    category = category.name,
    recurrence = recurrence.name,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    installmentsTotal = installmentsTotal,
)
