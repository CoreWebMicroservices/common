# Core Microservices Common Libraries

Shared libraries and utilities for all Core Microservices.

## Modules

| Module | Description |
|--------|-------------|
| `api` | OpenAPI codegen support, common API resources |
| `exception` | Global exception handling, error responses |
| `logging` | Structured logging with MDC, request tracing |
| `security` | JWT authentication, role-based authorization |
| `queue` | RabbitMQ integration, message handling |
| `inbound-client` | WebClient configuration for service-to-service calls |
| `observability` | Health checks, metrics, Prometheus integration |
| `utils/db-utils` | Database utilities, pagination, search |

## Prerequisites

Install the parent POM first:

```bash
cd ../parent
mvn clean install
```

## Installation

```bash
mvn clean install
```

This installs all common modules to your local Maven repository.

## Usage

Add dependencies in your service POM:

```xml
<dependencies>
    <!-- Security with JWT -->
    <dependency>
        <groupId>com.corems.common</groupId>
        <artifactId>security</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>com.corems.common</groupId>
        <artifactId>logging</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- Database utilities -->
    <dependency>
        <groupId>com.corems.common</groupId>
        <artifactId>db-utils</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Module Details

### Security
- JWT token generation and validation
- `@RequireRoles` annotation for role-based access
- `SecurityUtils` for accessing current user
- OAuth2 client support

### Logging
- Log4j2 configuration
- MDC context propagation
- Request/response logging filter

### Exception
- Global `@ControllerAdvice` exception handler
- Standardized error response format
- Validation error handling

### Queue
- RabbitMQ auto-configuration
- Message serialization/deserialization
- Dead letter queue support

### DB Utils
- `SearchableRepository` interface
- `PaginatedQueryExecutor` for listing endpoints
- Dynamic filtering and sorting
