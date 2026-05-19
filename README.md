# LifeForge Android — Sprint 4

Módulo Android nativo (Kotlin + Jetpack Compose) do TCC LifeForge.

> **Status atual:** Fase 4.0 — Fundação concluída.
> **Próxima:** Fase 4.1 — Camada de dados (Retrofit + Room + Hilt modules).

---

## Stack desta fase

| Camada | Tecnologia | Versão |
|--------|------------|--------|
| Build  | Android Gradle Plugin | 8.7.3 |
| Linguagem | Kotlin (K2) | 2.1.0 |
| UI | Jetpack Compose (BOM) | 2025.01.00 |
| DI | Hilt | 2.54 |
| Annotation processing | KSP | 2.1.0-1.0.29 |
| Logging | Timber | 5.0.1 |

`minSdk = 26` (Android 8.0) · `targetSdk = compileSdk = 35` · `JVM target = 17`

## Estrutura de pastas (Clean Architecture)

```
app/src/main/java/com/lifeforge/
├── LifeForgeApplication.kt        ← entrypoint Hilt + Timber
├── di/                            ← módulos Hilt (Fase 4.1)
├── data/
│   ├── api/                       ← Retrofit interfaces
│   ├── db/                        ← Room entities + DAOs
│   ├── repository/                ← RepositoryImpl
│   └── model/                     ← DTOs de rede e mappers
├── domain/
│   ├── model/                     ← entidades de domínio puras
│   ├── repository/                ← interfaces de repositório
│   └── usecase/                   ← casos de uso (Fase 4.2)
└── presentation/
    ├── MainActivity.kt            ← single-activity host
    ├── screens/
    │   ├── dashboard/
    │   ├── goals/
    │   ├── simulation/
    │   └── profile/
    ├── components/                ← componentes reutilizáveis
    ├── navigation/                ← grafo Navigation Compose
    └── theme/                     ← tema Material 3 do LifeForge
```

Toda pasta vazia carrega um `.gitkeep` para sobreviver ao versionamento.

## Como instalar os arquivos

1. Crie um diretório `android/` na raiz do repositório do TCC (ao lado da pasta `backend/`).
2. Copie todo o conteúdo deste pacote para dentro de `android/`.
3. Abra o Android Studio (Hedgehog ou superior) e selecione **Open** apontando para `android/`.
4. Aguarde o Gradle sync. Será necessário baixar o wrapper (~150 MB na primeira execução).
5. Crie um arquivo `local.properties` em `android/` (não versionado) com:

```properties
sdk.dir=/caminho/para/seu/Android/sdk
# Opcional — sobrescreve a URL padrão do backend
# API_BASE_URL_DEBUG=http://10.0.2.2:8080/api/v1/
```

> O Android Studio gera o `sdk.dir` automaticamente ao abrir o projeto.

## Verificando o build

Com o backend Ktor rodando localmente em `http://localhost:8080`:

```bash
# Na pasta android/
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # com emulador rodando
```

O app deve abrir mostrando a tela de boot **"LifeForge — Fase 4.0 OK"**.
Isso valida que: AGP, Kotlin K2, Compose Compiler, KSP, Hilt e
Kotlinx Serialization estão todos integrados corretamente.

## Decisões arquiteturais desta fase

- **Single-activity** com `MainActivity` hospedando todo o grafo Compose. Evita complexidade de fragments/intents internos.
- **Version catalog** (`gradle/libs.versions.toml`) centraliza todas as versões e bundles, facilitando upgrades futuros e mantendo o `build.gradle.kts` enxuto.
- **Bundles** agrupam dependências relacionadas (`compose`, `lifecycle`, `network`, `room`, `vico`, `unit-test`, `android-test`) para diminuir ruído no app module.
- **KSP** sobre KAPT — Hilt e Room usam KSP, que é ~2x mais rápido em projetos K2.
- **`networkSecurityConfig`** bloqueia cleartext em produção mas libera para `10.0.2.2` (loopback do emulador), permitindo dev local sem TLS.
- **`local.properties`** isola `API_BASE_URL` por ambiente sem expor segredos no Git.
- **ProGuard rules** mínimas mas suficientes para Kotlinx Serialization, Retrofit, OkHttp e Hilt — suficiente para R8 release builds.
- **`HiltViewModel` + `hiltNavigationCompose`** já no catálogo: ViewModels serão criados via `hiltViewModel()` direto nas Composables na Fase 4.2.

## O que NÃO está nesta fase

Itens deliberadamente adiados — entram nas próximas fases:

- Módulos Hilt (`NetworkModule`, `DatabaseModule`, `RepositoryModule`) → Fase 4.1
- Retrofit API interfaces, Room entities/DAOs, repositórios → Fase 4.1
- Tema Material 3 LifeForge (cores, tipografia) e grafo de navegação → Fase 4.2
- UseCases e domain models → Fase 4.2
- Telas Compose reais → Fases 4.3 e 4.4
- Gráficos Vico (histograma, fan chart, gauge) → Fase 4.4
- Wrapper do Gradle (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) — gerado automaticamente pelo Android Studio na primeira abertura

## Roadmap da Sprint 4

| Fase | Status | Escopo |
|------|--------|--------|
| 4.0 — Fundação | ✅ | Gradle, manifest, Application, estrutura |
| 4.1 — Camada de dados | ⏳ | Retrofit, Room, RepositoryImpl, módulos Hilt |
| 4.2 — Domínio + UI foundation | ⏳ | UseCases, tema MD3, navegação |
| 4.3 — Telas básicas | ⏳ | Auth, Dashboard, Goals, Profile |
| 4.4 — Simulação + gráficos | ⏳ | SimulationScreen, ResultScreen, Vico |
