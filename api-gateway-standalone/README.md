# api-gateway (standalone)

Reconstruído a partir do design já validado nesta conversa. É a **única
porta de entrada externa** do ecossistema Mother Care — proxy reverso +
validador de borda do JWT (Tabela 1 do TCC, Seção 4.2.2).

## O que ele faz (e o que ele NÃO faz)

- **Roteia por path** para os 3 microsserviços internos, sem o app
  precisar conhecer suas URLs individuais (Seção 4.4.2 do TCC).
- **Valida a assinatura do JWT** antes de deixar a requisição passar —
  mas não emite tokens (isso é papel do `auth-service`) nem faz
  chamada de rede pra validar; é uma checagem criptográfica local,
  usando o mesmo segredo compartilhado (`mothercare.jwt.secret`).
- **Não substitui** a validação de JWT que `clinical-service` e
  `scheduling-service` já fazem internamente — é defesa em profundidade,
  não redundância inútil.

## Rotas (roteamento, não endpoints próprios)

| Prefixo recebido | Encaminha para | Rota pública (sem JWT)? |
|---|---|---|
| `/api/auth/registrar`, `/api/auth/login` | auth-service | Sim |
| `/api/auth/**` (demais) | auth-service | Não |
| `/api/gestantes/**`, `/api/checkin/**`, `/api/exames/**`, `/api/peso/**`, `/api/prontuario/**` | clinical-service | Não |
| `/api/consultas/**` | scheduling-service | Não |

## Este serviço também não é standalone de verdade

Assim como o `scheduling-service`, ele só é realmente útil com os
outros 3 microsserviços no ar — sem eles, todo roteamento vai falhar
com erro de conexão (o Gateway em si sobe sozinho, mas não tem pra
onde mandar nada). A diferença é que os **testes unitários aqui não
dependem de rede nenhuma** — testam a lógica de decisão do filtro
isoladamente (deixar passar / bloquear / propagar identidade), sem
subir contexto Spring nem bater em serviço nenhum.

## Rodar

```bash
./gradlew bootRun    # sobe na porta 8080
```

Sozinho ele sobe, mas só serve pra você ver que não dá erro de start —
qualquer chamada roteada vai falhar até os outros 3 estarem no ar
(portas 8081/8082/8083, mesmo `mothercare.jwt.secret`).

## Testar

### Unitário (não depende de nada externo)
```bash
./gradlew test
```

- **`JwtValidatorTest`** — gera tokens JWT reais (mesma lib que o
  auth-service usa) e testa: assinatura válida, subject extraído
  corretamente, token expirado rejeitado, assinatura com segredo
  diferente rejeitada, string aleatória rejeitada.
- **`JwtAuthenticationGlobalFilterTest`** — testa o filtro em si com
  `MockServerWebExchange` (utilitário reativo do spring-test), sem
  contexto Spring completo: rotas públicas passam sem token, rotas
  protegidas sem `Authorization` são bloqueadas (401), token inválido é
  bloqueado, cabeçalho mal formatado (sem `Bearer`) é bloqueado, e
  token válido deixa passar propagando `X-Auth-User`.

### Manual (com os 4 serviços no ar — local ou via docker-compose do monorepo)
```bash
# Rota pública — deve passar mesmo sem token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@teste.com","senha":"senhaForte123"}'

# Rota protegida sem token — deve dar 401
curl -i http://localhost:8080/api/gestantes/qualquer-id/idade-gestacional

# Rota protegida com token válido — deve rotear pro clinical-service
curl http://localhost:8080/api/gestantes/<gestanteId>/idade-gestacional \
  -H "Authorization: Bearer <token-do-login-acima>"
```

## Observação sobre o teste do filtro

`MockServerWebExchange` não sobe um servidor HTTP de verdade — ele
simula a requisição/resposta reativa em memória, então o teste roda em
milissegundos e não precisa de porta nem de rede. É o jeito certo de
testar um `GlobalFilter` de Spring Cloud Gateway sem depender de
`@SpringBootTest` (que seria mais lento e vagamente redundante aqui,
já que a lógica de roteamento em si o Spring Cloud Gateway já testa
para nós — o que vale testar é a *nossa* lógica de decisão no filtro).
