package com.lifeforge.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.lifeforge.data.api.GoalApi
import com.lifeforge.data.db.dao.GoalDao
import com.lifeforge.data.db.entity.GoalEntity
import com.lifeforge.data.model.dto.GoalDto
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.GoalCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response
import java.math.BigDecimal
import java.time.Instant

/**
 * Cobre o padrão offline-first do [GoalRepositoryImpl]:
 *
 * - **observeAll**: Flow do DAO sem tocar na API
 * - **refresh**: API → replaceAll
 * - **create**: API → upsert no DAO
 * - **delete**: API → deleteById
 * - **erros HTTP**: 404 vira NotFound; 401 vira Unauthorized
 */
class GoalRepositoryImplTest {

    private val api: GoalApi = mockk()
    private val dao: GoalDao = mockk(relaxUnitFun = true)
    private val json = Json { ignoreUnknownKeys = true }

    private val repository = GoalRepositoryImpl(api, dao, json)

    // ------------------------------------------------------------------------
    // observeAll — flow puro do DAO
    // ------------------------------------------------------------------------

    @Test
    fun `observeAll mapeia entities do DAO para domain sem tocar na API`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(sampleEntity))

        repository.observeAll().test {
            val emitted = awaitItem()
            assertThat(emitted).hasSize(1)
            assertThat(emitted.first().name).isEqualTo("Aposentadoria")
            assertThat(emitted.first().category).isEqualTo(GoalCategory.RETIREMENT)
            awaitComplete()
        }

        // API jamais é chamada num observe
        coVerify(exactly = 0) { api.list() }
    }

    // ------------------------------------------------------------------------
    // refresh — API → replaceAll no DAO
    // ------------------------------------------------------------------------

    @Test
    fun `refresh com sucesso chama replaceAll no DAO`() = runTest {
        coEvery { api.list() } returns Response.success(listOf(sampleDto))
        val captured = slot<List<GoalEntity>>()
        coEvery { dao.replaceAll(capture(captured)) } returns Unit

        val result = repository.refresh()

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        assertThat(captured.captured).hasSize(1)
        assertThat(captured.captured.first().id).isEqualTo(sampleDto.id)
    }

    @Test
    fun `refresh com erro 401 retorna Unauthorized e nao toca no DAO`() = runTest {
        coEvery { api.list() } returns errorResponse(
            code = 401,
            body = """{"error":"UNAUTHORIZED","message":"token expirou"}""",
        )

        val result = repository.refresh()

        assertThat((result as DataResult.Failure).error)
            .isInstanceOf(AppError.Unauthorized::class.java)
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }

    // ------------------------------------------------------------------------
    // create — API → upsert no DAO
    // ------------------------------------------------------------------------

    @Test
    fun `create com sucesso persiste no DAO e retorna o domain`() = runTest {
        coEvery { api.create(any()) } returns Response.success(sampleDto)
        val captured = slot<GoalEntity>()
        coEvery { dao.upsert(capture(captured)) } returns Unit

        val result = repository.create(
            name = "Aposentadoria",
            category = GoalCategory.RETIREMENT,
            targetAmount = BigDecimal("1500000.00"),
            targetDate = Instant.parse("2055-01-01T00:00:00Z"),
            priority = 1,
        )

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        val goal = (result as DataResult.Success).data
        assertThat(goal.id).isEqualTo(sampleDto.id)
        assertThat(captured.captured.id).isEqualTo(sampleDto.id)
        coVerify(exactly = 1) { dao.upsert(any()) }
    }

    @Test
    fun `create com 400 nao escreve no DAO`() = runTest {
        coEvery { api.create(any()) } returns errorResponse(
            code = 400,
            body = """{"error":"VALIDATION","message":"name deve ser nao-vazio"}""",
        )

        val result = repository.create(
            name = "",
            category = GoalCategory.CUSTOM,
            targetAmount = BigDecimal("100"),
            targetDate = Instant.parse("2030-01-01T00:00:00Z"),
            priority = 1,
        )

        assertThat((result as DataResult.Failure).error)
            .isInstanceOf(AppError.Validation::class.java)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    // ------------------------------------------------------------------------
    // delete — API → deleteById no DAO
    // ------------------------------------------------------------------------

    @Test
    fun `delete com sucesso chama deleteById no DAO`() = runTest {
        coEvery { api.delete(42L) } returns Response.success(204, Unit)

        val result = repository.delete(42L)

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        coVerify(exactly = 1) { dao.deleteById(42L) }
    }

    @Test
    fun `delete com 404 nao toca no DAO`() = runTest {
        coEvery { api.delete(99L) } returns errorResponse(
            code = 404,
            body = """{"error":"NOT_FOUND","message":"meta nao encontrada"}""",
        )

        val result = repository.delete(99L)

        assertThat((result as DataResult.Failure).error)
            .isInstanceOf(AppError.NotFound::class.java)
        coVerify(exactly = 0) { dao.deleteById(any()) }
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private val sampleDto = GoalDto(
        id = 1L,
        userId = 10L,
        name = "Aposentadoria",
        category = "RETIREMENT",
        targetAmount = "1500000.00",
        targetDate = "2055-01-01T00:00:00Z",
        priority = 1,
        createdAt = "2026-05-09T10:00:00Z",
    )

    private val sampleEntity = GoalEntity(
        id = 1L,
        userId = 10L,
        name = "Aposentadoria",
        category = "RETIREMENT",
        targetAmount = BigDecimal("1500000.00"),
        targetDate = Instant.parse("2055-01-01T00:00:00Z"),
        priority = 1,
        createdAt = Instant.parse("2026-05-09T10:00:00Z"),
    )

    private fun <T> errorResponse(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))
}
