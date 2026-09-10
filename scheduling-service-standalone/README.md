# scheduling-service (standalone)

Extraído do monorepo `mother-care-backend` para ser testado
isoladamente. Cobre **UC03 (Agendar Consulta)**, a **RN04**
(periodicidade mensal/semanal conforme a semana gestacional) e a
integração com **RF04/RF05** (bloqueio por risco obstétrico).

## Rotas

| Método | Rota | Caso de uso | Regra |
|---|---|---|---|
| `POST` | `/consultas` | UC03 — Agendar consulta | RN04, RF04/RF05 |

## Dependência: este serviço NÃO é standalone de verdade

Diferente do `auth-service`, o `scheduling-service` **precisa do
clinical-service rodando** para funcionar — `AgendamentoService` chama
`GET /gestantes/{id}/idade-gestacional` nele via `ClinicalServiceClient`
pra saber a semana gestacional antes de aplicar a RN04 (é assim que o
TCC define Database per Service: nada de acesso direto ao schema
alheio). Sem o clinical-service de pé, todo `POST /consultas` vai
falhar com erro de conexão.

## Correções aplicadas (mesmos padrões dos outros dois serviços)

1. `Consulta` (entidade JPA) trocada de `data class` para `class` —
   equals/hashCode/copy de data class conflitam com identidade do
   Hibernate.
2. `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`
   adicionado — necessário no Gradle 9.x.
3. `.copy()` no `AgendamentoServiceTest` trocado por reconstrução manual
   da entidade (sem `data class` não existe mais `copy()`).

## Rodar

```bash
docker compose up -d              # sobe só o db-scheduling (porta 5435)

# em outro terminal, suba também o clinical-service standalone (porta 8082)
# — ver README dele para os passos

./gradlew bootRun                 # sobe este serviço na porta 8083
```

## Testar

### Unitário (não depende do clinical-service nem de banco)
```bash
./gradlew test
```
`AgendamentoServiceTest` mocka o `ClinicalServiceClient`, então cobre a
RN04 isoladamente: MENSAL antes da 28ª semana, SEMANAL a partir da 28ª
(incluindo o limite exato), MENSAL ainda na 27ª (limite inferior), e
rejeição por conflito de intervalo mínimo.

### Manual (com clinical-service E scheduling-service rodando)
```bash
# 1. Cadastre uma gestante no clinical-service (porta 8082) primeiro
#    e pegue o gestanteId — ver README do clinical-service.

# 2. Agende uma consulta
curl -X POST http://localhost:8083/consultas \
  -H "Content-Type: application/json" \
  -d '{"gestanteId":"<gestanteId>","dataHoraDesejada":"2026-09-15T10:00:00"}'
# confira "periodicidadeAplicada": deve ser MENSAL ou SEMANAL dependendo
# da semana gestacional que o clinical-service retornou

# 3. Tente agendar de novo dentro do intervalo mínimo — deve dar 409
curl -X POST http://localhost:8083/consultas \
  -H "Content-Type: application/json" \
  -d '{"gestanteId":"<gestanteId>","dataHoraDesejada":"2026-09-16T10:00:00"}'
```

## Observação sobre segurança

Igual ao clinical-service: todas as rotas exigem JWT válido
(`SecurityConfig` não libera nada). Pra testar sem o Gateway na frente,
comente temporariamente o `addFilterBefore` ou gere um token de verdade
no auth-service (mesmo `mothercare.jwt.secret` nos três serviços).

## Lacuna FECHADA nesta versão: check-in de PA agora bloqueia agendamento

Antes desta mudança, este serviço não checava a triagem de PA (RN02)
antes de confirmar o agendamento. Agora, `AgendamentoService` consulta
`GET /checkin/pressao/{gestanteId}/ultimo-status` no clinical-service
**antes de qualquer outra validação** — se o último check-in indicou
risco (`CRITICO`), o agendamento é bloqueado com HTTP 409
(`RiscoObstetricoImediatoException`), atendendo RF04/RF05 e o passo 3
do UC03 ("invoca obrigatoriamente" a triagem).

Se a gestante nunca fez check-in, o clinical-service retorna `NORMAL`
por padrão — não bloqueia por ausência de dado.

### Testar o bloqueio manualmente

```bash
# 1. Registre um check-in de PA crítico no clinical-service primeiro
curl -X POST http://localhost:8082/checkin/pressao \
  -H "Content-Type: application/json" \
  -d '{"gestanteId":"<gestanteId>","sistolica":150,"diastolica":95}'

# 2. Tente agendar — deve dar 409, sem nem chegar a checar RN04
curl -X POST http://localhost:8083/consultas \
  -H "Content-Type: application/json" \
  -d '{"gestanteId":"<gestanteId>","dataHoraDesejada":"2026-09-15T10:00:00"}'
```

## Lacuna que ainda falta (não fechada nesta mudança)

Não existe conceito de disponibilidade/agenda de horários (RF03, fluxo
A1 do UC03 — "sem horários, ligue pra clínica"). Isso segue em aberto.
