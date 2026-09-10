# auth-service (standalone)

Extraído do monorepo `mother-care-backend` para ser testado
isoladamente. Cobre **UC01 (Manter Cadastro)** e a emissão de JWT.

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/registrar` | Cadastro (nome, email, senha, papel opcional) |
| `POST` | `/auth/login` | Login → retorna token JWT |

Roles disponíveis (`Papel`): `GESTANTE` (padrão) e `MEDICO`.

## Rodar

```bash
docker compose up -d          # sobe só o db-auth (porta 5433)
./gradlew bootRun             # sobe o serviço na porta 8081
```

> Se `./gradlew` não existir ainda, abra a pasta no IntelliJ IDEA —
> ele gera o wrapper no primeiro sync. Alternativa via CLI:
> `gradle wrapper --gradle-version 8.9` (requer Gradle instalado).

## Testar

### Unitário (não depende de banco nem rede)
```bash
./gradlew test
```
Cobre `AuthServiceTest`: cadastro com sucesso, hash BCrypt (garante que a
senha nunca é salva em texto puro), rejeição de email duplicado (UC01 /
fluxo A1), rejeição de login com senha errada e com email inexistente.

### Manual (com o serviço rodando)
```bash
# Cadastro
curl -X POST http://localhost:8081/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Silva","email":"maria@teste.com","senha":"senhaForte123"}'

# Login
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@teste.com","senha":"senhaForte123"}'

# Cadastro duplicado — deve retornar 409
curl -X POST http://localhost:8081/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Silva","email":"maria@teste.com","senha":"outraSenha123"}'

# Login com senha errada — deve retornar 401
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@teste.com","senha":"errada"}'
```

## Observação

Este serviço não valida JWT nas próprias rotas (`SecurityConfig` libera
tudo com `permitAll`) — isso é intencional: no ecossistema completo, a
validação de borda acontece no `api-gateway`. Rodando standalone, não
há Gateway na frente, então **não há autenticação nas rotas** além do
próprio cadastro/login. Não é um problema para este teste isolado, mas
não exponha esta instância standalone publicamente.
