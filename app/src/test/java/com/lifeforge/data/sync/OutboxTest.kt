package com.lifeforge.data.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Regras de consolidação da fila de saída: no máximo uma operação por
 * entidade, refletindo só o estado final que precisa chegar ao servidor.
 */
class OutboxTest {

    private val dao = FakePendingOperationDao()
    private val clock = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC)
    private val outbox = Outbox(dao, clock)

    @Test
    fun `criacao offline vira uma operacao CREATE com o corpo do request`() = runTest {
        val id = outbox.enqueueCreate(OutboxEntityType.GOAL, localId = -1, payload = "{v1}")

        val op = dao.findById(id)!!
        assertThat(op.operation).isEqualTo("CREATE")
        assertThat(op.entityId).isEqualTo(-1)
        assertThat(op.payload).isEqualTo("{v1}")
        assertThat(op.createdAt).isEqualTo(Instant.parse("2026-10-02T12:00:00Z"))
    }

    @Test
    fun `editar uma criacao pendente so troca os dados que serao enviados`() = runTest {
        val createId = outbox.enqueueCreate(OutboxEntityType.GOAL, -1, "{v1}")

        val updateId = outbox.enqueueUpdate(OutboxEntityType.GOAL, -1, "{v2}")

        assertThat(updateId).isEqualTo(createId)
        assertThat(dao.snapshot).hasSize(1)
        assertThat(dao.snapshot.single().operation).isEqualTo("CREATE")
        assertThat(dao.snapshot.single().payload).isEqualTo("{v2}")
    }

    @Test
    fun `excluir uma criacao pendente descarta a operacao e nao envia nada`() = runTest {
        outbox.enqueueCreate(OutboxEntityType.GOAL, -1, "{v1}")

        val deleteId = outbox.enqueueDelete(OutboxEntityType.GOAL, -1)

        assertThat(deleteId).isNull()
        assertThat(dao.snapshot).isEmpty()
    }

    @Test
    fun `edicoes sucessivas de um registro sincronizado viram um unico UPDATE`() = runTest {
        outbox.enqueueUpdate(OutboxEntityType.INCOME, 7, "{a}")
        outbox.enqueueUpdate(OutboxEntityType.INCOME, 7, "{b}")

        assertThat(dao.snapshot).hasSize(1)
        assertThat(dao.snapshot.single().operation).isEqualTo("UPDATE")
        assertThat(dao.snapshot.single().payload).isEqualTo("{b}")
    }

    @Test
    fun `excluir depois de editar substitui a edicao pela exclusao`() = runTest {
        outbox.enqueueUpdate(OutboxEntityType.EXPENSE, 9, "{a}")

        val deleteId = outbox.enqueueDelete(OutboxEntityType.EXPENSE, 9)

        assertThat(deleteId).isNotNull()
        val op = dao.snapshot.single()
        assertThat(op.operation).isEqualTo("DELETE")
        assertThat(op.payload).isNull()
    }

    @Test
    fun `entidades diferentes tem operacoes independentes e a contagem e observavel`() = runTest {
        outbox.enqueueCreate(OutboxEntityType.GOAL, -1, "{g}")
        outbox.enqueueUpdate(OutboxEntityType.ASSET, 3, "{a}")
        outbox.enqueueDelete(OutboxEntityType.INCOME, 4)

        assertThat(outbox.observeCount().first()).isEqualTo(3)
        assertThat(outbox.pending().map { it.operation }).containsExactly("CREATE", "UPDATE", "DELETE").inOrder()
    }

    @Test
    fun `falha transitoria incrementa tentativas e guarda o motivo`() = runTest {
        val id = outbox.enqueueCreate(OutboxEntityType.GOAL, -1, "{v1}")

        outbox.recordFailure(id, "sem rede")
        outbox.recordFailure(id, "timeout")

        val op = dao.findById(id)!!
        assertThat(op.attempts).isEqualTo(2)
        assertThat(op.lastError).isEqualTo("timeout")
    }

    @Test
    fun `descartar um tipo remove so as operacoes dele`() = runTest {
        outbox.enqueueUpdate(OutboxEntityType.INCOME, 1, "{i}")
        outbox.enqueueUpdate(OutboxEntityType.EXPENSE, 1, "{e}")

        outbox.discardAll(OutboxEntityType.INCOME)

        assertThat(dao.snapshot.map { it.entityType }).containsExactly("EXPENSE")
    }
}
