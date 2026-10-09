# Mother Care — Especificação de comportamento do sistema

Derivada da proposta de TCC *"Mother Care: Desenvolvimento de um Aplicativo Móvel para o Acompanhamento do Pré-Natal"*
(Claudio Antonio Nery de Oliveira, UFMT – Campus Araguaia, 2026; orientador Prof. Dr. Thiago Pereira da Silva) e do documento
"Regras de negócios" que a acompanha. Descreve **como o sistema deve se comportar**, não como está implementado.

**Precedência quando as fontes divergem:** (1) tabelas de RN/RF/RNF/UC do TCC → (2) texto narrativo do TCC → (3) documento de
regras de negócio → (4) README de contexto do projeto. Divergências conhecidas estão na seção 10.

---

## 1. Propósito

Plataforma digital de apoio ao pré-natal que funciona como **assistente de saúde**, e não apenas como agenda. Deve:

- calcular a idade gestacional (IG) e a data provável do parto (DPP);
- organizar agendamentos conforme a evolução da gestação;
- guardar o histórico de exames laboratoriais e laudos (Registro Pessoal de Saúde);
- monitorar pressão arterial (PA) e ganho de peso;
- **emitir alertas automáticos** quando valores indicarem risco, para decisão em tempo oportuno.

Motivação: reduzir mortalidade materna por causas obstétricas evitáveis (síndromes hipertensivas, distúrbios metabólicos),
cobrindo o intervalo **entre** consultas presenciais, em que picos assintomáticos de PA deixam de ser rastreados.
O sistema é um **suporte à decisão clínica (CDSS)**: triagem e orientação, nunca diagnóstico.

## 2. Atores

| Ator | Papel |
|---|---|
| **Gestante** | Usuária principal: cadastra-se, registra PA/peso/exames, agenda consultas, consulta checklist e gráficos. |
| **Médico** | Insere/visualiza resultados no prontuário, recebe notificações de alerta, acompanha dados no **painel clínico**. |
| **Sistema (Timer/Regras)** | Executa cálculos, triagens, bloqueios, alertas e a atualização diária do contador de semanas. |

## 3. Arquitetura esperada

- Cliente **Android nativo** (Kotlin + Jetpack Compose) → **API Gateway** (Spring Cloud Gateway) → microsserviços Spring Boot (Kotlin).
- **API Gateway**: porta única de entrada; intercepta requisições, valida o JWT e roteia. O app conhece só o endereço do gateway.
- **Microsserviços**: (a) **Autenticação** — cadastro, perfis, emissão de credenciais; (b) **Triagem Clínica** — algoritmos do
  Caderno de Atenção Básica nº 32, Regra de Naegele, estados de alerta; (c) **Agendamento** — cronogramas e marcação de consultas.
- **PostgreSQL**, um banco/esquema **por serviço** (*Database per Service*); integridade referencial e ACID; todo registro
  clínico (PA, peso, exame) é vinculado univocamente à gestante.
- Comunicação **REST/JSON** síncrona; autenticação **JWT** + Spring Security; camadas Controller → Service → Repository.
- Regras clínicas isoladas na camada de serviço, sem acoplamento a UI ou persistência, para serem testáveis e fiéis aos limiares.
- Documentação da API via OpenAPI (Swagger); build com Gradle; versionamento Git (citados na Justificativa).

## 4. Regras de negócio

| ID | Regra | Comportamento exigido |
|---|---|---|
| **RN01** | Cálculo gestacional | Com a DUM, calcular a **DPP** pela Regra de Naegele (DUM + 7 dias − 3 meses [+1 ano], equivalente a DUM + 7 dias + 9 meses) e a **IG** em semanas e dias: `IG = (data atual − DUM) em dias ÷ 7` (semanas = quociente, dias = resto). |
| **RN02** | Triagem hipertensiva | PA **sistólica ≥ 140 mmHg OU diastólica ≥ 90 mmHg** ⇒ risco obstétrico (possível síndrome hipertensiva/pré-eclâmpsia). |
| **RN03** | Alerta laboratorial | **Hemoglobina < 11 g/dL** ⇒ alerta de anemia. **Glicemia de jejum ≥ 92 mg/dL** ⇒ alerta de diabetes gestacional. |
| **RN04** | Periodicidade | A partir da **28ª semana** (início do 8º mês) o modelo de agendamento passa de **mensal para semanal**. |
| **RN05** | Monitoramento de peso | Ganho de peso **> 1,5 kg por mês durante o 2º trimestre** ⇒ alerta nutricional. |

