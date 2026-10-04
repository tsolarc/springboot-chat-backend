# Microservicio de mensajería

API de conversaciones y mensajes para clientes móviles. PostgreSQL conserva el historial; RabbitMQ distribuye eventos; WebSocket/STOMP los entrega en vivo. La app se conecta a la API, no directamente a RabbitMQ.

## Ejecutar localmente

Requiere Docker Compose. Copia `env.local.sample` a `.env`, configura un `JWT_SECRET_KEY` propio y ejecuta:

```bash
docker compose up --build -d
docker compose ps
```

La API escucha en `http://localhost:8001`. Flyway crea o migra el esquema. El contenedor comprueba `/health`; Compose concede 40 segundos para apagarlo de forma ordenada.

## Flujo de mensajes

1. `POST /v1/conversations/{chatId}/messages` valida JWT y membresía. El cliente genera `clientMessageId` y lo reutiliza si debe reintentar.
2. El mensaje y su evento outbox se guardan en una sola transacción.
3. `db-scheduler` coordina las tareas entre instancias. Cada worker reserva un evento con `FOR UPDATE SKIP LOCKED`, libera la transacción y espera la confirmación de RabbitMQ. Una segunda transacción registra el resultado y su auditoría.
4. El consumidor publica en `/topic/chat.{chatId}` mediante el relay STOMP. Todas las instancias pueden entregar el evento a sus sesiones.
5. Al reconectar, el cliente consulta el historial por HTTP y deduplica por `messageId`. La publicación de eventos es al menos una vez; un fallo entre confirmación y persistencia puede repetir un `eventId`.

Los eventos fallidos usan reintentos con espera creciente y variación aleatoria. Al agotar el límite quedan marcados con `dead_lettered_at` para revisión. Las reservas vencidas permiten recuperar trabajo de un proceso caído. La auditoría conserva usuario, conversación, mensaje, `clientMessageId`, operación, origen y `requestId`, sin guardar cuerpos de mensajes ni tokens.

## Pruebas

Requiere Java 17. La suite habitual usa H2 y mocks:

```bash
cd Backend
./mvnw test
```

La suite de producción levanta PostgreSQL 16, RabbitMQ 4 con STOMP y Redis 7 en contenedores desechables; requiere Docker activo:

```bash
./mvnw -Pintegration verify
```

Comprueba migraciones, bloqueo concurrente, reservas vencidas, límites de reintento, auditoría transaccional, confirmaciones y rutas AMQP, caída y recuperación de RabbitMQ, cola de errores y dos instancias con clientes WebSocket. Las dos pruebas WebSocket antiguas siguen omitidas; la nueva suite ejercita el relay real. El perfil de integración falla si Docker no está disponible.

## Documentación

- [Contrato HTTP y WebSocket](docs/API.md)
- [Operación, salud, métricas y variables](docs/OPERACION.md)
- [Diseño y alcance pendiente](docs/PROPUESTA_MICROSERVICIO_MENSAJERIA.md)

La cuenta de monitoreo se configura con `MONITORING_USERNAME` y `MONITORING_PASSWORD`. Sin contraseña, las rutas de métricas e información permanecen cerradas. Las rutas de salud son públicas y no exponen detalles internos.
