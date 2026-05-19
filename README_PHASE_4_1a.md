# LifeForge Android — Fase 4.1a (Domínio + Rede)

> **Status:** ✅ Fase 4.1a concluída.
> **Próxima:** Fase 4.1b — Banco local (Room) + RepositoryImpl offline-first.

## O que foi adicionado nesta entrega

### Domínio (`domain/`)

| Arquivo | Conteúdo |
|---------|----------|
| `model/Enums.kt` | `RiskProfile`, `GoalCategory`, `IncomeType`, `ExpenseCategory`, `AssetType` — espelham o backend |
| `model/Entities.kt` | `User`, `Goal`, `Income`, `Expense`, `Asset`, `AuthSession` |
| `model/Simulation.kt` | `SimulationParameters`, `SimulationResult`, `SimulationSummary`, `HistogramBucket` |
| `model/Optimization.kt` | `OptimizationResult`, `IterationStep`, `OptimizationVerification`, `RebalanceResult` + enums |
| `model/DataResult.kt` | Sealed class `DataResult<T>` + `AppError` (Network/Unauthorized/NotFound/Validation/Conflict/Server/Unknown) |
| `repository/Repositories.kt` | 8 interfaces de repositório (Auth, User, Goal, Income, Expense, Asset, Simulation, Optimization) |

A camada de domínio é 100% pura — sem imports de Android, Retrofit, Room ou Hilt. Tudo o que dela depende é Kotlin stdlib + `java.time` + `java.math.BigDecimal`.

### DTOs de rede (`data/model/dto/`)

| Arquivo | Cobre |
|---------|-------|
| `AuthDtos.kt` | `RegisterRequestDto`, `LoginRequestDto`, `AuthResponseDto`, `UserDto`, `ErrorResponseDto` |
| `CrudDtos.kt` | `GoalDto/Request`, `IncomeDto/Request`, `ExpenseDto/Request`, `AssetDto/Request` |
| `SimulationDtos.kt` | `RunSimulationRequestDto`, `SimulationResultResponseDto`, `SimulationSummaryResponseDto`, `HistogramBucketDto` |
| `OptimizationDtos.kt` | 3 requests + `OptimizationResponseDto`, `RebalanceResponseDto`, `IterationStepDto`, `VerificationResultDto` |

Espelham linha-a-linha os DTOs em `com.lifeforge.dto.*` do backend Ktor. Convenções herdadas: `BigDecimal`→String, `Instant`→String ISO-8601, enums→`.name`.

### Retrofit APIs (`data/api/`)

| Interface | Endpoints cobertos |
|-----------|-------------------|
| `AuthApi` | `POST /auth/{register,login}` |
| `UserApi` | `GET /users/me` |
| `GoalApi` | CRUD `/goals` |
| `IncomeApi`, `ExpenseApi`, `AssetApi` | CRUD `/incomes`, `/expenses`, `/assets` |
| `SimulationApi` | `POST /simulation/run`, `GET /simulation/{id}`, `GET /simulation/by-goal/{goalId}`, `DELETE` |
| `OptimizationApi` | `POST /optimize/{contribution,horizon,rebalance}` |

Todos retornam `Response<T>` para que `safeApiCall` consiga inspecionar o status code e o errorBody.

### Auth e Util (`data/auth/` + `data/util/`)

- **`TokenStore`** — DataStore Preferences persistindo o JWT entre sessões. Expõe `tokenFlow: Flow<String?>` e helpers suspend.
- **`AuthInterceptor`** — OkHttp interceptor que injeta `Authorization: Bearer …` em rotas privadas, ignorando `/auth/login` e `/auth/register`.
- **`safeApiCall`** — wrapper inline que converte `Response<T>` + exceções em `DataResult<T>`. Mapeia 400→Validation, 401→Unauthorized, 404→NotFound, 409→Conflict, 5xx→Server.

### Hilt DI (`di/`)

- **`Qualifiers.kt`** — `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` para injeção de `CoroutineDispatcher`.
- **`DispatcherModule`** — provê os três dispatchers como singletons.
- **`NetworkModule`** — provê `Json`, `HttpLoggingInterceptor`, `OkHttpClient`, `Retrofit` e as 8 API interfaces. Timeout de 60s acomoda Monte Carlo de 10k iterações.

### Testes (`src/test/java/`)

- **`SafeApiCallTest`** — 9 cenários cobrindo caminho feliz, mapeamento de cada status code, body de erro malformado, IOException e Throwable genérico.
- **`AuthInterceptorTest`** — 5 cenários: rotas públicas (login/register) sem token, rota privada com token, sem token, token em branco.

## Como validar

```bash
cd android/
./gradlew :app:assembleDebug    # compila tudo
./gradlew :app:testDebugUnitTest # roda os 14 testes desta fase
```

Resultado esperado: build verde + 14/14 testes passando.

## Decisões arquiteturais

- **Sealed class `DataResult` em vez de `kotlin.Result`** — carrega informação semântica de erro (`Network` vs `Validation` etc.) que a UI usa para decidir reação sem inspecionar mensagens.
- **DTO ↔ Domain só na Fase 4.1b** — os mappers ainda não existem porque vivem junto dos `RepositoryImpl`. Aqui apenas separamos os tipos.
- **Não usar `kotlin.Result` da stdlib** — não é serializável e mistura sucesso/erro como Throwable, o que dificulta enum exhaustiveness.
- **`runBlocking` no AuthInterceptor** — aceitável: OkHttp já está em IO thread e DataStore é uma leitura única de arquivo pequeno. Alternativa (suspend interceptor) requer dependência adicional sem ganho prático aqui.
- **Token em DataStore Preferences sem criptografia** — para TCC é aceitável; para produção, encapsular em EncryptedSharedPreferences ou Android Keystore (registrado como TODO no código).
- **Timeout de 60s** — necessário para `optimize/contribution` que faz busca binária com verificação Monte Carlo no fim (pode levar 5-10s).

## O que NÃO está nesta fase (vai para 4.1b)

- Room: database, entities, DAOs, converters
- Mappers DTO ↔ Entity ↔ Domain
- `RepositoryImpl` para todos os 8 repositórios (com padrão offline-first)
- `RepositoryModule` (Hilt) — `@Binds` interface → impl
- `DatabaseModule` (Hilt)
- Sync background com WorkManager (provavelmente fica para Sprint 6)
- Testes de mappers e dos repositórios

## Roadmap atualizado

| Fase | Status | Escopo |
|------|--------|--------|
| 4.0 — Fundação | ✅ | Gradle, manifest, Application |
| 4.1a — Domínio + Rede | ✅ | Domain models, DTOs, Retrofit, Hilt network |
| 4.1b — Banco + Repositórios | ⏳ | Room, mappers, RepositoryImpl, Hilt DB |
| 4.2 — Domínio UI + Navegação | ⏳ | UseCases, tema MD3, navegação |
| 4.3 — Telas de cadastro | ⏳ | Auth, Dashboard, Goals, Profile |
| 4.4 — Simulação + gráficos | ⏳ | SimulationScreen, Vico charts |
