package com.lifeforge.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow

/**
 * Testa a projeção determinística de patrimônio (fórmula 6.1 do TCC).
 * Garante que o gráfico do Dashboard usa a fórmula correta de juros
 * compostos com aportes.
 */
class WealthProjectionTest {

    @Test
    fun `t zero retorna o patrimonio inicial`() {
        val series = WealthProjection.project(
            initial = 10_000.0,
            monthlyContribution = 500.0,
            annualReturnRate = 0.08,
            months = 12,
        )
        assertThat(series.first()).isWithin(1e-6).of(10_000.0)
    }

    @Test
    fun `retorna months mais 1 pontos`() {
        val series = WealthProjection.project(0.0, 100.0, 0.08, 60)
        assertThat(series).hasSize(61)
    }

    @Test
    fun `sem rendimento e apenas soma dos aportes`() {
        // r = 0: P(t) = P0 + A*t
        val series = WealthProjection.project(
            initial = 1_000.0,
            monthlyContribution = 200.0,
            annualReturnRate = 0.0,
            months = 10,
        )
        assertThat(series[10]).isWithin(1e-6).of(1_000.0 + 200.0 * 10)
    }

    @Test
    fun `com rendimento bate a formula de juros compostos com aporte`() {
        val initial = 10_000.0
        val contribution = 1_000.0
        val annual = 0.12
        val months = 24

        val series = WealthProjection.project(initial, contribution, annual, months)

        val r = (1.0 + annual).pow(1.0 / 12.0) - 1.0
        val growth = (1.0 + r).pow(months.toDouble())
        val expected = initial * growth + contribution * (growth - 1.0) / r

        assertThat(series[months]).isWithin(0.01).of(expected)
        // Com rendimento positivo, o patrimônio final supera a soma dos aportes.
        assertThat(series[months]).isGreaterThan(initial + contribution * months)
    }

    // ------------------------------------------------------------------------
    // Projeção dinâmica (Fase 2: personalizada pelo perfil)
    // ------------------------------------------------------------------------

    @Test
    fun `dinamica retorna months mais 1 pontos comecando no patrimonio atual`() {
        val s = WealthProjection.projectDynamic(
            ProjectionInputs(
                initialWealth = 10_000.0, monthlyIncome = 5_000.0, monthlyExpenses = 3_000.0,
                annualReturn = 0.09, annualSalaryGrowth = 0.05, annualInflation = 0.045, months = 120,
            )
        )
        assertThat(s.projected).hasSize(121)
        assertThat(s.contributionsOnly).hasSize(121)
        assertThat(s.projected.first()).isWithin(1e-6).of(10_000.0)
        assertThat(s.contributionsOnly.first()).isWithin(1e-6).of(10_000.0)
    }

    @Test
    fun `dinamica sem crescimento nem rendimento e soma simples dos aportes`() {
        val s = WealthProjection.projectDynamic(
            ProjectionInputs(
                initialWealth = 1_000.0, monthlyIncome = 2_000.0, monthlyExpenses = 1_500.0,
                annualReturn = 0.0, annualSalaryGrowth = 0.0, annualInflation = 0.0, months = 10,
            )
        )
        // aporte fixo = 500/mês
        assertThat(s.finalContributionsOnly).isWithin(1e-6).of(1_000.0 + 500.0 * 10)
        assertThat(s.finalProjected).isWithin(1e-6).of(1_000.0 + 500.0 * 10)
    }

    @Test
    fun `dinamica com rendimento supera apenas os aportes`() {
        val s = WealthProjection.projectDynamic(
            ProjectionInputs(
                initialWealth = 10_000.0, monthlyIncome = 5_000.0, monthlyExpenses = 3_000.0,
                annualReturn = 0.10, annualSalaryGrowth = 0.0, annualInflation = 0.0, months = 120,
            )
        )
        assertThat(s.finalProjected).isGreaterThan(s.finalContributionsOnly)
    }

