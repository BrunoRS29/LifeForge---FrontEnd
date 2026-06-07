package com.lifeforge.data.model.dto

import kotlinx.serialization.Serializable

/**
 * Contrato do perfil estendido (GET/PUT /api/v1/profile). O backend guarda
 * isto como um blob JSON; aqui definimos o formato tipado. Todos os campos são
 * opcionais — enums e valores monetários/percentuais viajam como String.
 */
@Serializable
data class UserProfileDto(
    val age: Int? = null,
    val monthlySalary: String? = null,
    val employmentType: String? = null,
    val retirementAge: Int? = null,
    val monthlyContribution: String? = null,
    val maritalStatus: String? = null,
    val dependents: Int? = null,
    val state: String? = null,
    val expectedSalaryGrowth: String? = null,
    val unemploymentRisk: String? = null,
    val housingStatus: String? = null,
    val housingMonthlyCost: String? = null,
    val taxRegime: String? = null,
    val emergencyReserve: String? = null,
    val totalDebt: String? = null,
    val plansChildren: Boolean? = null,
    val plansProperty: Boolean? = null,
)
