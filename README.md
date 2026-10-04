# Microservicio de mensajería

API de mensajería para clientes móviles. PostgreSQL conserva conversaciones y mensajes; RabbitMQ distribuye eventos en tiempo real; WebSocket/STOMP los entrega a los clientes. La app móvil se conecta a la API, no a RabbitMQ.

## Ejecutar localmente

Requiere Docker Compose. Copia `env.local.sample` a `.env`, define un `JWT_SECRET_KEY` propio y ejecuta:

```bash
docker compose up --build -d
docker compose ps
```

La API escucha en `http://localhost:8001`. Flyway crea el esquema en PostgreSQL. RabbitMQ habilita STOMP; Redis se usa para limitación de conexiones y caché.

## Flujo de mensajes

1. `POST /v1/conversations/{chatId}/messages` valida el JWT y la membresía. El cliente envía `clientMessageId` para que un reintento no duplique el mensaje.
2. El mensaje y su evento de outbox se guardan en una sola transacción PostgreSQL.
3. `db-scheduler` ejecuta `publish-message-outbox` cada segundo. La tabla `scheduled_tasks` coordina el trabajo entre instancias; el publicador confirma la entrega a RabbitMQ y reintenta los errores.
4. El consumidor de eventos reenvía el evento al destino STOMP `/topic/chat.{chatId}`. Tras reconectar, el cliente recupera mensajes con `GET /v1/conversations/{chatId}/messages?before=...`.

El historial, la lista de conversaciones y el cursor de lectura están bajo `/v1/conversations`. El WebSocket usa `/dm` y requiere JWT en `CONNECT`. Ver [propuesta técnica](docs/PROPUESTA_MICROSERVICIO_MENSAJERIA.md) para el diseño y las decisiones pendientes.

## Diagnóstico de tareas

`scheduled_task_log` guarda las ejecuciones del worker con estado, instancia, inicio, fin, duración y contadores. `scheduled_task_event_log` guarda cada intento de publicación con `event_id`, usuario, conversación, mensaje, `clientMessageId`, operación, origen, número de intento y error. El origen es la ruta HTTP cuando existe; para eventos creados por STOMP se registra el servicio que ejecutó la operación. El job es disparado por horario, por eso su `trigger_type` es `SCHEDULED` y no tiene un usuario propio. No se guardan cuerpos de mensajes ni tokens en estas tablas. Los ciclos sin eventos pendientes no generan filas de auditoría para evitar crecimiento continuo.

## Pruebas

```bash
cd Backend
./mvnw test
```

La suite unitaria usa H2 y desactiva `db-scheduler`; la migración y la coordinación entre instancias requieren una prueba de integración con PostgreSQL y RabbitMQ. Las pruebas WebSocket de integración existentes están omitidas.

## Variables principales

| Variable | Uso |
| --- | --- |
| `JWT_SECRET_KEY` | Firma de tokens JWT |
| `DATABASE_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL |
| `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | RabbitMQ |
| `RABBITMQ_STOMP_PORT` | Relay STOMP |
| `DB_SCHEDULER_POLLING_INTERVAL`, `DB_SCHEDULER_THREADS` | Frecuencia y concurrencia del scheduler |
| `SERVER_PORT` | Puerto HTTP, por defecto 8001 |

Los nombres de dominio y puertos se configuran por entorno. No están fijados en `AppConstants`.