    @Test
    fun `crescimento salarial aumenta os aportes acumulados`() {
        val flat = WealthProjection.projectDynamic(
            ProjectionInputs(0.0, 5_000.0, 3_000.0, 0.0, 0.0, 0.0, 60)
        )
        val growing = WealthProjection.projectDynamic(
            ProjectionInputs(0.0, 5_000.0, 3_000.0, 0.0, 0.10, 0.0, 60)
        )
        assertThat(growing.finalContributionsOnly).isGreaterThan(flat.finalContributionsOnly)
    }

    @Test
    fun `retorno por perfil de risco e crescente do conservador ao arrojado`() {
        val cons = WealthProjection.returnForRiskProfile(RiskProfile.CONSERVATIVE)
        val mod = WealthProjection.returnForRiskProfile(RiskProfile.MODERATE)
        val agg = WealthProjection.returnForRiskProfile(RiskProfile.AGGRESSIVE)
        assertThat(cons).isLessThan(mod)
        assertThat(mod).isLessThan(agg)
        assertThat(WealthProjection.returnForRiskProfile(null)).isEqualTo(mod)
    }

    // ------------------------------------------------------------------------
    // Ativos reais e custo de filhos (diferenciais)
    // ------------------------------------------------------------------------

    @Test
    fun `imovel valoriza e entra no patrimonio projetado`() {
        val base = ProjectionInputs(
            initialWealth = 10_000.0, monthlyIncome = 5_000.0, monthlyExpenses = 3_000.0,
            annualReturn = 0.0, annualSalaryGrowth = 0.0, annualInflation = 0.0, months = 120,
        )
        val comImovel = base.copy(initialPropertyValue = 100_000.0, annualPropertyAppreciation = 0.06)
        val semImovel = WealthProjection.projectDynamic(base).finalProjected
        val comImovelFinal = WealthProjection.projectDynamic(comImovel).finalProjected
        // O imóvel entra no patrimônio e ainda valoriza: final > base + valor inicial.
        assertThat(comImovelFinal).isGreaterThan(semImovel + 100_000.0)
    }

    @Test
    fun `veiculo deprecia cerca de dez por cento ao ano`() {
        val s = WealthProjection.projectDynamic(
            ProjectionInputs(
                initialWealth = 0.0, monthlyIncome = 0.0, monthlyExpenses = 0.0,
                annualReturn = 0.0, annualSalaryGrowth = 0.0, annualInflation = 0.0, months = 12,
                initialVehiclesValue = 50_000.0, annualVehicleDepreciation = 0.10,
            )
        )
        // Sem renda/rendimento, o patrimônio é só o veículo: ~90% após 1 ano.
        assertThat(s.finalProjected).isWithin(1.0).of(45_000.0)
    }

    @Test
    fun `custo de filhos reduz o patrimonio projetado`() {
        val base = ProjectionInputs(
            initialWealth = 10_000.0, monthlyIncome = 5_000.0, monthlyExpenses = 3_000.0,
            annualReturn = 0.08, annualSalaryGrowth = 0.0, annualInflation = 0.0, months = 120,
        )
        val comFilho = base.copy(
            childrenAges = listOf(5),
            childCostByAge = listOf(ChildCostBracket(ageMaxInclusive = 18, monthlyCost = 1_000.0)),
        )
        val semFilho = WealthProjection.projectDynamic(base).finalProjected
        val comFilhoFinal = WealthProjection.projectDynamic(comFilho).finalProjected
        assertThat(comFilhoFinal).isLessThan(semFilho)
    }

    @Test
    fun `divida reduz o patrimonio liquido projetado`() {
        val base = ProjectionInputs(
            initialWealth = 10_000.0, monthlyIncome = 5_000.0, monthlyExpenses = 3_000.0,
            annualReturn = 0.08, annualSalaryGrowth = 0.0, annualInflation = 0.0, months = 60,
        )
        val comDivida = base.copy(initialDebt = 20_000.0)
        val semDivida = WealthProjection.projectDynamic(base).finalProjected
        val comDividaFinal = WealthProjection.projectDynamic(comDivida).finalProjected
        // Passivo estatico: reduz o liquido em exatamente o valor da divida.
        assertThat(comDividaFinal).isWithin(1e-6).of(semDivida - 20_000.0)
    }
}
