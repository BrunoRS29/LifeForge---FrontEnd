# Gerar o APK e rodar o LifeForge no celular (em qualquer lugar)

O APK instalado no celular é só o **cliente**. O cérebro do sistema — backend
Ktor, microsserviço de IA e PostgreSQL — roda em um **servidor**. Para o app
funcionar fora da sua rede (no 4G, na faculdade, etc.), o servidor precisa de um
**endereço público na internet**.

A forma mais simples para apresentação é um **túnel** com o
[ngrok](https://ngrok.com): ele cria uma URL HTTPS pública apontando para o
backend que roda no seu PC.

```
[ Celular ] --internet--> [ URL ngrok HTTPS ] --túnel--> [ seu PC: Docker (Ktor + IA + Postgres) ]
```

---

## Pré-requisitos (uma vez só)

- **Docker Desktop** instalado e ligado.
- **Conta ngrok** (gratuita) + ngrok instalado. Após criar a conta, rode uma vez:
  `ngrok config add-authtoken SEU_TOKEN` (o token aparece no painel do ngrok).
- A chave de assinatura já foi criada (`lifeforge-release.jks` +
  `keystore.properties`). **Guarde esses dois arquivos** — eles não vão para o
  git e são o que assina o app.

> **Dica:** o plano gratuito do ngrok permite **reservar 1 domínio fixo**
> (`algo.ngrok-free.app`) no painel. Se reservar, a URL nunca muda e você gera o
> APK uma vez só. Sem reservar, a URL muda a cada `ngrok http` e o APK precisa
> ser gerado de novo.

---

## Passo a passo (dia da apresentação)

### 1. Suba o backend
Na pasta do backend:
```bash
docker compose up -d
```
Confirme que respondeu: abra `http://localhost:8080/api/v1/reference-data` no
navegador — deve voltar um JSON.

### 2. Abra o túnel
```bash
ngrok http 8080
```
O ngrok mostra uma linha como:
```
Forwarding  https://a1b2-200-100-50-10.ngrok-free.app -> http://localhost:8080
```
Copie a parte `https://...ngrok-free.app`.

### 3. Cole a URL no app
No arquivo `local.properties` (raiz do front-end), ajuste a linha — **mantendo o
`/api/v1/` no final**:
```properties
API_BASE_URL_RELEASE=https://a1b2-200-100-50-10.ngrok-free.app/api/v1/
```

### 4. Gere o APK
Na pasta do front-end:
```bash
gradlew assembleRelease
```
O APK assinado fica em:
```
app/build/outputs/apk/release/app-release.apk
```

### 5. Instale no celular
- Transfira o `.apk` (cabo USB, Google Drive, WhatsApp Web, etc.).
- No celular, abra o arquivo e permita **"instalar de fontes desconhecidas"**
  quando pedir.
- Abra o app, faça login/registro normalmente.

Pronto — enquanto o **Docker e o ngrok estiverem rodando no seu PC**, o app
funciona de qualquer rede (4G inclusive).

---

## Resolução de problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| App abre mas não loga / "erro de conexão" | URL errada ou ngrok/Docker desligado | Confira a URL no `local.properties` (com `/api/v1/`) e se os dois estão no ar |
| "App não instalado" | Já existe uma versão **debug** instalada | Desinstale a versão antiga (o release tem outro ID, sem o sufixo `.debug`) |
| Trocou de sessão do ngrok e parou | A URL mudou | Cole a nova URL (passo 3) e gere o APK de novo (passo 4) |
| Quero testar sem internet/4G | — | Use a versão **debug** com o celular na **mesma Wi-Fi** apontando para o IP do PC |

---

## E a versão "de produção" (Trabalho Futuro)

O túnel depende do seu PC ligado. Para o app ficar disponível 24h sem isso, o
backend, a IA e o Postgres seriam publicados em um provedor de nuvem
(Railway, Render, Fly.io ou um VPS), gerando uma URL fixa permanente. Aí o APK
aponta para essa URL uma vez e funciona sempre. Esse é o caminho descrito como
trabalho futuro no Capítulo 5.
