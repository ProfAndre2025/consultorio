# Sistema de Consultorio Odontologico — Java + Spring Boot

Transcricao do projeto didatico originalmente escrito em Python/PyQt6 para uma
aplicacao **web** em Java 21 + Spring Boot 3 + MySQL.

> **Estado atual: Etapa 3 de 4 — Aplicacao web completa.**
> Sistema funcional de ponta a ponta: controllers MVC, telas Thymeleaf,
> tratamento global de erros e comprovante em PDF. Sem Lombok.

---

## 1. Como rodar

### Pre-requisitos
- JDK 21
- Maven 3.9+ (ou o wrapper da sua IDE)
- MySQL Server em execucao

### Passo a passo

```bash
# 1. informe a senha do MySQL por variavel de ambiente
#    Linux/macOS:
export DB_PASSWORD=suasenha
#    Windows (PowerShell):
$env:DB_PASSWORD="suasenha"

# 2. suba a aplicacao
mvn spring-boot:run
```

Nao existe script de instalacao do banco, nem `application.yml`, nem Flyway.
Toda a configuracao do banco esta em `config/DataConfiguration.java`, seguindo
o padrao do projeto "Sistema de Suporte Tecnico" do curso. Na subida:

1. `garantirBancoCriado()` conecta no servidor MySQL e executa
   `CREATE DATABASE IF NOT EXISTS` -- a verificacao que evita o erro
   "Unknown database";
2. o **Hibernate** cria/atualiza as tabelas a partir das entidades
   (`hbm2ddl.auto = update`);
3. um `ApplicationRunner` carrega os dados de exemplo do `import.sql`
   (seed idempotente com `INSERT IGNORE`).

As auto-configuracoes de banco do Spring Boot ficam desligadas (ver os
`exclude` em `ConsultorioApplication`) para nao haver duas fontes de verdade.

Se o mapeamento divergir do schema, a aplicacao nao sobe — e isso e proposital.

---

## 2. Organizacao

```
src/main/java/br/senai/consultorio/
├── ConsultorioApplication.java     ponto de entrada
├── domain/
│   ├── model/                      entidades JPA + regras de negocio
│   ├── enums/                      StatusConsulta, OrigemCancelamento
│   └── exception/                  excecoes de dominio
├── repository/                     interfaces Spring Data JPA
├── dto/
│   ├── form/                       entrada: classes mutaveis + Bean Validation
│   └── view/                       saida: records imutaveis
├── mapper/                         traducao entidade <-> form <-> view
├── service/                        orquestracao + @Transactional
├── report/                         comprovante em PDF (porta + OpenPDF)
├── config/                         configuracao do banco (DataConfiguration) e da aplicacao
└── web/
    └── controller/                 controllers MVC (Thymeleaf)

src/main/resources/templates/       telas Thymeleaf
├── fragmentos/                     layout base + paginacao
├── pacientes/  dentistas/  produtos/
├── procedimentos/  agenda/  atendimentos/  erro/
src/main/resources/static/css/      folha de estilo (visual iOS)

src/main/resources/
└── import.sql                      dados de exemplo (seed idempotente)

Nao ha application.yml nem Flyway: a configuracao do banco (verificacao/criacao
do banco, DataSource, JPA/Hibernate e carga do seed) fica em
config/DataConfiguration.java; porta e dados da clinica ficam em
config/ConfiguracaoAplicacao.java.
```

---

## 3. Correspondencia com o projeto Python

| Python                        | Java / Spring Boot                          |
|-------------------------------|---------------------------------------------|
| `banco/conexao.py`            | `spring-boot-starter-data-jpa` + HikariCP   |
| `banco/schema.sql`            | `db/migration/V1__criacao_do_schema.sql`    |
| `instalar_banco.py`           | Flyway (automatico na subida)               |
| `banco/repositorios.py`       | `repository/*.java` + regras nas entidades  |
| `interface/tela_*.py`         | controllers + templates Thymeleaf (Etapa 3) |
| `relatorios/pdf_atendimento.py` | `ComprovanteService` (Etapa 3)            |
| `config.py`                   | `DataConfiguration.java` + variaveis de ambiente |

---

## 4. Defeitos do projeto anterior corrigidos aqui

