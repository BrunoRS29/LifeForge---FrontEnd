# LifeForge — App Android

Aplicativo Android nativo (Kotlin + Jetpack Compose) do TCC **LifeForge** —
planejamento de vida com modelagem probabilística, Simulação de Monte Carlo,
otimização e IA preditiva. Consome a API REST do backend Ktor (repositório
`LifeForge---BackEnd`).

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem / Build | Kotlin 2.1 (K2) · AGP 8.7 · Gradle 9.8 · JVM 17 |
| UI | Jetpack Compose (BOM 2025.10) · **Material 3 Expressive** (`material3` 1.5.0-alpha14) · navegação adaptativa (barra / trilho) · tema da marca gerado pelo Material Color Utilities (claro/escuro, contraste padrão/médio/alto, cores dinâmicas opcionais) |
| Gráficos | Vico 2 (*fan chart* em faixas, histograma, linhas com área) + Canvas (gauge de probabilidade) |
| Injeção de dependência | Hilt 2.54 (+ KSP) |
| Rede | Retrofit + OkHttp (interceptador JWT) + kotlinx.serialization |
| Persistência local | Room 2.6 (fonte única de verdade, esquema exportado em `app/schemas/`) |
| Sincronização | WorkManager (`SyncWorker`, Hilt Worker) |
| Preferências | DataStore (token JWT, tema, última sincronização) |
| Navegação | Navigation Compose (rotas type-safe `@Serializable`) |
| Assíncrono | Coroutines + Flow |
| Testes | JUnit 4 · Truth · MockK · Turbine · coroutines-test · MockWebServer |

`minSdk = 26` (Android 8.0) · `targetSdk = compileSdk = 35`.

## Arquitetura (Clean Architecture + MVVM, offline-first)

```
app/src/main/java/com/lifeforge/
├── data/
│   ├── api/          ← interfaces Retrofit
│   ├── db/           ← Room: entidades, DAOs, conversores, migrações (v1 → v2)
│   ├── sync/         ← fila de saída (Outbox), SyncEngine, OfflineWriter,
│   │                   SyncWorker, agendador WorkManager, monitor de rede
│   ├── repository/   ← implementações (rede + Room)
│   ├── mapper/       ← DTO ↔ entidade ↔ domínio
│   └── model/dto/    ← DTOs de rede (espelham o backend)
├── domain/           ← Kotlin puro: modelos e regras (projeção patrimonial,
│                       saúde das metas, comparação de estratégias, SUS),
│                       interfaces de repositório e casos de uso
├── di/               ← módulos Hilt (Network, Database, Repository, Sync, Dispatcher)
└── presentation/     ← telas (Screen + ViewModel com StateFlow imutável),
                        navegação, tema e componentes reutilizáveis
```

### Offline-first

- **Leitura:** as telas observam `Flow` do Room — renderizam na hora, sem rede.
- **Escrita:** criar/editar/excluir grava no Room e na fila de saída
  (`pending_operations`) na mesma transação e tenta enviar na hora. Sem conexão,
  o registro fica marcado como pendente (criações usam id temporário negativo) e o
  `SyncWorker` envia quando a rede volta, com recuo exponencial.
- **Fila consolidada:** uma operação por entidade (criar + editar = criar com os
  dados novos; criar + excluir = nada a enviar). Criações levam `Idempotency-Key`,
  então um reenvio após resposta perdida não duplica o registro no servidor.
- **Recusas** do servidor (validação, registro apagado em outro aparelho) desfazem
  a alteração local; o **refresh** preserva o que ainda não subiu.
- **Sessão:** token expirado volta ao login sem apagar o banco — ao entrar de novo
  com o mesmo usuário, o cache e as pendências continuam lá.
- Simulação, otimização, predições, agendamentos e importação dependem do servidor;
  o histórico de simulações e a saúde das metas ficam em cache e abrem offline.

## Telas

- **Autenticação** — login e registro (com os essenciais do perfil).
- **Painel** — patrimônio, renda e despesa do mês, **saúde das metas** (última
  simulação: no caminho / atenção / em risco), **patrimônio realizado × projetado**
  (12 meses reconstruídos pelo fluxo de caixa + projeção personalizada pelo perfil),
  índice de independência financeira (FI/RE), recorrências e atalho para as
  predições.
- **Finanças** — receitas e despesas do mês agrupadas por dia (seletor de mês e
  ano, filtro por categoria, deslizar para excluir), ativos com a alocação por tipo,
  lançamentos recorrentes, importação de extratos/faturas e marca de itens
  aguardando envio.
- **Metas** — lista com selo de saúde, detalhe com a última simulação, edição.
- **Simulação** — Monte Carlo com gauge de probabilidade, cenários
  pessimista/realista/otimista (dizendo se atingem a meta), *fan chart* com as
  faixas P10–P90 e P25–P75, histograma destacando o que atinge a meta,
  estatísticas, percentis, histórico e **comparação lado a lado de estratégias**
  (veredito, resultados e premissas).
