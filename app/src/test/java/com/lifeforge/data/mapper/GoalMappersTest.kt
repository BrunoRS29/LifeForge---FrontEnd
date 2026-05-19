package com.lifeforge.data.mapper

import com.google.common.truth.Truth.assertThat
import com.lifeforge.data.model.dto.GoalDto
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Cobre as três conversões de Goal e garante que o pipeline
 * DTO → Entity → Domain → Entity é fiel (idempotente nos campos).
 */
class GoalMappersTest {

    // ------------------------------------------------------------------------
    // DTO → Entity
    // ------------------------------------------------------------------------

    @Test
    fun `GoalDto toEntity converte BigDecimal e Instant corretamente`() {
        val dto = GoalDto(
            id = 42L,
            userId = 7L,
            name = "Aposentadoria",
            category = "RETIREMENT",
            targetAmount = "1500000.00",
            targetDate = "2055-01-01T00:00:00Z",
            priority = 1,
            createdAt = "2026-05-09T10:00:00Z",
        )

        val entity = dto.toEntity()

        assertThat(entity.id).isEqualTo(42L)
        assertThat(entity.userId).isEqualTo(7L)
        assertThat(entity.name).isEqualTo("Aposentadoria")
        assertThat(entity.category).isEqualTo("RETIREMENT")
        assertThat(entity.targetAmount).isEqualTo(BigDecimal("1500000.00"))
        assertThat(entity.targetDate).isEqualTo(Instant.parse("2055-01-01T00:00:00Z"))
        assertThat(entity.priority).isEqualTo(1)
    }

    // ------------------------------------------------------------------------
    // Entity → Domain
    // ------------------------------------------------------------------------

    @Test
    fun `GoalEntity toDomain resolve enum corretamente`() {
        val dto = sampleDto.copy(category = "FINANCIAL_INDEPENDENCE")
        val domain = dto.toEntity().toDomain()

        assertThat(domain.category).isEqualTo(GoalCategory.FINANCIAL_INDEPENDENCE)
    }

    @Test
    fun `GoalEntity toDomain falha em enum desconhecido`() {
        val dto = sampleDto.copy(category = "INVALID_CATEGORY")
        val entity = dto.toEntity()

        // Enum.valueOf lança IllegalArgumentException — capturado pelo
        // mapCatching nos repositórios e mapeado para AppError.Unknown
        assertThat(runCatching { entity.toDomain() }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    // ------------------------------------------------------------------------
    // Round-trip Domain → Entity → Domain
    // ------------------------------------------------------------------------

    @Test
    fun `Goal round-trip Domain - Entity - Domain preserva todos os campos`() {
        val original = Goal(
            id = 99L,
            userId = 1L,
            name = "Casa própria",
            category = GoalCategory.REAL_ESTATE,
            targetAmount = BigDecimal("500000.00"),
            targetDate = Instant.parse("2030-12-31T00:00:00Z"),
            priority = 2,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

        val recovered = original.toEntity().toDomain()

        assertThat(recovered).isEqualTo(original)
    }

    // ------------------------------------------------------------------------
    // Round-trip via DTO (simula ciclo completo de rede + cache)
    // ------------------------------------------------------------------------

    @Test
    fun `Goal round-trip DTO - Entity - Domain preserva semantica`() {
        val dto = sampleDto

        val domain = dto.toEntity().toDomain()

        assertThat(domain.id).isEqualTo(dto.id)
        assertThat(domain.userId).isEqualTo(dto.userId)
        assertThat(domain.name).isEqualTo(dto.name)
        assertThat(domain.category.name).isEqualTo(dto.category)
        assertThat(domain.targetAmount.toPlainString()).isEqualTo(dto.targetAmount)
        assertThat(domain.targetDate.toString()).isEqualTo(dto.targetDate)
        assertThat(domain.priority).isEqualTo(dto.priority)
    }

    // ------------------------------------------------------------------------
    // Request builder
    // ------------------------------------------------------------------------

    @Test
    fun `goalRequestDto formata BigDecimal sem notacao cientifica`() {
        val req = goalRequestDto(
            name = "Viagem",
            category = GoalCategory.TRAVEL,
            targetAmount = BigDecimal("0.00000001"),  // valor minúsculo
            targetDate = Instant.parse("2027-06-01T00:00:00Z"),
            priority = 3,
        )

        assertThat(req.targetAmount).doesNotContain("E")
        assertThat(req.targetAmount).isEqualTo("0.00000001")
        assertThat(req.category).isEqualTo("TRAVEL")
        assertThat(req.targetDate).isEqualTo("2027-06-01T00:00:00Z")
    }

    @Test
    fun `goalRequestDto formata BigDecimal grande sem notacao cientifica`() {
        val req = goalRequestDto(
            name = "Imóvel",
            category = GoalCategory.REAL_ESTATE,
            targetAmount = BigDecimal("1500000000.50"),  // 1.5 bilhão
            targetDate = Instant.parse("2050-01-01T00:00:00Z"),
            priority = 1,
        )

        assertThat(req.targetAmount).doesNotContain("E")
        assertThat(req.targetAmount).isEqualTo("1500000000.50")
    }

    private val sampleDto = GoalDto(
        id = 1L,
        userId = 10L,
        name = "Teste",
        category = "CUSTOM",
        targetAmount = "100000.00",
        targetDate = "2030-01-01T00:00:00Z",
        priority = 1,
        createdAt = "2026-01-01T00:00:00Z",
    )
}
