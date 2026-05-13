# api-cobranca-bilhetagem

Microserviço de cobranças de bilhetagem. Suporta criação de cobranças via **PIX** ou **Cartão de Crédito**, consulta de status, processamento de webhooks PIX e validação de checkout 3DS.

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| Java (JDK) | 17 |
| Maven | 3.8+ |
| Docker + Docker Compose | qualquer versão recente |

> Maven pode ser instalado via Chocolatey (`choco install maven`) ou manualmente em [maven.apache.org](https://maven.apache.org/download.cgi).

---

## Subindo as dependências locais (Docker)

O projeto inclui um `docker-compose.yml` que sobe PostgreSQL, Redis e Kafka com um único comando. Nenhuma instalação manual dessas ferramentas é necessária.

```bash
# Inicia PostgreSQL, Redis e Kafka em background
docker compose up -d

# Verifica se os containers estão healthy
docker compose ps

# Para tudo (mantém os dados nos volumes)
docker compose down

# Para tudo e apaga os dados
docker compose down -v
```

> Kafka pode levar ~30 segundos para ficar `healthy` na primeira inicialização.

Portas expostas:

| Serviço | Porta |
|---|---|
| PostgreSQL | `5432` |
| Redis | `6379` |
| Kafka | `9092` |

---

## Build

```bash
# Linux / macOS
mvn clean package -DskipTests
# ou: ./mvnw clean package -DskipTests

# Windows
mvn clean package -DskipTests
# ou: mvnw.cmd clean package -DskipTests
```

O artefato gerado estará em:

```
target/api-cobranca-bilhetagem-1.0.0-SNAPSHOT.jar
```

---

## Execução local

O projeto inclui scripts de execução para Linux e Windows que fazem o build automaticamente se o JAR não existir.

**Linux / macOS:**
```bash
chmod +x run.sh

# Executa (build automático se o JAR não existir)
./run.sh

# Força rebuild antes de iniciar
./run.sh --build
```

**Windows:**
```cmd
# Executa (build automático se o JAR não existir)
run.bat

# Força rebuild antes de iniciar
run.bat --build
```

A aplicação sobe na porta `8080` por padrão.

### Fluxo completo recomendado

```bash
# 1. Sobe as dependências
docker compose up -d

# 2. Aguarda os containers ficarem healthy, então inicia a API
./run.sh --build   # ou run.bat --build no Windows
```

---

## Execução de testes

```bash
# Linux / macOS
mvn test
# ou, se o wrapper estiver gerado:
./mvnw test

# Windows
mvn test
# ou, se o wrapper estiver gerado:
mvnw.cmd test
```

Para rodar uma classe específica:
```bash
mvn test -Dtest=CobrancaServiceTest
```

Para rodar um método específico:
```bash
mvn test -Dtest=CobrancaServiceTest#criarCobrancaPixComSucessoDeveSalvarERetornarCamposEsperados
```

Os testes usam o perfil `test`, que ativa H2 em memória e implementações fake para os clientes externos. Nenhuma dependência externa é necessária para rodar os testes.

---

## Execução com cobertura

```bash
# Linux / macOS
mvn verify
# ou: ./mvnw verify

# Windows
mvn verify
# ou: mvnw.cmd verify
```

O plugin JaCoCo verifica cobertura mínima de **70% de linhas** no pacote `service`. O build falha se a cobertura estiver abaixo do limite.

O relatório HTML de cobertura é gerado em:

```
target/site/jacoco/index.html
```

---

## Variáveis de ambiente

Todas as variáveis têm valores padrão compatíveis com o `docker-compose.yml` incluído, então nenhuma configuração extra é necessária para rodar localmente.

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `cobranca` | Nome do banco de dados |
| `DB_USER` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `REDIS_HOST` | `localhost` | Host do Redis (lock distribuído) |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Endereço do broker Kafka |
| `SERVER_PORT` | `8080` | Porta HTTP da aplicação |

Para sobrescrever, defina antes de executar:

```bash
# Linux / macOS
DB_PASSWORD=outra_senha SERVER_PORT=9090 ./run.sh

# Windows
set DB_PASSWORD=outra_senha & set SERVER_PORT=9090 & run.bat
```

> Em ambiente de testes (`-Dspring.profiles.active=test`), Redis, Kafka e PostgreSQL são substituídos por implementações fake e H2 em memória — nenhuma dependência externa é necessária.

---

## Testando os endpoints localmente

Base path: `/api/v1/cobrancas`

> Os exemplos abaixo usam `curl` no Windows CMD. No Linux/macOS remova as barras invertidas e use aspas simples.

---

### 1. Criar cobrança PIX

```cmd
curl -X POST http://localhost:8080/api/v1/cobrancas -H "Content-Type: application/json" -d "{\"valor\": 50.00, \"tipo\": \"RECARGA\", \"metodo\": \"PIX\"}"
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "txid": "txid-3f2a1b4c-...",
  "copiaECola": "00020126...",
  "dataExpiracao": "2026-05-13T14:00:00",
  "transactionId": null
}
```

---

### 2. Criar cobrança Cartão de Crédito

```cmd
curl -X POST http://localhost:8080/api/v1/cobrancas -H "Content-Type: application/json" -d "{\"valor\": 150.00, \"tipo\": \"RECARGA\", \"metodo\": \"CARTAO_CREDITO\"}"
```

**Response `201 Created`:**
```json
{
  "id": 2,
  "txid": null,
  "copiaECola": null,
  "dataExpiracao": null,
  "transactionId": "trans-9a8b7c6d-..."
}
```

> `txid` e `transactionId` são gerados com UUID único por requisição — copie o valor retornado para usar nos próximos passos.

---

### 3. Consultar cobrança por ID

```cmd
curl http://localhost:8080/api/v1/cobrancas/1
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "txid": "txid-3f2a1b4c-...",
  "idUsuario": "user-123",
  "tipo": "RECARGA",
  "metodo": "PIX",
  "status": "SOLICITADA",
  "valorSolicitado": 50.00,
  "valorPago": null,
  "dataCriacao": "2026-05-13T13:00:00",
  "dataExpiracao": "2026-05-13T14:00:00",
  "dataFinalizada": null
}
```

---

### 4. Webhook PIX (simula pagamento recebido)

Use o `txid` retornado na criação:

```cmd
curl -X POST http://localhost:8080/api/v1/cobrancas/webhook/pix -H "Content-Type: application/json" -d "{\"pix\": [{\"txid\": \"txid-3f2a1b4c-...\", \"valor\": 50.00, \"endToEndId\": \"E9999901012341234123412345678901\"}]}"
```

**Response:** `200 OK`

Após isso, consulte o ID novamente — o status deve ser `FINALIZADA`.

---

### 5. Validar checkout 3DS (Cartão de Crédito)

Use o `transactionId` retornado na criação com cartão:

```cmd
curl -X POST http://localhost:8080/api/v1/cobrancas/trans-9a8b7c6d-.../validate -H "Content-Type: application/json" -d "{\"cavv\": \"AAABCSIIAAAAAAAAAAAAAAAAAAo=\", \"xid\": \"MDAwMDAwMDAwMDAwMDAwMDAwMDE=\", \"eci\": \"05\"}"
```

**Response:** `200 OK`

---

### Fluxo de teste recomendado

```
POST /cobrancas (PIX)       → anote id e txid
GET  /cobrancas/{id}        → status: SOLICITADA
POST /webhook/pix (txid)    → simula pagamento
GET  /cobrancas/{id}        → status: FINALIZADA ✓

POST /cobrancas (CARTAO)    → anote transactionId
POST /{transactionId}/validate → 200 OK ✓
```

---

## Referência dos endpoints

### Request body — Criar cobrança

```json
{
  "valor": 29.90,
  "tipo": "RECARGA",
  "metodo": "PIX"
}
```

- `valor`: obrigatório, maior que `0.00`
- `tipo`: opcional — `RECARGA` (padrão), `RECARGA_TERCEIROS`, `ENVIO_CARTAO`
- `metodo`: opcional — `PIX` (padrão), `CARTAO_CREDITO`

---

### Códigos de status HTTP

| Código | Situação |
|---|---|
| `200 OK` | Consulta ou webhook processado com sucesso |
| `201 Created` | Cobrança criada com sucesso |
| `400 Bad Request` | Campos obrigatórios ausentes ou inválidos |
| `404 Not Found` | Cobrança não encontrada pelo ID ou transactionId |
| `422 Unprocessable Entity` | Erro de negócio (lock indisponível, erro no gateway, etc.) |
| `500 Internal Server Error` | Erro interno inesperado |

---

### Status de cobrança

| Status | Código | Descrição |
|---|---|---|
| `SOLICITADA` | 2 | Cobrança criada, aguardando pagamento |
| `EXPIRADA` | 3 | Prazo de pagamento expirado |
| `ERRO_APROVACAO_PEDIDO` | 4 | Erro na aprovação do pedido |
| `FINALIZADA` | 5 | Pagamento confirmado |
| `EM_REPROCESSAMENTO` | 6 | Cobrança em reprocessamento |
| `ERRO_ANALISE_PENDENTE` | 9 | Erro com análise pendente |

---

## Tópicos Kafka

| Tópico | Evento |
|---|---|
| `cobranca.criada` | Publicado após criação bem-sucedida de uma cobrança |
| `cobranca.finalizada` | Publicado após finalização via webhook PIX |
