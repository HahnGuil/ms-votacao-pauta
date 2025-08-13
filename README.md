# Desafio Técnico — Sistema de Votação

> 📑 Para detalhes completos do desafio, requisitos e instruções de execução, acesse o [README de Instruções](./README-instructions.md).

Este projeto consiste em um sistema de votação digital, desenvolvido para ser robusto, escalável e com alta responsividade, utilizando tecnologias modernas do ecossistema Java e soluções de mensageria e cache distribuído.


## 📖 Descrição do Projeto
---

## 🔧 Tecnologias & Ferramentas

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat&logo=mongodb&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-231F20?style=flat&logo=apache-kafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=flat&logo=postman&logoColor=white)
![JUnit](https://img.shields.io/badge/JUnit-25A162?style=flat&logo=junit5&logoColor=white)

- **Java + Spring Boot** — Backend principal, com suporte a agendamento de tarefas, controle de sessões e endpoints REST.
- **Apache Kafka** — Mensageria para processamento assíncrono e desacoplamento das operações de voto.
- **Redis** — Cache distribuído para controle de votos recebidos e otimização de consultas de votos duplicados.
- **MongoDB** — Banco de dados NoSQL utilizado para persistência dos dados.
- **Docker/Docker Compose** — Orquestração dos ambientes de desenvolvimento e produção, facilitando a execução dos serviços.
- **Postman** — Collections para teste dos endpoints e documentação do fluxo das APIs.
- **JUnit** — Framework de testes unitários para Java, utilizado nos testes dos serviços.

---

## 🚀 Versões dos Serviços

### [Microsserviço de Votação](./ms-votacao-pauta)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat&logo=spring-boot&logoColor=white)


### [Serviço de Validação de CPF](./validador)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat&logo=spring-boot&logoColor=white)


Ambos utilizam as mesmas versões de Java e Spring Boot, garantindo padronização e compatibilidade entre os serviços.

---

## 🐳 Serviços do Docker Compose

| Serviço                | Descrição                                                                 | Porta   |  
|------------------------|--------------------------------------------------------------------------|---------|
| ![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat&logo=mongodb&logoColor=white) **mongo-votacaodbDOCKER** | MongoDB para ambiente local, inicializado com usuário, senha e banco via variáveis de ambiente. Scripts personalizados. | 27017    |
| ![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat&logo=mongodb&logoColor=white) **mongo-votacaodbDev** | MongoDB para desenvolvimento, com configurações e scripts específicos para testes. | 27018    |
| ![Kafka](https://img.shields.io/badge/Kafka-231F20?style=flat&logo=apache-kafka&logoColor=white) **kafka**            | Apache Kafka para mensageria assíncrona entre microsserviços.                      | 9092     |
| ![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white) **redis**                   | Redis para cache e armazenamento temporário de dados.                              | 6379     |
| ![Validador](https://img.shields.io/badge/Validador-6DB33F?style=flat&logo=spring-boot&logoColor=white) **validador** | Microsserviço para validação de CPF.                                               | 26000    |
| ![Votação](https://img.shields.io/badge/Votacao-ED8B00?style=flat&logo=java&logoColor=white) **ms-votacao-pauta** | Microsserviço principal de votação e gerenciamento de pautas.                      | 30000    |

> Todos os serviços estão conectados pela rede interna `app-network` e utilizam volumes para persistência de dados.

---

## ⚡ Principais Decisões de Implementação

- <img src="https://img.shields.io/badge/VoteService-6DB33F?style=flat&logo=spring-boot&logoColor=white" height="20"> **VoteService:**
  - Recebe votos e garante integridade do processo.
  - Utiliza <img src="https://img.shields.io/badge/Kafka-231F20?style=flat&logo=apache-kafka&logoColor=white" height="16"> para processamento assíncrono.
  - Utiliza <img src="https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white" height="16"> para cache e controle de votos.
  - Votos gravados em lote a cada 30s.
  - Validações: votação ativa → voto duplicado → CPF válido.

- <img src="https://img.shields.io/badge/Duplicidade-47A248?style=flat&logo=mongodb&logoColor=white" height="20"> **Controle de votos duplicados:**
  - Realizado no <img src="https://img.shields.io/badge/MongoDB-47A248?style=flat&logo=mongodb&logoColor=white" height="16">, <img src="https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white" height="16"> e batch em memória.
  - Garante que votos em processamento não sejam duplicados.

- <img src="https://img.shields.io/badge/Scheduler-6DB33F?style=flat&logo=spring-boot&logoColor=white" height="20"> **VotingScheduler:**
  - Verifica e encerra votações expiradas a cada minuto.
  - Atualiza status, grava votos pendentes e emite evento para cálculo do resultado.

- <img src="https://img.shields.io/badge/EventHandler-ED8B00?style=flat&logo=java&logoColor=white" height="20"> **VotingEventHandler:**
  - Facilita o envio do resultado da votação.
  - Front-end consulta se o resultado está disponível e obtém o resultado sem depender de tópicos ou filas.

- <img src="https://img.shields.io/badge/Versão%20API-232F3E?style=flat&logo=github&logoColor=white" height="20"> **Controle de Versão da API:**
  - Versão recebida como parâmetro na requisição, inserida em DTO e propagada até o service.
  - Permite regras de negócio diferentes conforme a versão, sem alterar o payload do usuário.
  - Sistema flexível para evoluções futuras.

---

## 🛠️ Configuração das Variáveis de Ambiente

Para rodar o projeto corretamente, é necessário configurar as variáveis de ambiente utilizadas nos arquivos de configuração (`.env`).

| Variável      | Exemplo         | Descrição                       |
|--------------|-----------------|---------------------------------|
| `MONGO_USER` | admin           | Usuário do MongoDB              |
| `MONGO_PASS` | admin123        | Senha do MongoDB                |
| `MONGO_DB`   | votacaodbHO     | Nome do banco de dados          |

> 💡 Recomenda-se copiar os valores diretamente do arquivo `.env` para garantir que estejam corretos.

### ⚙️ Como configurar nas principais ferramentas

#### ![IntelliJ](https://img.shields.io/badge/IntelliJ_IDEA-000000?style=flat&logo=intellij-idea&logoColor=white) IntelliJ IDEA
1. Vá em **Run > Edit Configurations**.
2. No campo **Environment variables**, adicione:
   ```
   MONGO_USER=admin;MONGO_PASS=admin123;MONGO_DB=votacaodbHO
   ```

#### ![STS](https://img.shields.io/badge/STS-6DB33F?style=flat&logo=spring&logoColor=white) Spring Tool Suite (STS)
1. Clique com o botão direito no projeto e vá em **Run As > Run Configurations**.
2. Na aba **Environment**, clique em **New...** e adicione as variáveis conforme o arquivo `.env`.

#### ![Windows](https://img.shields.io/badge/Windows-0078D6?style=flat&logo=windows&logoColor=white) / ![Linux](https://img.shields.io/badge/Linux-333333?style=flat&logo=linux&logoColor=white) / ![Mac](https://img.shields.io/badge/macOS-000000?style=flat&logo=apple&logoColor=white) Sistema Operacional
- **Windows (Prompt de Comando):**
  ```
  set MONGO_USER=admin
  set MONGO_PASS=admin123
  set MONGO_DB=votacaodbHO
  ```
- **Linux/Mac (Terminal):**
  ```
  export MONGO_USER=admin
  export MONGO_PASS=admin123
  export MONGO_DB=votacaodbHO
  ```

---

### 🔀 Perfis de Execução

O projeto pode ser executado com dois perfis principais:

- <img src="https://img.shields.io/badge/local-6DB33F?style=flat&logo=spring-boot&logoColor=white" height="18"> **local**: Para desenvolvimento, conecta aos serviços rodando localmente. Banco padrão: `votacaodbDev` (porta 27018).
- <img src="https://img.shields.io/badge/docker-2496ED?style=flat&logo=docker&logoColor=white" height="18"> **docker**: Para execução em containers, conecta aos serviços do `docker-compose`. Banco padrão: `votacaodbDocker` (porta 27017).

#### Como definir o perfil ativo na IDE
- **IntelliJ/STS**: Adicione em VM options:
  ```
  -Dspring.profiles.active=local
  ```
  ou
  ```
  -Dspring.profiles.active=docker
  ```

> Dica: O perfil ativo pode ser alterado facilmente para testar integrações tanto com serviços locais quanto com containers Docker.

---

### 🐳 Como Rodar o Projeto com Docker Compose

1. Faça o build dos containers sem cache:
   ```
   docker compose build --no-cache
   ```
2. Inicie os serviços em segundo plano:
   ```
   docker compose up -d
   ```

Todos os microsserviços e dependências (MongoDB, Kafka, Redis, etc.) serão inicializados conforme definido no arquivo `docker-compose.yml`. O perfil `docker` será ativado automaticamente.

> Para acompanhar os logs dos serviços:
> ```
> docker compose logs -f
> ```
> **Atenção:** Aguarde todos os containers subirem e ficarem saudáveis antes de acessar os serviços. O Docker Compose segue uma sequência de inicialização, garantindo que dependências como MongoDB, Kafka, Redis e Validador estejam prontos antes de iniciar o microsserviço principal ([ms-votacao-pauta](./ms-votacao-pauta)).
> Para verificar o status dos containers:
> ```
> docker compose ps
> ```

---

## 🧪 Como Testar a API com Postman

Para facilitar os testes dos endpoints, utilize as collections e ambientes disponíveis:

| Collection/Env | Descrição | Porta |
|---|---|---|
| [desafio-votacao-dev.json](./docs/postman-collections/desafio-votacao-dev.json) | Testes em ambiente de desenvolvimento | 2500 |
| [desafio-votacao-docker.json](./docs/postman-collections/desafio-votacao-docker.json) | Testes em ambiente Docker | 30000 |
| [validador-cpf.json](./docs/postman-collections/validador-cpf.json) | Testes do serviço de validação de CPF | 26000 |
| [envs-votacao.json](./docs/postman-environments/envs-votacao.json) | Ambiente para popular variáveis durante os testes | - |

> 📁 As collections estão em `docs/postman-collections/` e o ambiente em `docs/postman-environments/`

### 🚦 Passos Gerais

1. <img src="https://img.shields.io/badge/Importar-FF6C37?style=flat&logo=postman&logoColor=white" height="18"> Importe a collection desejada no Postman.
2. <img src="https://img.shields.io/badge/Importar%20Ambiente-FF6C37?style=flat&logo=postman&logoColor=white" height="18"> Importe o ambiente `envs-votacao.json` para facilitar testes em lote.
3. Siga as instruções descritas nas collections para criar usuários, votações, votos e consultar resultados.

### 🔑 Endpoints Principais

#### 👤 User
- **Create-single-user**: Cria um usuário por vez, com nome e CPF aleatórios.
- **Create-Multi-user**: Cria múltiplos usuários em lote, usando o ambiente para armazenar IDs.
  - Importe o ambiente `envs-votacao.json`.
  - Execute o request em modo "Run" e defina o número de interações e delay.
  - IDs criados ficam salvos na variável `userIds`.

#### 🗳️ Voting
- **Create-Voting**: Cria uma nova votação. Informe o assunto e o tempo de expiração (em minutos).

#### ✅ Vote
- **Create-Single-Vote**: Registra um voto para um usuário específico.
- **Create-Multi-vote**: Registra múltiplos votos em lote, usando os IDs criados anteriormente.
  - Execute o Multi-user para criar usuários.
  - Execute o Multi-vote para votar em lote, definindo o número de interações e delay.

#### 📊 Result
- **Check-result**: Consulta o resultado de uma votação pelo `votingId`.
- **Check-Result-Exists**: Verifica se o resultado de uma votação já está disponível.

#### 📚 Swagger
- Acesse a documentação interativa da API:
  - <img src="https://img.shields.io/badge/Dev-2500-6DB33F?style=flat" height="16"> [Swagger Dev](http://localhost:2500/api/votacao/swagger-ui.html)
  - <img src="https://img.shields.io/badge/Docker-30000-2496ED?style=flat" height="16"> [Swagger Docker](http://localhost:30000/api/votacao/swagger-ui.html)

#### ❤️ Health-check
- Verifique se os serviços estão ativos usando os endpoints de health check.
  - Votação: `/actuator/health` na porta correspondente
  - Validador de CPF: `http://localhost:26000/api/v1/actuator/health`

---
> ℹ️ Consulte as instruções detalhadas em cada collection para exemplos de uso, bodies de requisição e dicas para testes em lote.
