# Contrato de mensajería

Base local: `http://localhost:8001`. En producción se utiliza HTTPS/WSS. Las rutas privadas requieren `Authorization: Bearer <JWT>`. Actualmente el servicio emite JWT mediante `POST /auth/login`; la integración con un proveedor externo sigue pendiente.

## HTTP para clientes móviles

| Método y ruta | Entrada | Respuesta |
| --- | --- | --- |
| `POST /v1/conversations/direct` | `{"otherUserId": 2}` | `200`, conversación existente o creada |
| `GET /v1/conversations?before=&limit=20` | Cursor opaco opcional | `{"conversations": [...], "nextCursor": "..."}` |
| `POST /v1/conversations/{chatId}/messages` | `{"clientMessageId": "UUID", "content": "Hola"}` | `201` al crear; `200` al repetir el mismo envío |
| `GET /v1/conversations/{chatId}/messages?before=&limit=20` | Cursor opaco opcional | `{"messages": [...], "nextCursor": "..."}` |
| `PUT /v1/conversations/{chatId}/read-cursor` | `{"messageId": 123}` | Cursor de lectura monotónico |

El contenido admite entre 1 y 5000 caracteres. El remitente se obtiene del JWT. La respuesta del mensaje incluye `id`, `chatId`, `clientMessageId`, `content`, `sender`, `type`, `sendedAt` y `deletedAt`; no expone contraseñas. La paginación ordena por fecha e ID. Un `nextCursor` nulo indica que no hay otra página.

Las operaciones de edición y borrado conservan sus rutas existentes: `PUT /message/{id}` (DTO con `chatId`, `content`, `type`; devuelve `200`) y `DELETE /message/{id}` (`204`). Ambas requieren autoría. Todavía no existen las rutas propuestas `/v1/messages/{id}`.

## WebSocket y reconexión

Conectar un cliente SockJS a `/dm`, enviar el JWT en el header `Authorization` del frame STOMP `CONNECT` y suscribirse a `/topic/chat.{chatId}`. El servidor valida membresía para suscripciones y envíos.

Los eventos persistidos son `message.created`, `message.updated`, `message.deleted` y `conversation.read`. Incluyen `eventId`, `schemaVersion`, `eventType` y `conversationId`; los eventos de mensaje incluyen `messageId`, `clientMessageId`, `senderId`, `content` y `occurredAt`.

La entrega es al menos una vez: deduplicar eventos por `eventId` y mensajes por `messageId`. Tras una desconexión, recuperar el historial por HTTP; la suscripción no reproduce por sí sola los eventos perdidos. Un reintento HTTP debe reutilizar el mismo `clientMessageId`.

El envío STOMP opcional usa `/app/chat/{chatId}/send` con el mismo cuerpo que el envío HTTP. Lectura y escritura efímera conservan `/app/chat/{chatId}/read/{messageId}` y `/app/chat/{chatId}/typing`.

## Errores y correlación

Enviar opcionalmente `X-Request-Id` (1–64 caracteres: letras, números, punto, guion o guion bajo). Un valor ausente o inválido se reemplaza por un UUID. La respuesta siempre incluye ese header; CORS permite leerlo desde el cliente.

Los errores manejados por la API y la seguridad utilizan `application/problem+json` con `status`, `title`, `detail`, `code`, `message` y `requestId`. Las validaciones agregan `errors` por campo. Los códigos HTTP principales son `400`, `401`, `403`, `404` y `409`. Algunas respuestas de las rutas antiguas `/message` conservan un cuerpo vacío en caso de `403`.

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required",
  "code": "UNAUTHORIZED",
  "message": "Authentication is required",
  "requestId": "mobile-ticket-123"
}
```

## Salud y monitoreo

`GET /health` devuelve `200` si la API está lista y PostgreSQL está disponible; `503` si no lo está. `GET /livez` comprueba la vida del proceso. Ambos son públicos y ocultan detalles.

`GET /actuator/prometheus` y `GET /actuator/info` requieren la cuenta de monitoreo por HTTP Basic. Sus credenciales no autorizan operaciones de chat; los JWT de los usuarios no autorizan monitoreo. Ver [operación](OPERACION.md).