- **Simular com IA** — um toque; aporte calibrado pelas predições, com a origem de
  cada premissa (modelo, perfil ou média do histórico).
- **Otimizar** — aporte ideal e prazo ajustado (busca binária, com o passo a
  passo em gráfico e tabela) e carteira sugerida para o perfil de risco.
- **Predições** — renda, despesas e patrimônio (realizado × projetado), com MAE,
  RMSE e R².
- **Perfil** — dados para projeções, tema, cores dinâmicas, sincronização (estado,
  pendências, última sincronização) e **avaliação de usabilidade (SUS)**: tarefas
  cronometradas, questionário de 10 itens, resultados agregados e exportação CSV.

## Interface (Material 3 Expressive)

A interface segue as diretrizes do Material 3 Expressive e do *Core App Quality*
do Android. As peças compartilhadas ficam em `presentation/common` e todas as
telas as reutilizam:

| Peça | Arquivo | Uso |
| --- | --- | --- |
| Tema, formas, tipografia (algarismos tabulares) e movimento expressivo | `theme/` | `MaterialExpressiveTheme` com os esquemas gerados pelo MCU |
| Barras superiores que recolhem com a rolagem | `DesignSystem.kt` | `TabTopAppBar` (abas) e `DetailTopAppBar` (detalhes e formulários) |
| Listas segmentadas, seções, cartões e seção recolhível | `DesignSystem.kt` | `ListGroup`, `FormSection`, `ContentCard`, `ExpandableSection` |
| Ações fixas na base, acima do teclado | `DesignSystem.kt` | `BottomActionBar`, `ProgressActionBar` (com indicador ondulado) |
| Escolha única em botões conectados | `DesignSystem.kt` | `ConnectedChoice` (tema, perfil de risco, modos, horizontes) |
| Campos | `CurrencyField.kt`, `FormFields.kt`, `DatePickerField.kt`, `EnumDropdown.kt` | `MoneyField` (R$ fixo e milhares ao digitar), `PercentField`, `MonthsField`, `DateField`, menu com ícones, `ToggleRow` |
| Gráficos | `ChartStyles.kt`, `ChartLegend.kt`, `AllocationBar.kt` | linhas sólida/tracejada/com área, bordas de faixa do *fan chart*, barra de alocação |

Princípios aplicados: margens de 16 dp e conteúdo limitado a 840 dp em telas
largas; uma ação principal por tela, ao alcance do polegar; divulgação
progressiva (premissas já preenchidas ficam recolhidas, com resumo); taxas
digitadas em porcentagem; alvos de toque ≥ 48 dp e contraste ≥ 4,5:1; resumo
textual (`contentDescription`) em cada gráfico; tipos de conteúdo para o
preenchimento automático no login e no cadastro; estados de carregamento, vazio
e erro em toda tela. A navegação e as faixas globais se recolhem com o teclado
aberto.

Por que `material3` 1.5.0-alpha14: é a versão mais recente com os componentes
expressivos (botões conectados, listas segmentadas, indicadores ondulados, barras
flexíveis) que compila com o `compileSdk 35` e o AGP 8.7 do projeto; as versões
seguintes exigem um `compileSdk` e um AGP mais novos.

## Como rodar

1. Abra a pasta no **Android Studio** e aguarde o Gradle sync.
2. Suba o backend (`docker compose up -d` no repositório do backend) — porta 8080.
3. Rode num **emulador**: em debug o app aponta para `http://10.0.2.2:8080/api/v1/`
   (loopback do computador visto pelo emulador).

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug        # com emulador rodando
./gradlew :app:testDebugUnitTest   # testes unitários
```

A URL do backend pode ser sobrescrita em `local.properties` (não versionado):

```properties
sdk.dir=/caminho/para/o/Android/sdk
# API_BASE_URL_DEBUG=http://10.0.2.2:8080/api/v1/
# API_BASE_URL_RELEASE=https://<seu-tunel>.ngrok-free.app/api/v1/
```

APK para o celular (fora da rede local): ver `docs/gerar-apk.md`.

## Testes

Testes unitários (JVM) cobrem: fila de saída e motor de sincronização (com DAOs e
API falsos em memória — rede/sem rede, recusas, idempotência, corridas e ids
temporários), repositórios, mapeadores e conversores do Room, interceptador de
autenticação, chamada segura de API, casos de uso, projeção e reconstrução do
patrimônio, saúde das metas, comparação de estratégias, pontuação SUS e
exportação CSV, e ViewModels representativos. Execução:
`./gradlew :app:testDebugUnitTest` (também no CI a cada push).
