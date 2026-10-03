package com.lifeforge.presentation.screen.finance

import com.google.common.truth.Truth.assertThat
import com.lifeforge.domain.model.Asset
import com.lifeforge.domain.model.AssetType
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/** Alocação da carteira por tipo de ativo (barra e legenda da aba Ativos). */
class AssetAllocationTest {

    private fun asset(id: Long, type: AssetType, value: String) = Asset(
        id = id, userId = 1, name = "Ativo $id", assetType = type, currentValue = BigDecimal(value),
        expectedReturn = BigDecimal("0.08"), volatility = BigDecimal("0.15"), createdAt = Instant.EPOCH,
    )

    @Test
    fun `soma por tipo e ordena do maior para o menor`() {
        val slices = assetAllocation(
            listOf(
                asset(1, AssetType.STOCKS, "20000"),
                asset(2, AssetType.FIXED_INCOME, "50000"),
                asset(3, AssetType.STOCKS, "10000"),
                asset(4, AssetType.CRYPTO, "20000"),
            ),
        )
        assertThat(slices.map { it.type }).containsExactly(AssetType.FIXED_INCOME, AssetType.STOCKS, AssetType.CRYPTO).inOrder()
        assertThat(slices.map { it.share }).containsExactly(0.5, 0.3, 0.2).inOrder()
        assertThat(slices[1].value).isEqualToIgnoringScale(BigDecimal("30000"))
    }

    @Test
    fun `carteira vazia ou zerada nao tem fatias`() {
        assertThat(assetAllocation(emptyList())).isEmpty()
        assertThat(assetAllocation(listOf(asset(1, AssetType.OTHER, "0")))).isEmpty()
    }

    @Test
    fun `participacao com uma casa decimal`() {
        assertThat(percentText(0.4567)).isEqualTo("45,7%")
        assertThat(percentText(0.5)).isEqualTo("50%")
    }
}
