# 🛒 E-commerce API - Spring Boot

API REST completa para sistema de e-commerce B2C (Business to Consumer) desenvolvida com Spring Boot.

## 📋 Funcionalidades

### ✅ Implementadas

- **Autenticação e Autorização**
  - Registro e login de usuários
  - JWT (JSON Web Token)
  - Roles (CUSTOMER, ADMIN)

- **Gestão de Categorias**
  - CRUD completo
  - Validações

- **Gestão de Produtos**
  - CRUD completo
  - Paginação e filtros
  - Busca por nome
  - Filtro por categoria e faixa de preço
  - Controle de estoque

- **Gestão de Endereços**
  - CRUD completo
  - Múltiplos endereços por usuário
  - Endereço padrão

- **Carrinho de Compras**
  - Adicionar/remover produtos
  - Atualizar quantidades
  - Validação de estoque

- **Gestão de Pedidos**
  - Checkout completo
  - Histórico de pedidos
  - Cancelamento com devolução de estoque
  - Status do pedido

- **Sistema de Pagamento**
  - Múltiplos métodos (PIX, Cartão, Boleto)
  - Simulação de aprovação/rejeição
  - Reembolso

- **Sistema de Avaliações**
  - Avaliar produtos comprados
  - Rating de 1 a 5 estrelas
  - Comentários

## 🛠️ Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 3.3.4**
- **Spring Data JPA**
- **Spring Security**
- **JWT (jjwt 0.12.5)**
- **PostgreSQL**
- **Lombok**
- **ModelMapper**
- **SpringDoc OpenAPI (Swagger)**
- **JUnit 5 & Mockito**
- **Maven**

## 🚀 Como Executar

### Pré-requisitos

- Java 17 ou superior
- PostgreSQL 12 ou superior
- Maven 3.6 ou superior

### Passo 2: Configurar o Banco de Dados

Crie o banco de dados no PostgreSQL:
```sql
CREATE DATABASE ecommerce_db;
```

### Passo 3: Configurar application.properties

Edite `src/main/resources/application-dev.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_db
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
```

### Passo 4: Executar a Aplicação
```bash
./mvnw spring-boot:run
```

ou
```bash
mvn spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8080`

### Passo 5: Acessar a Documentação

Swagger UI: `http://localhost:8080/swagger-ui.html`

## 📚 Documentação da API

### Endpoints Públicos

- `POST /api/auth/register` - Registrar usuário
- `POST /api/auth/login` - Login
- `GET /api/products` - Listar produtos
- `GET /api/products/{id}` - Detalhes do produto
- `GET /api/categories` - Listar categorias
- `GET /api/products/{id}/reviews` - Listar avaliações

### Endpoints Autenticados (CUSTOMER)

- `GET /api/addresses` - Listar meus endereços
- `POST /api/addresses` - Criar endereço
- `GET /api/cart` - Ver carrinho
- `POST /api/cart/items` - Adicionar ao carrinho
- `POST /api/orders` - Criar pedido (checkout)
- `GET /api/orders` - Meus pedidos
- `POST /api/payments/process` - Processar pagamento
- `POST /api/products/{id}/reviews` - Avaliar produto

### Endpoints Administrativos (ADMIN)

- `POST /api/products` - Criar produto
- `PUT /api/products/{id}` - Atualizar produto
- `DELETE /api/products/{id}` - Deletar produto
- `POST /api/categories` - Criar categoria
- `GET /api/orders/admin/all` - Listar todos pedidos
- `PUT /api/orders/{id}/status` - Atualizar status do pedido

## 🧪 Executar Testes
```bash
./mvnw test
```

## 🏗️ Estrutura do Projeto
```
src/
├── main/
│   ├── java/com/seudominio/ecommerce/
│   │   ├── config/          # Configurações
│   │   ├── controller/      # Controllers REST
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── exception/       # Exceções customizadas
│   │   ├── model/           # Entidades JPA
│   │   ├── repository/      # Repositórios
│   │   ├── security/        # Configuração de segurança
│   │   └── service/         # Lógica de negócio
│   └── resources/
│       ├── application.properties
│       ├── application-dev.properties
│       └── application-prod.properties
└── test/                    # Testes unitários e integração
```

## 🔒 Segurança

- Senhas criptografadas com BCrypt
- Autenticação via JWT
- Proteção contra SQL Injection
- Validação de dados
- CORS configurado

## 📝 Variáveis de Ambiente (Produção)
```bash
DATABASE_URL=jdbc:postgresql://host:5432/database
DATABASE_USERNAME=usuario
DATABASE_PASSWORD=senha
JWT_SECRET=sua-chave-secreta-base64
```

## 🐛 Health Check

- `GET /actuator/health` - Status da aplicação
- `GET /actuator/info` - Informações da aplicação