Limiares são exatos: 140/90 e 92 g são **inclusivos**; 11 g/dL é **exclusivo** (11,0 é normal).

## 5. Requisitos funcionais

| ID | Requisito |
|---|---|
| RF01 | Permitir o cadastro inicial da gestante informando **DUM, peso e altura**. |
| RF02 | Atualizar automaticamente o contador de semanas gestacionais **diariamente** (Dashboard). |
| RF03 | Permitir agendar consultas **verificando a disponibilidade de horários**. |
| RF04 | Exigir o preenchimento da PA no momento do agendamento (**check-in obrigatório**). |
| RF05 | **Bloquear a finalização do agendamento** se a PA informada atender à RN02. |
| RF06 | Exibir a lista de **exames obrigatórios do trimestre atual**. |
| RF07 | Permitir que gestante e médico façam **upload ou inserção manual** de resultados laboratoriais. |
| RF08 | **Notificar médico e gestante** quando exames violarem a RN03. |
| RF09 | Gerar **gráfico evolutivo** do ganho de peso. |

## 6. Requisitos não funcionais

| ID | Categoria | Requisito |
|---|---|---|
| RNF01 | Segurança | Criptografia de ponta a ponta dos dados sensíveis (LGPD, sigilo médico). Senhas nunca em texto puro. |
| RNF02 | Disponibilidade | Uptime de **99,9%**, garantindo entrega dos alertas críticos. |
| RNF03 | Desempenho | Triagem de risco (bloqueio de agendamento) em **< 1,5 s**. |
| RNF04 | Usabilidade | Seguir **WCAG 2.1** (leitura clara, contraste adequado). |
| RNF05 | Portabilidade | Android **10+** e iOS **14+**. |
| RNF06 | Confiabilidade | Não perder dados clínicos em falha temporária de rede (**offline-first**). |

## 7. Casos de uso

Numeração conforme o diagrama (UC01–UC09). `include`: UC01→UC02, UC03→UC04. `extend`: UC04→UC05, UC07→UC08.

**UC01 — Manter cadastro** · Ator: Gestante
- Pré: app instalado; primeiro acesso.
- Fluxo: acessa a tela de cadastro → informa nome, e-mail, DUM, peso e altura → sistema valida e confirma a criação da conta.
- A1 (e-mail já cadastrado): redirecionar para o login.
- Pós: perfil criado com DUM e IMC inicial salvos. DUM é obrigatória e **não pode ser futura**.

**UC02 — Calcular IG e DPP** · Ator: Sistema
- Pré: UC01 concluído com DUM válida.
- Fluxo: lê a DUM → aplica Naegele para a DPP → calcula semanas/dias atuais com a data do dia.
- A1 (DUM futura): solicitar correção.
- Pós: IG e DPP disponíveis; o trimestre atual alimenta checklist e agendamento.

**UC03 — Agendar consulta** · Ator: Gestante
- Pré: usuária logada; UC02 ativo.
- Fluxo: solicita agendamento → sistema verifica se está no 8º mês (≥ 28ª semana) para liberar **horários semanais** → invoca **obrigatoriamente o check-in de saúde (UC04)**.
- A1 (sem horários): sugerir que ligue diretamente para a clínica.
- Pós: consulta reservada e check-in concluído.

**UC04 — Realizar check-in de saúde (PA)** · Ator: Gestante
- Pré: tentativa de agendamento iniciada (UC03).
- Fluxo: sistema solicita a PA → gestante informa sistólica e diastólica → sistema valida que estão **abaixo de 140/90**.
- A1 (PA ≥ 140/90): desviar para o bloqueio crítico (UC05).
- Pós: valores registrados; permissão para agendar concedida.

