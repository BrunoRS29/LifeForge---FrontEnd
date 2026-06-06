package com.lifeforge.data.api

import com.lifeforge.data.model.dto.ImportRequestDto
import com.lifeforge.data.model.dto.ImportResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Importação em lote de extratos bancários já classificados no app.
 * Pode enviar dezenas/centenas de lançamentos numa única chamada — por isso
 * o timeout generoso (60s) do OkHttpClient é importante aqui.
 */
interface FinanceImportApi {

    @POST("finance/import")
    suspend fun importTransactions(
        @Body body: ImportRequestDto,
    ): Response<ImportResultDto>
}
