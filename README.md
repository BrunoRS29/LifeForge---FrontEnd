# LifeForge — App Android

Aplicativo Android nativo (Kotlin + Jetpack Compose) do TCC **LifeForge** —
planejamento de vida com modelagem probabilística, Simulação de Monte Carlo,
otimização e IA preditiva. Consome a API REST do backend Ktor (repositório
`LifeForge---BackEnd`).

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem / Build | Kotlin (K2) 2.1.0 · AGP 8.7.3 · JVM 17 |
| UI | Jetpack Compose (BOM 2025.01.00) · Material 3 |
| Gráficos | Vico (histograma, *fan chart*, gauge de probabilidade) |
| Injeção de dependência | Hilt 2.54 (+ KSP) |
| Rede | Retrofit + OkHttp + conversor kotlinx.serialization |
| Persistência local | Room (cache offline-first) |
| Navegação | Navigation Compose (rotas type-safe `@Serializable`) |
| Assíncrono | Coroutines + Flow |
| Testes | JUnit · Truth · MockK · Turbine · coroutines-test · Room testing |

`minSdk = 26` (Android 8.0) · `targetSdk = compileSdk = 35`.

## Arquitetura (Clean Architecture, offline-first)

```
app/src/main/java/com/lifeforge/
├── data/
│   ├── api/          ← interfaces Retrofit (auth, goals, finance, simulation,
│   │                   optimization, prediction, profile, reference-data, import)
│   ├── db/           ← Room: entities, DAOs, converters
│   ├── repository/   ← RepositoryImpl (rede + cache Room)
│   ├── mapper/       ← DTO ↔ domínio
│   └── model/dto/    ← DTOs de rede
├── domain/
│   ├── model/        ← entidades puras + regras (WealthProjection, ReferenceData…)
│   ├── repository/   ← interfaces de repositório
│   └── usecase/      ← casos de uso
├── di/               ← módulos Hilt (Network, Database, Repository)
└── presentation/
    ├── screen/       ← auth, dashboard, finance, goal, simulation, optimization,
    │                   prediction, profile, imports
    ├── navigation/   ← grafo Navigation Compose
    └── common/       ← componentes e formatadores reutilizáveis
```

Padrão **offline-first**: `observeXxx()` expõe `Flow` do Room (fonte única);
`refreshXxx()` sincroniza com a API e atualiza o cache. `DataResult<T>`
(Success/Failure) + `safeApiCall` padronizam o tratamento de erro.

## Telas

- **Autenticação** — login e registro (com essenciais do perfil).
- **Dashboard** — métricas, evolução patrimonial real × projetada (personalizada
  pelo perfil), **índice FI/RE** (independência financeira) e predição de patrimônio.
- **Finanças** — Receitas / Despesas / Ativos, lançamentos recorrentes, navegador de
  mês e **importação de extratos/faturas** (Nubank).
- **Metas** — lista, detalhe e edição.
- **Simulação** — Monte Carlo clássico e **"Simular com IA" de 1 toque** (premissas
  calibradas pelo perfil), com **cenários** pessimista/realista/otimista, *fan chart*,
  gauge de probabilidade e histograma.
- **Otimização** — aporte ideal, prazo e rebalanceamento.
- **Predições** — projeções de renda/despesa/patrimônio (microsserviço de ML).
- **Perfil** — dados opcionais que refinam as projeções (sobrepõem a base estatística).

## Como rodar

1. Abra a pasta do projeto no **Android Studio** (Ladybug ou superior) e aguarde o
   Gradle sync.
2. Suba o backend localmente (`docker compose up` no repositório do backend) —
   `http://localhost:8080`.
3. Rode em um **emulador** (o app aponta para `http://10.0.2.2:8080/api/v1/` em
   debug, que é o loopback do host no emulador).

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug        # com emulador rodando
./gradlew :app:testDebugUnitTest   # testes unitários
```

A URL do backend pode ser sobrescrita em `local.properties` (não versionado):

```properties
sdk.dir=/caminho/para/o/Android/sdk
# Opcional:
# API_BASE_URL_DEBUG=http://10.0.2.2:8080/api/v1/
```

`networkSecurityConfig` bloqueia *cleartext* em produção mas libera `10.0.2.2`
para desenvolvimento local sem TLS.

## Testes

Testes unitários cobrem mapeadores, conversores do banco local, o interceptador de
autenticação, utilitários (chamada segura de API), o repositório de metas, casos de
uso representativos e a lógica de domínio (projeção patrimonial, base de referência).
Execução: `./gradlew :app:testDebugUnitTest`.