**UC05 — Bloquear agendamento (risco imediato)** · Ator: Sistema · *sem fluxo alternativo (segurança mandatória)*
- Pré: PA alterada detectada no UC04.
- Fluxo: cancela a reserva do horário → exibe alerta visual de risco de pré-eclâmpsia → indica **busca imediata por atendimento hospitalar**.
- Pós: agendamento impedido; alerta de emergência na tela.

**UC06 — Visualizar checklist de exames** · Ator: Gestante
- Pré: cadastro ativo; IG calculada.
- Fluxo: acessa a aba "Exames" → sistema filtra a lista pelo trimestre (1º, 2º ou 3º) → exibe **exames pendentes e realizados**.

**UC07 — Gerenciar prontuário digital** · Atores: Gestante e Médico
- Pré: acesso ao sistema; resultados em mãos.
- Fluxo: usuário faz upload do laudo **ou** insere valores manuais (Glicemia/Hb) → sistema salva em **ordem cronológica** → médico visualiza no painel clínico.
- A1 (formato inválido — não é PDF nem imagem): solicitar reenvio.
- Pós: dados e laudos armazenados no histórico (listagem em ordem cronológica reversa por trimestre).

**UC08 — Emitir alertas (anemia/diabetes)** · Atores: Sistema e Médico
- Pré: dados laboratoriais inseridos (UC07).
- Fluxo normal: verifica Hb e glicemia → constata normalidade → finaliza **sem disparar avisos**.
- A1: **Hb < 11 g/dL** dispara alerta de anemia. A2: **glicemia ≥ 92 mg/dL** dispara alerta de diabetes gestacional.
- Pós: notificação gerada somente se houver alteração.

**UC09 — Monitorar ganho de peso** · Ator: Gestante
- Pré: peso inicial/IMC registrado no UC01.
- Fluxo: informa o peso atual → sistema plota o ponto na **curva de ganho** → verifica se está dentro do esperado.
- A1 (> 1,5 kg/mês no 2º trimestre): emitir orientações nutricionais.

## 8. Comportamentos transversais

### 8.1 Trimestres (base do checklist, da RN05 e do agendamento)
| Trimestre | Semanas | Foco clínico |
|---|---|---|
| 1º | até a 13ª | Confirmar saúde da mãe e viabilidade da gestação |
| 2º | 14ª a 27ª | Desenvolvimento detalhado do bebê |
| 3º | 28ª até o parto | Preparação para o nascimento e segurança final |

O checklist destaca os exames a agendar conforme a IG. A lista de exames por trimestre vem do Caderno de Atenção Básica nº 32,
**Quadro 12** (roteiro de exames do pré-natal de baixo risco); o TCC não a reproduz item a item.

### 8.2 Fluxo de agendamento (sequência esperada)
1. Gestante solicita agendar → sistema obtém a IG atual.
2. **Check-in de PA obrigatório.** Se PA ≥ 140/90 ⇒ **bloqueia** (UC05), sem exceção e sem agendamento eletivo.
3. Se PA normal ⇒ aplica a periodicidade: **mensal** antes da 28ª semana, **semanal** a partir dela.
4. Verifica disponibilidade de horários; se não houver, sugere contato telefônico com a clínica (A1 do UC03).
5. Confirma a consulta e informa o progresso em relação à **meta mínima de 6 consultas** de pré-natal.
6. Agendamentos de rotina **não podem ser marcados após a DPP** calculada (restrição de validação na persistência).

### 8.3 Lembretes e notificações
- Se o intervalo recomendado entre consultas estiver **expirando**, enviar **notificação automática** (push) lembrando a gestante de marcar a próxima.
- Ao entrar no 8º mês, o fluxo de agendamento passa a priorizar a abertura de horários semanais.
- Alertas críticos (PA, Hb, glicemia) devem ser despachados com prioridade pelo sistema operacional (push nativo).

### 8.4 Alertas — quem recebe o quê
| Gatilho | Gestante | Médico/profissional |
|---|---|---|
| PA ≥ 140/90 | Tela de alerta crítico + orientação de buscar atendimento imediato + agendamento bloqueado | — |
| Hb < 11 g/dL | Notificação de anemia | Notificação assíncrona no painel; **tag de atenção** no registro da paciente |
| Glicemia ≥ 92 mg/dL | Notificação de risco de diabetes gestacional; antecipação de conduta e agendamento do **TOTG** | Notificação no painel |
| Ganho > 1,5 kg/mês (2º tri) | Alerta lógico + sugestões nutricionais | — |

