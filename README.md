# Spring Boot Chat Backend

Backend API para una aplicación de chat en tiempo real construida con Spring Boot, WebSockets, Redis, RabbitMQ y PostgreSQL.

## 📋 Descripción

Este proyecto implementa un sistema de chat en tiempo real utilizando tecnologías modernas de mensajería y comunicación bidireccional. El sistema permite:

- **Comunicación en tiempo real** mediante WebSockets y STOMP
- **Mensajería asíncrona** con RabbitMQ para escalabilidad
- **Caché distribuido** con Redis para sesiones y rendimiento
- **Persistencia de datos** con PostgreSQL (PostGIS)
- **Autenticación y seguridad** con JWT y Spring Security

## 🏗️ Arquitectura

```
┌─────────────┐         WebSocket          ┌──────────────┐
│   Cliente   │ ←────────────────────────→ │ Spring Boot  │
└─────────────┘         STOMP/WS           │   Backend    │
                                            └───────┬──────┘
                                                    │
                    ┌───────────────────────────────┼───────────────────┐
                    │                               │                   │
              ┌─────▼─────┐              ┌─────────▼────┐      ┌──────▼──────┐
              │ RabbitMQ  │              │    Redis     │      │ PostgreSQL  │
              │ (Mensajes)│              │   (Cache)    │      │   (Datos)   │
              └───────────┘              └──────────────┘      └─────────────┘
```

### Componentes principales:

- **WebSockets**: Comunicación bidireccional en tiempo real entre clientes y servidor
- **RabbitMQ**: Message broker para desacoplar servicios y manejar mensajes asincrónicamente
- **Redis**: Cache distribuido para sesiones, tokens y datos temporales
- **PostgreSQL**: Base de datos relacional con extensión PostGIS para datos geoespaciales
- **Spring Security + JWT**: Autenticación y autorización con tokens JSON Web

## 🚀 Tecnologías

- **Java 17**
- **Spring Boot 3.4.5**
  - Spring WebSocket & STOMP
  - Spring Security & OAuth2
  - Spring Data JPA
  - Spring Data Redis
  - Spring AMQP (RabbitMQ)
  - Spring Actuator
- **PostgreSQL 16** con PostGIS 3.5
- **Redis** (cache y pub/sub)
- **RabbitMQ 3** con STOMP
- **Docker & Docker Compose**
- **Lombok** (reducción de boilerplate)
- **JWT** (autenticación)

## 📦 Estructura del Proyecto

```
springboot-chat-backend/
├── Backend/                    # Código fuente del backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Código Java
│   │   │   └── resources/     # Configuración
│   │   └── test/              # Tests
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yaml         # Orquestación de servicios
└── env.local.sample           # Plantilla de variables de entorno
```

## 🛠️ Configuración y Ejecución

### Prerrequisitos

- Docker y Docker Compose
- Java 17 (para desarrollo local)
- Maven (para desarrollo local)

### Variables de Entorno

Copia el archivo `env.local.sample` a `.env` y configura las siguientes variables:

```bash
# JWT Configuration
JWT_SECRET_KEY=<tu-token-de-seguridad-jwt>
JWT_EXPIRATION_DATE=900000  # 15 minutos

# Database Configuration (para desarrollo local)
DB_USER=postgres
DB_PASSWORD=1234
DATABASE_URL=jdbc:postgresql://localhost:5432/BeatMap_BDD
```

### Ejecutar con Docker Compose

```bash
# Levantar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f backend

# Detener servicios
docker-compose down
```

### Ejecutar en modo desarrollo (local)

```bash
cd Backend
./mvnw spring-boot:run
```

## 🔌 Servicios y Puertos

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| Backend | 8000 | API REST |
| Backend SSL | 8443 | API REST (HTTPS) |
| PostgreSQL | 5432 | Base de datos |
| Redis | 6379 | Cache |
| RabbitMQ | 5672 | AMQP |
| RabbitMQ Management | 15672 | UI de administración |
| RabbitMQ STOMP | 61613 | WebSocket STOMP |
| cAdvisor | 8082 | Métricas de contenedores |
| Jenkins | 8080 | CI/CD |

## 🔐 Seguridad

El proyecto implementa:

- **Autenticación JWT**: Tokens firmados con secreto configurable
- **Spring Security**: Filtros de seguridad y autorización
- **OAuth2**: Soporte para login con proveedores externos
- **CORS**: Configurado para comunicación con frontend
- **HTTPS**: Certificados SSL con Let's Encrypt

## 📊 Monitoreo

El stack de monitoreo incluye:

- **cAdvisor**: Métricas de contenedores Docker
- **PostgreSQL Exporter**: Métricas de la base de datos
- **Redis Exporter**: Métricas de Redis
- **RabbitMQ Exporter**: Métricas de RabbitMQ
- **Spring Actuator**: Métricas de la aplicación

## 🧪 Testing

```bash
cd Backend
./mvnw test
```

## 📝 API Endpoints

Los principales endpoints de la API incluyen:

- **WebSocket**: `ws://localhost:8000/ws` - Conexión WebSocket para chat en tiempo real
- **REST API**: `http://localhost:8000/api/...` - Endpoints REST para operaciones CRUD

## 🤝 Contribuir

1. Fork el proyecto
2. Crea tu rama de feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo licencia MIT.

## 👥 Autores

- BeatMap Team
