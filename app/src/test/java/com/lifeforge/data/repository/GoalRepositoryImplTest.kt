package com.lifeforge.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.lifeforge.data.db.entity.GoalEntity
import com.lifeforge.data.db.entity.UserEntity
import com.lifeforge.data.mapper.toEntity
import com.lifeforge.data.model.dto.GoalDto
import com.lifeforge.data.model.dto.GoalRequestDto
import com.lifeforge.data.sync.DirectTransactionRunner
import com.lifeforge.data.sync.FakeGoalDao
import com.lifeforge.data.sync.FakeGoalRemote
import com.lifeforge.data.sync.FakeLastSyncStore
import com.lifeforge.data.sync.FakeNetworkMonitor
import com.lifeforge.data.sync.FakePendingOperationDao
import com.lifeforge.data.sync.FakeUserDao
import com.lifeforge.data.sync.OfflineWriter
import com.lifeforge.data.sync.Outbox
import com.lifeforge.data.sync.OutboxEntityType
import com.lifeforge.data.sync.RecordingSyncScheduler
import com.lifeforge.data.sync.RemoteEntitySyncHandler
import com.lifeforge.data.sync.SyncEngine
import com.lifeforge.data.sync.SyncState
import com.lifeforge.data.sync.asLocalStore
import com.lifeforge.domain.model.AppError
import com.lifeforge.domain.model.DataResult
import com.lifeforge.domain.model.Goal
import com.lifeforge.domain.model.GoalCategory
import com.lifeforge.domain.model.RiskProfile
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Padrão offline-first do [GoalRepositoryImpl] — e, por meio dele, da fila de
 * saída, do [SyncEngine] e do [OfflineWriter] (mesmo caminho de receitas,
 * despesas e ativos):
 *
 * - escrita com rede sincroniza na hora; sem rede, fica pendente e agendada;
 * - a sincronização troca o id temporário pelo definitivo;
 * - recusas do servidor desfazem a alteração local;
 * - o refresh preserva o que ainda não subiu;
 * - corridas entre a tela e o envio não perdem nem ressuscitam dados.
 */
class GoalRepositoryImplTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val goalDao = FakeGoalDao()
    private val opsDao = FakePendingOperationDao()
    private val remote = FakeGoalRemote()
    private val tx = DirectTransactionRunner()
    private val outbox = Outbox(opsDao, clock)
    private val lastSync = FakeLastSyncStore()
    private val network = FakeNetworkMonitor(online = true)
    private val scheduler = RecordingSyncScheduler()

    private val handler = RemoteEntitySyncHandler(
        type = OutboxEntityType.GOAL,
        store = goalDao.asLocalStore(),
        remote = remote,
        requestSerializer = GoalRequestDto.serializer(),
        toEntity = { it.toEntity() },
        json = json,
        tx = tx,
    )
    private val engine = SyncEngine(outbox, setOf(handler), emptySet(), tx, lastSync, clock)
    private val writer = OfflineWriter(outbox, engine, tx, network, scheduler)
    private val user = UserEntity(1L, "gabriel@exemplo.com", "Gabriel", RiskProfile.MODERATE.name, Instant.EPOCH)

    private val repository = GoalRepositoryImpl(goalDao, FakeUserDao(user), writer, engine, json, clock)

    private suspend fun createSample(name: String = "Aposentadoria"): DataResult<Goal> = repository.create(
        name = name,
        category = GoalCategory.RETIREMENT,
        targetAmount = BigDecimal("1000000.00"),
        targetDate = Instant.parse("2046-12-31T00:00:00Z"),
        priority = 1,
    )

    // ------------------------------------------------------------------------
    // Escrita com e sem rede
    // ------------------------------------------------------------------------

    @Test
    fun `create com rede sincroniza na hora e devolve o id do servidor`() = runTest {
        val result = createSample()

        val goal = (result as DataResult.Success).data
        assertThat(goal.id).isEqualTo(100L)
        assertThat(goal.pendingSync).isFalse()
        assertThat(opsDao.snapshot).isEmpty()
        assertThat(goalDao.snapshot.keys).containsExactly(100L)
        assertThat(remote.server.keys).containsExactly(100L)
    }

    @Test
    fun `create sem rede grava no aparelho com id temporario, enfileira e agenda`() = runTest {
        network.online = false

        val goal = (createSample() as DataResult.Success).data

        assertThat(goal.id).isLessThan(0L)
        assertThat(goal.pendingSync).isTrue()
        assertThat(opsDao.snapshot.single().operation).isEqualTo("CREATE")
        assertThat(scheduler.syncRequests).isEqualTo(1)
        assertThat(remote.creates).isEqualTo(0)
    }

    @Test
    fun `ao sincronizar, o id temporario vira o definitivo e observeById acompanha`() = runTest {
        network.online = false
        val local = (createSample() as DataResult.Success).data

        repository.observeById(local.id).test {
            assertThat(awaitItem()!!.id).isEqualTo(local.id)

            network.online = true
            val synced = engine.syncAll()

            assertThat(synced).isInstanceOf(DataResult.Success::class.java)
            val remapped = awaitItem()!!
            assertThat(remapped.id).isEqualTo(100L)
            assertThat(remapped.pendingSync).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(goalDao.snapshot.keys).containsExactly(100L)
        assertThat(opsDao.snapshot).isEmpty()
        assertThat(lastSync.value).isEqualTo(Instant.parse("2026-10-02T12:00:00Z"))
    }

    @Test
    fun `create recusado pelo servidor desfaz a gravacao local e devolve o erro`() = runTest {
        remote.mode = FakeGoalRemote.Mode.BAD_REQUEST

        val result = createSample()

        assertThat((result as DataResult.Failure).error).isInstanceOf(AppError.Validation::class.java)
        assertThat(goalDao.snapshot).isEmpty()
        assertThat(opsDao.snapshot).isEmpty()
    }

    @Test
    fun `falha transitoria mantem a operacao na fila e registra a tentativa`() = runTest {
        remote.mode = FakeGoalRemote.Mode.SERVER_ERROR

        val goal = (createSample() as DataResult.Success).data

        assertThat(goal.pendingSync).isTrue()
        val op = opsDao.snapshot.single()
        assertThat(op.attempts).isEqualTo(1)
        assertThat(scheduler.syncRequests).isEqualTo(1)
    }

    @Test
    fun `reenvio de uma criacao usa a mesma chave de idempotencia`() = runTest {
        remote.mode = FakeGoalRemote.Mode.LOST_RESPONSE
        createSample()

        remote.mode = FakeGoalRemote.Mode.OK
        engine.pushAll()

        assertThat(remote.idempotencyKeys).hasSize(2)
        assertThat(remote.idempotencyKeys.distinct()).hasSize(1)
    }

    // ------------------------------------------------------------------------
    // Edição e exclusão
    // ------------------------------------------------------------------------

    @Test
    fun `update sem rede fica pendente e sobe na proxima sincronizacao`() = runTest {
        val created = (createSample() as DataResult.Success).data
        network.online = false

        val updated = repository.update(
            id = created.id,
            name = "Aposentadoria antecipada",
            category = GoalCategory.RETIREMENT,
            targetAmount = BigDecimal("1500000.00"),
            targetDate = Instant.parse("2040-12-31T00:00:00Z"),
            priority = 1,
        )

        assertThat((updated as DataResult.Success).data.pendingSync).isTrue()
        assertThat(opsDao.snapshot.single().operation).isEqualTo("UPDATE")

        network.online = true
        engine.syncAll()

        assertThat(remote.server.getValue(created.id).name).isEqualTo("Aposentadoria antecipada")
        assertThat(goalDao.snapshot.getValue(created.id).syncState).isEqualTo(SyncState.SYNCED.name)
    }

    @Test
    fun `delete sem rede some da tela na hora e apaga no servidor ao sincronizar`() = runTest {
        val created = (createSample() as DataResult.Success).data
        network.online = false

        repository.delete(created.id)

        repository.observeAll().test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(remote.server).isNotEmpty()

        network.online = true
        engine.syncAll()

        assertThat(remote.server).isEmpty()
        assertThat(goalDao.snapshot).isEmpty()
    }

    @Test
    fun `delete de meta criada offline nao precisa do servidor`() = runTest {
        network.online = false
        val local = (createSample() as DataResult.Success).data

        repository.delete(local.id)

        assertThat(goalDao.snapshot).isEmpty()
        assertThat(opsDao.snapshot).isEmpty()
        assertThat(remote.creates).isEqualTo(0)
    }

    @Test
    fun `update de meta apagada em outro aparelho remove a copia local`() = runTest {
        val created = (createSample() as DataResult.Success).data
        remote.server.clear() // apagada no servidor por outro aparelho

        val result = repository.update(
            id = created.id,
            name = "x",
            category = GoalCategory.TRAVEL,
            targetAmount = BigDecimal.TEN,
            targetDate = Instant.parse("2030-01-01T00:00:00Z"),
            priority = 2,
        )

        assertThat((result as DataResult.Failure).error).isInstanceOf(AppError.NotFound::class.java)
        assertThat(goalDao.snapshot).isEmpty()
        assertThat(opsDao.snapshot).isEmpty()
    }

    // ------------------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------------------

    @Test
    fun `refresh traz a lista do servidor preservando o que ainda nao subiu`() = runTest {
        val synced = (createSample("Viagem") as DataResult.Success).data
        network.online = false
        val pending = (createSample("Reserva") as DataResult.Success).data
        // Outro aparelho apagou a "Viagem" e criou uma nova meta no servidor.
        remote.server.clear()
        remote.server[500L] = GoalDto(
            500L, 1L, "Casa", "REAL_ESTATE", "300000", "2035-01-01T00:00:00Z", 1, "2026-01-01T00:00:00Z",
        )
        remote.mode = FakeGoalRemote.Mode.OFFLINE

        // Sem rede: refresh falha e o cache fica como está.
        assertThat(repository.refresh()).isInstanceOf(DataResult.Failure::class.java)
        assertThat(goalDao.snapshot.keys).containsExactly(synced.id, pending.id)

        // Com rede: a criação pendente sobe e o cache passa a refletir o servidor.
        remote.mode = FakeGoalRemote.Mode.OK
        network.online = true
        assertThat(repository.refresh()).isInstanceOf(DataResult.Success::class.java)

        val names = goalDao.snapshot.values.map(GoalEntity::name)
        assertThat(names).containsExactly("Casa", "Reserva")
        assertThat(goalDao.snapshot.values.all { it.syncState == SyncState.SYNCED.name }).isTrue()
    }

    @Test
    fun `id temporario ja remapeado nunca e reutilizado por uma nova criacao offline`() = runTest {
        network.online = false
        val first = (createSample("Primeira") as DataResult.Success).data
        network.online = true
        engine.syncAll() // first: -1 → 100 (remapeado)

        network.online = false
        val second = (createSample("Segunda") as DataResult.Success).data

        assertThat(second.id).isLessThan(first.id) // não reaproveita o -1
        assertThat(second.name).isEqualTo("Segunda")
        repository.observeById(first.id).test {
            assertThat(awaitItem()!!.name).isEqualTo("Primeira") // segue para o 100
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------------
    // Corridas entre a tela e o envio
    // ------------------------------------------------------------------------

    @Test
    fun `edicao feita durante o envio da criacao vira UPDATE do id definitivo`() = runTest {
        network.online = false
        val local = (createSample() as DataResult.Success).data
        network.online = true
        // Enquanto a criação está "no ar", o usuário edita a meta.
        remote.duringCreate = {
            remote.duringCreate = null
            outbox.enqueueUpdate(OutboxEntityType.GOAL, local.id, """{"name":"Editada","category":"RETIREMENT","targetAmount":"1","targetDate":"2046-12-31T00:00:00Z"}""")
        }

        engine.pushAll()

        val op = opsDao.snapshot.single()
        assertThat(op.operation).isEqualTo("UPDATE")
        assertThat(op.entityId).isEqualTo(100L)
        assertThat(goalDao.snapshot.getValue(100L).syncState).isEqualTo(SyncState.PENDING_UPDATE.name)

        engine.pushAll()
        assertThat(remote.server.getValue(100L).name).isEqualTo("Editada")
    }

    @Test
    fun `exclusao feita durante o envio da criacao apaga o registro recem-criado`() = runTest {
        network.online = false
        val local = (createSample() as DataResult.Success).data
        network.online = true
        remote.duringCreate = {
            remote.duringCreate = null
            // A tela exclui a meta: a criação pendente é descartada localmente...
            outbox.enqueueDelete(OutboxEntityType.GOAL, local.id)
            goalDao.deleteById(local.id)
        }

        engine.pushAll() // ...mas o servidor já tinha recebido a criação.
        assertThat(opsDao.snapshot.single().operation).isEqualTo("DELETE")

        engine.pushAll()
        assertThat(remote.server).isEmpty()
        assertThat(goalDao.snapshot).isEmpty()
    }
}
