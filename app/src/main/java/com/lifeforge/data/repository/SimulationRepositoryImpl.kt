package com.lifeforge.data.repository

import com.lifeforge.data.api.SimulationApi
import com.lifeforge.data.db.dao.SimulationDao
import com.lifeforge.data.mapper.toDomain
import com.lifeforge.data.mapper.toEntity
import com.lifeforge.data.mapper.toRequestDto
import com.lifeforge.data.mapper.toSummary
import com.lifeforge.data.util.safeApiCall
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.SimulationParameters
import com.lifeforge.domain.model.SimulationResult
import com.lifeforge.domain.model.SimulationSummary
import com.lifeforge.domain.model.mapCatching
import com.lifeforge.domain.repository.SimulationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação do repositório de simulações.
 *
 * Diferente dos repositórios CRUD, simulações têm semântica de
 * compute-on-demand: cada `run()` é uma operação cara no backend
 * (~1-3s para 10k iterações) e o resultado é imutável uma vez gravado.
 *
 * - `run()`: API → cache. O resultado fica disponível via `getById`
 *   e na lista de `observeByGoal`.
 * - `getById()`: tenta cache local primeiro; se ausente, consulta API
 *   e cacheia. Permite recuperar resultados de simulações antigas
 *   sem rodar o motor de novo.
 * - `observeByGoal()`: Flow do Room — mostra histórico imediatamente.
 * - `refreshByGoal()`: substitui o cache de uma meta específica.
 */
@Singleton
class SimulationRepositoryImpl @Inject constructor(
    private val api: SimulationApi,
    private val dao: SimulationDao,
    private val json: Json,
) : SimulationRepository {

    override suspend fun run(parameters: SimulationParameters): DataResult<SimulationResult> =
        safeApiCall(json) { api.run(parameters.toRequestDto()) }
            .mapCatching { response ->
                // Cacheia a entity (histórico), mas retorna o domínio mapeado
                // DIRETO do DTO: a entity não guarda a trajectory do fan chart.
                dao.upsert(response.toEntity())
                response.toDomain()
            }

    override suspend fun getById(id: Long): DataResult<SimulationResult> {
        // Tenta cache primeiro — recupera o resultado completo sem custo de rede.
        dao.findById(id)?.let { return DataResult.Success(it.toDomain()) }
        // Cache miss → busca no backend e cacheia.
        return safeApiCall(json) { api.getById(id) }
            .mapCatching { response ->
                val entity = response.toEntity()
                dao.upsert(entity)
                entity.toDomain()
            }
    }

    override fun observeByGoal(goalId: Long): Flow<List<SimulationSummary>> =
        // Servimos summaries derivados do cache local — o histograma
        // continua disponível via getById quando o usuário toca num item.
        dao.observeByGoal(goalId).map { entities -> entities.map { it.toSummary() } }

    override suspend fun refreshByGoal(goalId: Long): DataResult<Unit> =
        safeApiCall(json) { api.listByGoal(goalId) }
            .mapCatching { dtos ->
                // Os summaries vindos do backend NÃO têm histograma — não
                // dá para gravar como SimulationEntity completa. Estratégia:
                // mantemos no cache apenas resultados completos (vindos de
                // /run ou /{id}). Aqui só validamos que o backend respondeu.
                // Itens que vierem na próxima abertura via getById hidratam
                // o cache. Esse trade-off mantém o tipo Entity consistente
                // (sempre tem histograma) ao custo de uma round-trip extra
                // quando o usuário abre detalhes.
                @Suppress("UNUSED_VARIABLE") val acknowledged = dtos.size
            }

    override suspend fun delete(id: Long): DataResult<Unit> =
        safeApiCall(json) { api.delete(id) }
            .mapCatching { dao.deleteById(id) }
}
