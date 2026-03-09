# Documentación de API - Portfolio Chat

## Endpoints WebSocket

### Rutas de WebSocket en Gateway

| Endpoint | Descripción | Protocolo | Autenticación |
|----------|-------------|----------|----------------|
| `ws://localhost:8000/dm` | Endpoint principal de WebSocket para chat | WebSocket | JWT Requerido (header) |
| `ws://localhost:8000/ws-event` | Notificaciones en tiempo real de eventos | WebSocket | JWT Requerido (header) |

### Acceso Directo a WebSocket Backend (para desarrollo)

| Endpoint | Descripción | Protocolo | Autenticación |
|----------|-------------|----------|----------------|
| `ws://localhost:8001/dm` | Acceso directo al servicio de chat | WebSocket | JWT Requerido (header) |
| `ws://localhost:8001/ws-event` | Acceso directo a notificaciones de eventos | WebSocket | JWT Requerido (header) |

## Endpoints de Chat HTTP

### Gestión de Chats

| Método | Endpoint | Descripción | Cuerpo de Solicitud | Respuesta |
|--------|----------|-------------|-------------|----------|
| GET | `http://localhost:8000/api/chat/direct/{submitterUserId}` | Obtiene o crea chat directo | - | `{"id": 1, "chatName": "DM: user1 & user2", "isPrivate": true, "chatType": "PRIVATE", ...}` |
| POST | `http://localhost:8000/api/chat` | Crea un chat privado entre dos usuarios | `{"user1Id": 1, "user2Id": 2}` | `{"id": 1, "chatName": "DM: user1 & user2", "isPrivate": true, ...}` |
| GET | `http://localhost:8000/api/chat/{chatId}/messages` | Obtiene historial de mensajes simple | - | `[{"id": 1, "content": "Hola", "sendedAt": "2025-10-14T00:41:37-03:00", ...}]` |

### Gestión de Mensajes

| Método | Endpoint | Descripción | Cuerpo de Solicitud | Respuesta |
|--------|----------|-------------|-------------|----------|
| GET | `http://localhost:8000/api/message/{chatId}/messages` | Obtiene historial de mensajes paginado | Parámetros: `page=0&size=20&sortBy=sendedAt&direction=desc` | Página de `{"id": 1, "content": "Hola", ...}` |
| GET | `http://localhost:8000/api/message/unread/{chatId}` | Obtiene mensajes no leídos | - | `[{"id": 1, "content": "Nuevo mensaje", ...}]` |
| POST | `http://localhost:8000/api/message` | Encola mensaje para envío (uso interno) | `{"chatId": 1, "content": "Hola", "type": "TEXT", ...}` | `202 Accepted` |
| PUT | `http://localhost:8000/api/message/{id}` | Encola edición de mensaje (uso interno) | `{"chatId": 1, "content": "Mensaje editado", ...}` | `202 Accepted` |
| DELETE | `http://localhost:8000/api/message/{id}` | Encola borrado de mensaje (uso interno) | - | `204 No Content` |

## Comunicación WebSocket (STOMP)

Para conectar a los endpoints WebSocket, necesitas incluir un token JWT:

### Cabeceras STOMP para WebSocket (chat)


### Destinos STOMP

#### Suscripción a Canales de Chat

| Destino | Descripción | Formato del Mensaje |
|---------|-------------|---------------------|
| `/topic/chat.{chatId}` | Canal principal de mensajes para un chat | `{"id": 1, "content": "Hola", "sender": {...}, "type": "TEXT", "sendedAt": "2025-10-14T00:41:37-03:00"}` |
| `/topic/chat.{chatId}.typing` | Notificaciones de escritura | `{"userId": 1, "username": "usuario1", "chatId": 1, "timestamp": "2025-10-14T00:41:37-03:00"}` |
| `/topic/chat.{chatId}.read` | Estado de lectura de mensajes | `{"messageId": 1, "userId": 1, "chatId": 1, "timestamp": "2025-10-14T00:41:37-03:00"}` |

#### Envío de Mensajes y Eventos

| Destino | Descripción | Cuerpo del Mensaje |
|---------|-------------|-------------------|
| `/app/chat/{chatId}/send` | Enviar nuevo mensaje | `{"content": "Hola mundo", "type": "TEXT"}` |
| `/app/chat/{chatId}/typing` | Notificar que el usuario está escribiendo | - |
| `/app/chat/{chatId}/read/{messageId}` | Marcar mensaje como leído | - |

## RabbitMQ (Interno)

El módulo utiliza RabbitMQ como broker de mensajes para desacoplar y escalar:

| Componente | Descripción |
|------------|-------------|
| Exchange | `chat.exchange` |
| Routing keys | `chat.{chatId}`, `chat.edit`, `chat.delete`, `chat.message` |
| Colas | `queue.chat.{chatId}` (una por chat) |
| Tipos de contenido | `chat.message`, `chat.typing`, `chat.read` |

## Pruebas con Postman

### Pruebas de Autenticación

1. **Registrar un nuevo usuario**:
    - POST a `http://localhost:8000/api/auth/register`
    - Cuerpo: `{"username": "usuario_prueba", "email": "prueba@ejemplo.com", "password": "contraseñaSegura123"}`

2. **Iniciar sesión con el usuario**:
    - POST a `http://localhost:8000/api/auth/login`
    - Cuerpo: `{"username": "usuario_prueba", "password": "contraseñaSegura123"}`
    - Guarda el token JWT devuelto

3. **Acceder a recursos protegidos**:
    - Añade cabecera: `Authorization: Bearer TU_TOKEN_JWT`
    - GET a `http://localhost:8000/api/users/me`

### Pruebas de WebSocket

1. **Crear una conexión WebSocket**:
    - URL: `ws://localhost:8000/dm`
    - Cabeceras: `Authorization: Bearer TU_TOKEN_JWT`

2. **Suscribirse a un canal**:
    - Usar frame STOMP: `SUBSCRIBE`
    - Destino: `/topic/chat.1`
    - ID: `sub-0`

3. **Enviar un mensaje**:
    - Usar frame STOMP: `SEND`
    - Destino: `/app/chat/1/send`
    - Cuerpo: `{"content": "Mensaje de prueba", "type": "TEXT"}`

4. **Enviar notificación de escritura**:
    - Usar frame STOMP: `SEND`
    - Destino: `/app/chat/1/typing`

5. **Marcar mensaje como leído**:
    - Usar frame STOMP: `SEND`
    - Destino: `/app/chat/1/read/123` (donde 123 es el ID del mensaje)

## Control de Tasa (Rate Limiting)

Los endpoints están protegidos por límites de tasa:

- Conexiones WebSocket: 5 conexiones concurrentes por usuario
- Mensajes WebSocket: 10 mensajes por segundo por usuario

Fuente: `src/main/java/com/Backend/Chat/config/WebSocket/WebSocketRateLimitingConfig.java` y `src/main/java/com/Backend/Chat/Interceptor/RateLimitingChannelInterceptor.java`

## Respuestas de Error

```json
{
  "timestamp": "2025-10-14T00:41:37-03:00",
  "status": 400,
  "error": "Solicitud Incorrecta",
  "message": "Descripción del error",
  "path": "/api/recurso"
}
```
