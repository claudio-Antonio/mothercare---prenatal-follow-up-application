# Mother Care — App Android (protótipo)

Protótipo funcional em Kotlin + Jetpack Compose, cobrindo o fluxo
completo já validado no backend: login/cadastro, cadastro de gestante
(UC01/UC02), dashboard de IG/DPP, checklist de exames (UC06), check-in
de PA (UC04/UC05/RN02) e agendamento (UC03/RN04 + bloqueio RF04/RF05).

## Como abrir

1. Abra o Android Studio → **Open** → selecione a pasta `mothercare-android`
2. Deixe o Gradle sincronizar
3. Rode num **emulador**, OU use os `@Preview` (ver abaixo) sem rodar nada

## @Preview — veja cada tela sem emulador nem backend

Cada tela foi dividida em duas partes:
- `XScreen` — a versão "conectada" (fala com o repositório/ViewModel de verdade), usada pela navegação real
- `XScreenContent` — a versão "burra", que só desenha a partir de um `UiState` pronto — é essa que tem `@Preview`

No total são **24 previews** cobrindo os principais estados de cada
tela (vazio, preenchido, carregando, sucesso, erro, crítico). No
Android Studio: abra qualquer arquivo `*Screen.kt`, clique na aba
**Split** ou **Design** no canto superior direito do editor, e as
previews renderizam sozinhas. Exemplos do que dá pra ver sem rodar nada:

| Arquivo | Previews |
|---|---|
| `LoginScreen.kt` | vazio, preenchido, carregando, erro |
| `CadastroScreen.kt` | vazio, erro (email já existe) |
| `CadastroGestanteScreen.kt` | vazio, preenchido |
| `DashboardScreen.kt` | carregando, com dados, erro |
| `ChecklistScreen.kt` | carregando, com itens (Realizado/Pendente/Não rastreado) |
| `CheckInScreen.kt` | vazio, resultado normal, resultado crítico |
| `AgendamentoScreen.kt` | vazio, sucesso, bloqueado por risco (RF04/RF05) |
| `home/BottomNavBar.kt` | aba Início selecionada, aba Check-in selecionada |
| `components/*.kt` | Loading, ErrorBanner, PrimaryButton (normal/carregando) |

`HomeScreen.kt` **não tem preview** — ela depende do `AppContainer` de
verdade e dispara chamada de rede real no `init{}` de cada
sub-ViewModel; só dá pra ver funcionando no emulador com o backend no
ar. A barra de navegação inferior (a parte visual isolável) foi
extraída pra `BottomNavBar.kt`, essa sim com preview.

## Antes de rodar de verdade (emulador) — o backend precisa estar de pé

```bash
cd mother-care
docker compose up --build
```

O app aponta pra `http://10.0.2.2:8080/api/` (alias do emulador pro
`localhost` da máquina host, onde o `docker compose` publica o
`api-gateway`). Só funciona em emulador — num aparelho físico, troque
pelo IP da máquina na rede local, em `ApiClient.kt`.

## O que está implementado

| Tela | Cobre | Endpoint |
|---|---|---|
| Login / Cadastro | Identidade | `auth-service` |
| Cadastro da gestante | UC01/UC02 | `POST /gestantes` |
| Dashboard (aba "Início") | IG, DPP, trimestre, IMC | `GET /gestantes/{id}/idade-gestacional` |
| Checklist (aba "Exames") | UC06 | `GET /exames/checklist/{id}` |
| Check-in (aba "Check-in") | RN02, alerta visual de risco | `POST /checkin/pressao` |
| Agendamento (aba "Consultas") | RN04 + bloqueio RF04/RF05 (trata o 409 na UI) | `POST /consultas` |

Sessão (token JWT) persiste entre reinícios via DataStore.

## Simplificações conscientes desse protótipo

- **Datas como texto livre**, não seletor de calendário nativo — evita
  depender de API experimental do Material3 não testável aqui.
- **Sem Hilt** — DI manual via `AppContainer`.
- **Sem verificação de "gestante já cadastrada"** — o backend não expõe
  `GET /gestantes/por-usuario/{usuarioId}`, então todo login passa pela
  tela de cadastro de gestante de novo. O repositório já tem
  `findByUsuarioId`, só falta expor via controller.
- **Ícone do launcher é um vetor genérico**, não arte de design.

## Nada disso foi compilado no sandbox

Escrevi e revisei a sintaxe com cuidado, mas não tive Android
SDK/Gradle disponível aqui pra compilar de verdade. Primeira tentativa
de build/preview no Android Studio pode revelar algum ajuste de versão
— me manda o erro que eu corrijo rápido.