| Defeito                                       | Correcao |
|-----------------------------------------------|----------|
| Instalador pulava `CREATE TABLE`               | Hibernate gera o schema; seed idempotente no `DataConfiguration` |
| Estoque `INT` e podia ficar negativo           | `DECIMAL(12,2)` + `Produto.baixarEstoque()` valida saldo |
| Reajuste de preco alterava atendimento passado | `preco_mao_obra` e `preco_unitario` copiados no atendimento |
| Mudanca de duracao bagunçava a agenda          | `data_hora_fim` gravada no agendamento |
| Corrida ao agendar (dois atendentes)           | `@Version` + `existeConflito()` antes de gravar |
| Senha do banco em texto plano no codigo        | variavel de ambiente `DB_PASSWORD` |
| `dia_semana` 0..6 (segunda? domingo?)          | `java.time.DayOfWeek` gravado como texto |
| Instalador rodado 2x duplicava horarios        | `UNIQUE (dentista_id, dia_semana, hora_inicio)` |
| Material previsto duplicava linha              | `Atendimento.adicionarMaterial()` soma a quantidade |
| `except Exception: pass` escondia erros        | excecoes de dominio tipadas |

---

## 5. Decisoes de projeto

**Regra de negocio mora na entidade.** `Consulta.cancelar()`, `Produto.baixarEstoque()`
e `Atendimento.finalizar()` decidem o que e valido. O service so orquestra:
carrega, chama, deixa a transacao gravar. Assim a regra existe em UM lugar.

**Nao ha `XServiceImpl`.** Interface com uma unica implementacao nao desacopla
nada — so dobra o numero de arquivos. O desacoplamento real esta nos
repositorios (contrato sem implementacao) e nas portas de infraestrutura.

**Services dependem de repositorios, nunca de outros services.** Evita
dependencia circular e propagacao de mudancas em cascata.

**Entidade nunca chega ao template.** Templates recebem `record` de `dto/view`.
Sem isso, um acesso LAZY dentro do HTML estouraria `LazyInitializationException`.

**Formularios sao classes mutaveis; views sao records.** O Thymeleaf precisa de
`new PacienteForm()` e de getter/setter para o `th:field`. Ja a saida e somente
leitura, entao record.

**N+1 combatido explicitamente:** `join fetch` no carregamento de agregados,
`findByConsultaIdIn` para buscar atendimentos em lote e `ProcedimentoResumoView`
nas listagens, que nao toca na colecao de materiais.

---

## 6. Proximas etapas

- **Etapa 4** — testes automatizados (JUnit + Mockito) e empacotamento final.

## 7. Como usar o sistema

1. `mvn spring-boot:run` e abra `http://localhost:8080`.
2. Cadastre **dentistas** e, na tela de horarios, informe os dias e faixas de atendimento.
3. Cadastre **materiais** e **procedimentos**; vincule os materiais padrao de cada procedimento.
4. Cadastre **pacientes**.
5. Na **agenda**, clique em "Agendar consulta": informe paciente, procedimento, data e hora,
   veja os dentistas livres, escolha um e confirme.
6. Na agenda, clique em **Atender**: registre os materiais usados e finalize.
   A finalizacao baixa o estoque numa unica transacao.
7. Emita o **comprovante em PDF** do atendimento finalizado.

## 8. Decisoes de projeto da camada web

**POST-Redirect-GET em todo lugar.** Depois de gravar, o controller sempre
redireciona. Apertar F5 nao reenvia o formulario nem duplica cadastros.

**Erros num lugar so.** `ControleGlobalDeErros` (`@ControllerAdvice`) traduz
cada tipo de erro: regra de negocio volta a tela com mensagem, `@Version` vira
aviso de "editado por outra pessoa", id inexistente vira 404. E o oposto do
`except Exception: pass` do projeto Python.

**Comprovante desacoplado de verdade.** A porta `GeradorComprovante` (interface)
separa "o que" de "como". Trocar PDF por outro formato nao toca em mais nada.
A implementacao recebe um `AtendimentoView` (record), nao a entidade, entao nao
depende de Hibernate nem corre risco de acesso LAZY fora da transacao.

**Agendamento em dois passos.** O usuario ve quem esta livre antes de escolher;
a disponibilidade e reconferida no servidor antes de gravar.

**Uma folha de estilo, sem framework.** Visual claro tipo iOS, cartoes
arredondados e um unico azul de destaque — legivel para quem esta aprendendo.
# consultorio
