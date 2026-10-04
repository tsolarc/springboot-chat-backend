# Operación del servicio

## Configuración

Copiar `env.local.sample` a `.env`. Compose transmite explícitamente estas variables al backend. Todos los nodos deben usar la misma configuración de workers.

| Variable | Valor predeterminado | Uso |
| --- | --- | --- |
| `JWT_SECRET_KEY` | Requerida | Firma JWT; utilizar un secreto propio de al menos 32 bytes |
| `JWT_EXPIRATION_DATE` | `900000` en Compose | Duración JWT en milisegundos |
| `DB_PASSWORD`, `RABBITMQ_PASSWORD` | `localdev` en Compose | Credenciales locales; reemplazarlas en despliegues |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Orígenes permitidos |
| `MONITORING_USERNAME` | `prometheus` | Usuario exclusivo de monitoreo |
| `MONITORING_PASSWORD` | Vacía | Habilita monitoreo cuando se configura |
| `DB_POOL_SIZE` | `15` | Máximo de conexiones PostgreSQL por instancia |
| `DB_SCHEDULER_THREADS` | `2` | Tareas simultáneas por instancia |
| `DB_SCHEDULER_POLLING_INTERVAL` | `1s` | Consulta de tareas pendientes |
| `OUTBOX_WORKERS` | `4` | Tareas de publicación compartidas entre instancias |
| `OUTBOX_BATCH_SIZE` | `50` | Máximo de eventos por ejecución de una tarea |
| `OUTBOX_MAX_ATTEMPTS` | `12` | Límite de intentos por evento |
| `OUTBOX_CONFIRM_TIMEOUT` | `5s` | Espera de confirmación AMQP |
| `OUTBOX_LEASE_DURATION` | `30s` | Reserva temporal del evento; al menos dos veces el timeout |
| `OUTBOX_POLL_INTERVAL` | `1s` | Espera entre ejecuciones de cada tarea |

Fuera de Compose también deben definirse `DATABASE_URL`, `DB_USER`, `DB_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_STOMP_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `REDIS_HOST` y `REDIS_PORT`. El puerto HTTP se configura con `SERVER_PORT`, por defecto `8001`.

## Salud y métricas

- `/livez`: vida del proceso, independiente de servicios externos.
- `/health`: disponibilidad HTTP y PostgreSQL. La API puede seguir persistiendo mensajes cuando RabbitMQ está caído; por eso el broker no invalida esta señal.
- `/actuator/health`: estado agregado, incluido RabbitMQ, Redis y el relay STOMP, sin detalles públicos.
- `/actuator/prometheus` y `/actuator/info`: cuenta de monitoreo por HTTP Basic. Con contraseña vacía no hay cuenta habilitada. Utilizar HTTPS o la red privada del despliegue.

Para Prometheus, configurar `basic_auth.username` y `basic_auth.password_file` con la credencial de monitoreo. No reutilizar contraseñas de usuarios de chat.

Las métricas `messaging_outbox_publications_total` y `messaging_outbox_publish_duration_seconds` distinguen `outcome=success` y `outcome=failure`. Representan intentos de transporte, no confirmaciones de lectura ni entrega exacta al usuario. Revisar además la cola `chat.dead.queue` y el backlog de PostgreSQL.

El healthcheck de Compose consulta `/health`. Docker marca el contenedor como no saludable si falla; ese estado por sí solo no reinicia el proceso. El apagado tiene un plazo de 30 segundos y Compose espera 40 antes de forzarlo.

## Diagnóstico y recuperación

Buscar `X-Request-Id` en logs HTTP, `message_outbox.origin_request_id` y `scheduled_task_event_log.origin_request_id`. `scheduled_task_log` registra cada ejecución con trabajo; `scheduled_task_event_log` registra cada transición confirmada del outbox. Una transacción revertida no deja un éxito falso en auditoría.

Los eventos reservados se recuperan cuando vence `claimed_until`. Si un proceso cae después de publicar y antes de guardar el resultado, puede repetirse la publicación: los consumidores y clientes deben deduplicar.

Al agotar reintentos, `message_outbox.dead_lettered_at` detiene nuevos intentos. Corregir primero el error de infraestructura o contenido, revisar la auditoría y reactivar únicamente el evento elegido con una operación administrativa acotada. No hay endpoint público de reproceso. Los mensajes rechazados por el consumidor AMQP pasan a `chat.dead.queue` tras los reintentos configurados; tampoco se reproducen automáticamente desde esa cola.

Un reinicio abrupto puede dejar auditorías `RUNNING`; la reserva del evento sí vence y se recupera. La retención de auditoría y outbox publicado todavía requiere una política operativa: estas tablas no se purgan automáticamente.

## Validación antes de desplegar

Ejecutar `./mvnw -Pintegration verify` en `Backend` con Java 17 y Docker activo. Testcontainers crea y elimina infraestructura aislada; no utiliza las bases de datos ni los volúmenes de Compose. Los informes quedan en `Backend/target/surefire-reports` y `Backend/target/failsafe-reports`.

La suite comprueba la actualización de V4 a V5 con eventos existentes, workers concurrentes y reservas vencidas, auditoría y límites de reintento, confirmaciones AMQP, caída y recuperación real de RabbitMQ, dead letters y dos instancias de API con clientes STOMP y reintento HTTP idempotente.
