package com.lifeforge.domain.model

/**
 * Perfil estendido do usuário — parâmetros opcionais que melhoram as
 * projeções (Fase 1: coleta e persistência; o consumo pelas simulações vem
 * na Fase 2). Todos os campos são opcionais; valores monetários e percentuais
 * ficam como String (mesma convenção dos outros DTOs) e são interpretados na
 * etapa de cálculo.
 */
data class UserProfile(
    // --- Essenciais (coletados também no registro) ---
    val age: Int? = null,
    val monthlySalary: String? = null,
    val employmentType: EmploymentType? = null,
    val retirementAge: Int? = null,
    val monthlyContribution: String? = null,
    // --- Pessoal ---
    val maritalStatus: MaritalStatus? = null,
    val dependents: Int? = null,
    val childrenAges: String? = null,            // idades dos filhos, CSV ex.: "3, 7"
    val state: String? = null,
    val lifeExpectancy: Int? = null,             // expectativa de vida desejada (anos)
    // --- Profissional ---
    val expectedSalaryGrowth: String? = null,   // % ao ano
    val unemploymentRisk: RiskLevel? = null,
    // --- Moradia ---
    val housingStatus: HousingStatus? = null,
    val housingMonthlyCost: String? = null,      // parcela ou aluguel
    val propertyValue: String? = null,           // valor de mercado do imóvel próprio
    // --- Veículos ---
    val vehiclesValue: String? = null,           // valor de mercado total dos veículos
    // --- Tributação ---
    val taxRegime: TaxRegime? = null,
    // --- Patrimônio / dívidas ---
    val emergencyReserve: String? = null,
    val totalDebt: String? = null,
    // --- Planejamento ---
    val plansChildren: Boolean? = null,
    val plansProperty: Boolean? = null,
) {
    companion object {
        val EMPTY = UserProfile()
    }
}

enum class EmploymentType(val label: String) {
    CLT("CLT"),
    PJ("PJ"),
    ENTREPRENEUR("Empresário(a)"),
    SELF_EMPLOYED("Autônomo(a)"),
    CIVIL_SERVANT("Servidor(a) público(a)"),
}

enum class MaritalStatus(val label: String) {
    SINGLE("Solteiro(a)"),
    MARRIED("Casado(a)"),
    STABLE_UNION("União estável"),
    DIVORCED("Divorciado(a)"),
    WIDOWED("Viúvo(a)"),
}

enum class RiskLevel(val label: String) {
    LOW("Baixo"),
    MEDIUM("Médio"),
    HIGH("Alto"),
}

enum class HousingStatus(val label: String) {
    OWNED("Imóvel próprio (quitado)"),
    FINANCED("Imóvel financiado"),
    RENTED("Alugado"),
}

enum class TaxRegime(val label: String) {
    CLT("CLT"),
    MEI("MEI"),
    SIMPLES("Simples Nacional"),
    LUCRO_PRESUMIDO("Lucro Presumido"),
    LUCRO_REAL("Lucro Real"),
}
