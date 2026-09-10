# clinical-service (standalone)

Extraído do monorepo `mother-care-backend` para ser testado
isoladamente. É o **CDSS (Clinical Decision Support System)** do Mother
Care — o microsserviço que concentra toda a inteligência clínica do TCC.

## Rotas

| Método | Rota | Caso de uso | Regra |
|---|---|---|---|
| `POST` | `/gestantes` | UC01 — Cadastro (DUM, peso, altura) | — |
| `GET` | `/gestantes/{id}/idade-gestacional` | UC02 — IG e DPP | RN01 |
| `POST` | `/checkin/pressao` | UC04/UC05 — Check-in de PA | RN02 |
| `GET` | `/checkin/pressao/{gestanteId}/ultimo-status` | Consultado pelo scheduling-service (RF04/RF05) | RN02 |
| `GET` | `/exames/checklist/{gestanteId}` | UC06 — Checklist de exames | — |
| `POST` | `/exames` | UC07/UC08 — Registrar exame + alerta | RN03 |
| `POST` | `/peso` | UC09 — Registrar peso | RN05 |
| `GET` | `/peso/{gestanteId}/grafico` | UC09 — Gráfico de evolução | RN05 |
| `POST` | `/prontuario/documentos` (multipart) | UC07 — Upload de laudo (PDF/imagem) | RF07, A1 |
| `GET` | `/prontuario/documentos/{gestanteId}` | UC07 — Listagem cronológica reversa | — |
| `GET` | `/prontuario/documentos/conteudo/{id}` | UC07 — Download do arquivo | — |

## Prontuário digital (UC07) — decisão de arquitetura

O binário do laudo é salvo via `@Lob` (bytea) direto no Postgres do
clinical-service, **fiel à Seção 4.3.3 do TCC**, que define apenas
PostgreSQL como tecnologia de persistência (sem object storage externo
como S3/MinIO). Isso foi uma escolha consciente, não um descuido — o
trade-off está documentado no Javadoc de `DocumentoClinico.kt`: bytea no
Postgres escala pior que um object storage dedicado para arquivos
grandes/alto volume, o que pode pressionar a meta de 99,9% de uptime da
RNF02 em produção real. Se isso virar um problema prático, a evolução
natural é migrar para MinIO (self-hosted, compatível com S3) guardando
só a referência no banco — vale citar como "trabalho futuro" na
monografia.

O upload valida o `Content-Type` do arquivo (`application/pdf`,
`image/jpeg`, `image/png`) antes de salvar — é o fluxo alternativo A1 do
UC07 ("se o arquivo não for PDF ou imagem, o sistema solicita reenvio"),
implementado como `FormatoArquivoInvalidoException` → HTTP 400.

`ExameLaboratorial.documentoId` (antes `laudoUrl`) linka o valor
numérico (Hb/Glicemia) ao arquivo do laudo, quando o app manda os dois
juntos. Para itens do checklist sem valor numérico rastreado (USG,
sorologias), o documento existe sozinho, sem `ExameLaboratorial`
associado.

## Correção aplicada (mesmo caso do auth-service)

As 4 entidades JPA (`Gestante`, `RegistroPressaoArterial`,
`ExameLaboratorial`, `RegistroPeso`) foram trocadas de `data class` para
`class` — mesmo motivo do auth-service (equals/hashCode/copy de data
class conflitam com o gerenciamento de identidade do Hibernate). Os
DTOs continuam `data class` normalmente, pois não são entidades.

## Rodar

```bash
docker compose up -d      # sobe só o db-clinical (porta 5434)
./gradlew bootRun         # sobe o serviço na porta 8082
```

## Testar

### Unitário
```bash
./gradlew test
```

### Manual (com o serviço rodando)
```bash
# 1. Cadastro — retorna IG/DPP já calculadas (UC01+UC02)
curl -X POST http://localhost:8082/gestantes \
  -H "Content-Type: application/json" \
  -d '{"usuarioId":"11111111-1111-1111-1111-111111111111","dataUltimaMenstruacao":"2025-11-15","pesoInicialKg":60,"alturaMetros":1.65}'
# guarde o "gestanteId" da resposta

# 2. Checklist de exames do trimestre atual (UC06)
curl http://localhost:8082/exames/checklist/<gestanteId>

# 3. Check-in de PA normal
curl -X POST http://localhost:8082/checkin/pressao \
  -H "Content-Type: application/json" \
  -d '{"gestanteId":"<gestanteId>","sistolica":120,"diastolica":80}'
# agendamentoLiberado: true

# 4. Check-in de PA crítica (RN02) — deve bloquear
curl -X POST http://localhost:8082/checkin/pressao \
  -H "Content-Type: application/json" \
  -d '{"gestanteId":"<gestanteId>","sistolica":150,"diastolica":95}'
# agendamentoLiberado: false

# 5. Exame com alerta de anemia (RN03)
curl -X POST http://localhost:8082/exames \
  -H "Content-Type: application/json" \
  -d '{"gestanteId":"<gestanteId>","tipo":"HEMOGLOBINA","valor":9.5}'
# alertaDisparado: true

# 6. Registrar peso (UC09)
curl -X POST http://localhost:8082/peso \
  -H "Content-Type: application/json" \
  -d '{"gestanteId":"<gestanteId>","pesoKg":61}'

# 7. Upload de laudo (UC07)
curl -X POST http://localhost:8082/prontuario/documentos \
  -F "gestanteId=<gestanteId>" \
  -F "categoria=Hemograma completo" \
  -F "arquivo=@/caminho/para/laudo.pdf;type=application/pdf"

# 8. Listar documentos da gestante
curl http://localhost:8082/prontuario/documentos/<gestanteId>
```

## Observação

Igual ao auth-service: rodando isolado (sem o api-gateway na frente), o
`SecurityConfig` deste serviço exige JWT válido nas rotas — só que aqui,
diferente do auth-service, **as rotas não são públicas**. Ou seja, os
comandos curl acima vão retornar 401 se você não mandar um
`Authorization: Bearer <token>` válido (gerado pelo auth-service com o
mesmo `mothercare.jwt.secret`). Pra testar sem o Gateway, ou gere um
token real fazendo login no auth-service primeiro, ou comente
temporariamente o `addFilterBefore` no `SecurityConfig` deste serviço.
