# Sistema de Gestão de Pousada

Aplicação de console em Java para gerenciar hóspedes, quartos e hospedagens de uma pousada. Usa Hibernate (JPA) para persistência em um banco MariaDB.

## Grupo

- Rafael Lima
- Vitor Miguel
- Kauã Lopes



## Funcionalidades

| Opção | Ação |
|-------|------|
| 1 | Adicionar hospedagem (cadastra hóspede e quarto na hora, se ainda não existirem) |
| 2 | Listar hospedagens |
| 3 | Cadastrar hóspede |
| 4 | Listar hóspedes |
| 5 | Cadastrar quarto |
| 6 | Listar quartos |
| 7 | Editar hóspede |
| 8 | Excluir hóspede |
| 9 | Editar quarto |
| 10 | Excluir quarto |
| 0 | Sair |

### Regras de negócio

- O e-mail do hóspede é único; o número do quarto também.
- O check-out deve ser posterior ao check-in.
- Um quarto não pode ter duas hospedagens com períodos que se cruzam. O check-out pode coincidir com o check-in da hospedagem seguinte. Hospedagens canceladas não contam.
- Quartos em `MANUTENCAO` não recebem hospedagens.
- O valor total é calculado como número de noites × diária do quarto.
- Cada hospedagem tem um pagamento (PIX, cartão ou dinheiro).
- Excluir um hóspede apaga também suas hospedagens e pagamentos (com confirmação).
- Um quarto que já tem hospedagens não pode ser excluído; use o status `MANUTENCAO`.
- Datas são digitadas como `dd/MM/yyyy` e validadas de forma estrita (`31/02/2025` é rejeitada).
- Valores decimais aceitam vírgula ou ponto e ficam com 2 casas.

## Requisitos

- JDK 17 ou superior
- Maven 3.8+
- MariaDB em execução na porta 3306

## Configuração

Edite `src/main/resources/hibernate.cfg.xml` com seus dados de acesso:

```xml
<property name="hibernate.connection.url">jdbc:mariadb://localhost:3306/pousada_db?createDatabaseIfNotExist=true</property>
<property name="hibernate.connection.username">root</property>
<property name="hibernate.connection.password">SUA_SENHA</property>
```

- O banco `pousada_db` é criado automaticamente (`createDatabaseIfNotExist=true`).
- As tabelas são criadas e atualizadas pelo Hibernate (`hbm2ddl.auto=update`).
- Não versione a senha real do banco em repositórios públicos.

## Como executar

Pelo terminal:

```
mvn compile exec:java
```

Ou pelo IntelliJ: abra a pasta do projeto como projeto Maven, recarregue as dependências e execute a classe `com.pousada.Main`.

Se os acentos aparecerem quebrados no terminal do Windows, rode `chcp 65001` antes ou adicione `-Dfile.encoding=UTF-8` nas opções da JVM.

## Estrutura do projeto

```
pousada/
├── pom.xml
└── src/main/
    ├── java/com/pousada/
    │   ├── Main.java              # menu de console e leitura de dados
    │   ├── dao/
    │   │   ├── HospedeDAO.java
    │   │   ├── QuartoDAO.java
    │   │   └── ReservaDAO.java
    │   ├── model/
    │   │   ├── Hospede.java
    │   │   ├── Quarto.java
    │   │   ├── Reserva.java
    │   │   ├── Pagamento.java
    │   │   ├── MetodoPagamento.java   # PIX, CARTAO, DINHEIRO
    │   │   ├── StatusQuarto.java      # DISPONIVEL, OCUPADO, MANUTENCAO
    │   │   ├── StatusReserva.java     # PENDENTE, CONFIRMADA, CHECK_IN, FINALIZADA, CANCELADA
    │   │   └── TipoQuarto.java        # STANDARD, DELUXE, FAMILIA
    │   └── util/
    │       └── HibernateUtil.java     # SessionFactory e transações com rollback
    └── resources/
        └── hibernate.cfg.xml
```

## Modelo de dados

- **Hospede** 1 — N **Reserva**
- **Quarto** 1 — N **Reserva**
- **Reserva** 1 — 1 **Pagamento**

Tabelas geradas: `hospedes`, `quartos`, `reservas`, `pagamentos`.

## Tecnologias

- Java 17
- Hibernate ORM 6.6.4
- MariaDB Connector/J 3.4.1
- Maven

## Ocultar os logs do Hibernate

Para deixar o terminal limpo (só o menu), o projeto pode ter:

1. No `Main.java`, como primeira linha do `main`, com o logger guardado em um campo `static`:

   ```java
   private static final Logger HIBERNATE_LOG = Logger.getLogger("org.hibernate");
   // dentro do main:
   HIBERNATE_LOG.setLevel(Level.SEVERE);
   ```

2. No `pom.xml`, a dependência `org.slf4j:slf4j-nop:2.0.7`, que silencia o aviso do SLF4J.