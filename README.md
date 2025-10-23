# 💳 Thuler Payment Gateway API

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)

Gateway de pagamentos desenvolvido com Java 21 e Spring Boot seguindo princípios de Domain-Driven Design (DDD).

---

## 📋 Sobre o Projeto

Sistema completo de gateway de pagamentos que permite:

- 👤 Gerenciamento de usuários com autenticação JWT
- 💰 Criação e gestão de cobranças entre usuários
- 💳 Pagamentos via saldo em conta ou cartão de crédito
- 🔌 Integração com autorizador externo
- ❌ Cancelamento de cobranças com estorno automático

---

## 🚀 Tecnologias Utilizadas

- **Java 21** - Linguagem de programação
- **Spring Boot 3.5.6** - Framework (Web, Data JPA, Security, Validation)
- **PostgreSQL 16** - Banco de dados
- **JWT** - Autenticação via tokens
- **Docker & Docker Compose** - Containerização
- **Swagger/OpenAPI** - Documentação da API
- **JUnit 5 & Mockito** - Testes unitários e de integração
- **Maven** - Gerenciamento de dependências

---

## ✨ Funcionalidades

### Gerenciamento de Usuários
- Cadastro com validação de CPF
- Login com CPF ou Email
- Autenticação JWT

### Cobranças
- Criar cobranças para outros usuários
- Consultar cobranças (enviadas/recebidas)
- Filtrar por status (Pendente, Paga, Cancelada)

### Pagamentos
- Pagar com saldo em conta
- Pagar com cartão de crédito
- Validação via autorizador externo
- Depósito em conta

### Cancelamento
- Cancelar cobranças pendentes
- Cancelar e estornar cobranças pagas

---

## 📦 Pré-requisitos

**Para executar com Docker (Recomendado):**
- [Docker](https://www.docker.com/get-started) 20.10+
- [Docker Compose](https://docs.docker.com/compose/install/) 2.0+

**Para executar localmente:**
- [Java JDK 21](https://adoptium.net/)
- [Maven 3.9+](https://maven.apache.org/download.cgi)
- [PostgreSQL 16](https://www.postgresql.org/download/)

---

## 🔧 Instalação e Execução

### ⚠️ IMPORTANTE: Configuração Inicial

**Antes de executar o projeto, configure o arquivo de propriedades:**
```bash
# Copie o arquivo de exemplo
cp src/main/resources/application.example.yml src/main/resources/application.yml

# Edite o application.yml com suas configurações
nano src/main/resources/application.yml  # ou use seu editor preferido
```

**Ajuste as seguintes propriedades no `application.yml`:**
- URL do banco de dados
- Credenciais do PostgreSQL
- Chave secreta do JWT (importante para produção!)
- URL do autorizador externo (se necessário)

---

### 🐳 Opção 1: Docker (Recomendado)

**Execução completa (PostgreSQL + API):**
```bash
# 1. Clone o repositório
git clone https://github.com/henrythuler/thuler_gateway.git
cd thuler-gateway

# 2. Configure o application.yml (veja aviso acima)

# 3. Inicie os serviços
docker-compose up -d

# 4. Acompanhe os logs (opcional)
docker-compose logs -f app

# 5. Acesse a aplicação
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

**Comandos úteis:**
```bash
# Parar serviços
docker-compose down

# Reconstruir a aplicação após mudanças
docker-compose build app

# Ver logs
docker-compose logs -f app
docker-compose logs -f postgres

# Limpar volumes (reseta o banco)
docker-compose down -v
```

---

### 💻 Opção 2: Execução Local
```bash
# 1. Clone o repositório
git clone https://github.com/henrythuler/thuler_gateway.git
cd thuler-gateway

# 2. Configure o PostgreSQL
psql -U postgres
CREATE DATABASE gateway_db;
CREATE USER gateway_user WITH PASSWORD 'gateway_pass';
GRANT ALL PRIVILEGES ON DATABASE gateway_db TO gateway_user;
\q

# 3. Configure o application.yml (veja aviso acima)

# 4. Execute a aplicação
./mvnw spring-boot:run

# Ou compile e execute o JAR
./mvnw clean package -DskipTests
java -jar target/gateway-1.0.0.jar
```

---

### 🗄️ Opção 3: PostgreSQL no Docker + App Local
```bash
# 1. Suba apenas o PostgreSQL
docker-compose up postgres -d

# 2. Configure o application.yml (veja aviso acima)

# 3. Execute a aplicação localmente
./mvnw spring-boot:run
```

---

## 📖 Documentação da API

### Swagger UI (Interativo)
Acesse: **http://localhost:8080/swagger-ui.html**

### Fluxo de Uso Básico

**1. Cadastrar usuário:**
```bash
POST /api/usuarios/cadastro
Content-Type: application/json

{
  "nome": "João Silva",
  "cpf": "52998224725",
  "email": "joao@example.com",
  "senha": "senha123"
}
```

**2. Fazer login:**
```bash
POST /api/usuarios/login
Content-Type: application/json

{
  "identificador": "joao@example.com",
  "senha": "senha123"
}
```

**3. Depositar (com token JWT):**
```bash
POST /api/conta/deposito
Authorization: Bearer {seu-token}
Content-Type: application/json

{
  "valor": 1000.00
}
```

**4. Criar cobrança:**
```bash
POST /api/cobrancas
Authorization: Bearer {seu-token}
Content-Type: application/json

{
  "cpfDestinatario": "12345678909",
  "valor": 100.00,
  "descricao": "Pagamento de serviço"
}
```

**5. Pagar cobrança:**
```bash
POST /api/cobrancas/pagar/saldo
Authorization: Bearer {token-destinatario}
Content-Type: application/json

{
  "cobrancaId": 1
}
```

**6. Consultar saldo:**
```bash
GET /api/conta/saldo
Authorization: Bearer {seu-token}
```

---

## 🧪 Testes
```bash
# Executar todos os testes
./mvnw test
```

---

## 🌐 Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `SPRING_DATASOURCE_URL` | URL do PostgreSQL | `jdbc:postgresql://localhost:5432/gateway_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `gateway_user` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `gateway_pass` |
| `JWT_SECRET` | Chave secreta JWT | *(definir no application.yml)* |
| `JWT_EXPIRATION` | Expiração do token (ms) | `86400000` (24h) |
| `AUTHORIZER_URL` | URL do autorizador | *(configurado)* |