### 8.5 Painel e prontuário
- O **médico visualiza** no painel clínico os dados registrados (UC07) e é notificado dos alertas laboratoriais (RF08).
- O prontuário é um **Registro Pessoal de Saúde (RPS)**: armazena laudos (PDF/imagem) associados a metadados numéricos
  (Hb, glicemia de jejum), em ordem cronológica; o app é o repositório centralizado dos exames.

### 8.6 Peso e IMC
IMC pré-gestacional = peso inicial ÷ altura² (kg/m²), calculado no cadastro e usado como base da curva de ganho ponderal.
O gráfico mostra a evolução real; a verificação "dentro do esperado" aplica a RN05.

### 8.7 Segurança e privacidade
Dados de saúde são sensíveis (LGPD, sigilo médico): acesso apenas autenticado (JWT), tráfego protegido, senhas com hash,
validação do token na borda (gateway) e nos serviços. Requisições sem credencial válida são rejeitadas antes de chegar ao serviço.

## 9. Referências clínicas e normativas
Ministério da Saúde — Caderno de Atenção Básica nº 32 (Quadro 12) · FEBRASGO (mínimo de 6 consultas; consultas semanais no 8º mês) ·
IEEE 830 · ISO/IEC/IEEE 12207 e 29148 · ISO/IEC 25010 · SWEBOK v4.

## 10. Inconsistências e ambiguidades nas fontes (e decisão adotada)

| # | Ponto | Divergência | Adotado |
|---|---|---|---|
| 1 | Início do "8º mês" | Seção 4.1 diz **≥ 31 semanas**; RN04/UC03 e a nota do doc. de regras dizem **28ª semana** | **28ª semana** (RN04 é a tabela normativa) |
| 2 | Glicemia | Seção 4.1: faixa **92–125 mg/dL** como "Ponto de Atenção"; RN03/UC08: **≥ 92** sem teto | **≥ 92**; nada definido acima de 125 |
| 3 | Naegele | RN01 resume "−3 meses" sem o "+1 ano"; README separa por mês de DUM | Equivalentes: **DUM + 7 dias + 9 meses** |
| 4 | Exemplo de IG | Doc. de regras: 15/11/2025 → 18/01/2026 = "65 dias / 9 sem 2 d" | Cálculo de calendário: **64 dias = 9 sem 1 d** |
| 5 | Numeração dos UC | UC03 cita "UC05" (deveria ser UC04); UC04 A1 cita "UC11" (= UC05); pré-condições de UC04, UC05 e UC08 citam o próprio UC ou outro errado; pós-condição do UC02 repete a do UC01; o texto fala em "oito" casos de uso, o diagrama tem **nove** | Numeração do diagrama (UC01–UC09) |
| 6 | Agendamento > 8º mês | README antigo: quinzenal no 3º tri e semanal só da 36ª semana | **Semanal da 28ª** (TCC e doc. de regras) |
| 7 | Nome do projeto | Título "Mother Care"; Justificativa cita "Gestação Segura" | **Mother Care** |
| 8 | Notificação do médico | RF08 fala em notificar "médico e gestante"; seção 4.1 fala em tag + notificação no **painel** do profissional | Ambos (tabela 8.4) |
| 9 | Atualização diária (RF02) | Descreve rotina a cada 24 h | Calcular na hora da consulta é funcionalmente equivalente |
| 10 | E2E (RNF01) | "Criptografia de ponta a ponta" sem definir mecanismo | JWT + hash de senha + canal seguro; E2E estrito não especificado |
| 11 | Não definidos no TCC | Modelo de agenda/horários e quem os cadastra (RF03); vínculo médico↔gestante e cadastro de médico; comportamento abaixo da meta de 6 consultas além do lembrete; método de medir "ganho por mês" (RN05); formato exato do upload além de PDF/imagem | Decidir caso a caso e registrar |
| 12 | Itens citados só na Justificativa | OpenAPI/Swagger e push nativo aparecem no texto, mas não viram RF/RNF | Tratar como desejáveis |
