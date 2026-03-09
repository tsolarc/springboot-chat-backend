# API Documentation - Portfolio Chat

## WebSocket Endpoints

### Gateway WebSocket Routes

| Endpoint | Description | Protocol | Authentication |
|----------|-------------|----------|----------------|
| `ws://localhost:8000/ws/chat` | Main WebSocket chat endpoint | WebSocket | JWT Required (header) |
| `ws://localhost:8000/ws/events` | Events real-time notifications | WebSocket | JWT Required (header) |
| `ws://localhost:8000/ws/notifications` | User notifications endpoint | WebSocket | JWT Required (header) |

### Backend WebSocket Direct Access (for development)

| Endpoint | Description | Protocol | Authentication |
|----------|-------------|----------|----------------|
| `ws://localhost:8001/ws/chat` | Direct access to chat service | WebSocket | JWT Required (header) |
| `ws://localhost:8001/ws/events` | Direct access to events notifications | WebSocket | JWT Required (header) |

## Authentication and Security Endpoints

### Authentication

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|-------------|----------|
| POST | `http://localhost:8000/api/auth/login` | User authentication | `{"username": "string", "password": "string"}` | `{"token": "JWT_TOKEN", "refreshToken": "REFRESH_TOKEN", "expiresIn": 3600}` |
| POST | `http://localhost:8000/api/auth/register` | New user registration | `{"username": "string", "email": "string", "password": "string"}` | `{"id": 1, "username": "string", "email": "string"}` |
| POST | `http://localhost:8000/api/auth/refresh` | Refresh authentication token | `{"refreshToken": "REFRESH_TOKEN"}` | `{"token": "NEW_JWT_TOKEN", "refreshToken": "NEW_REFRESH_TOKEN", "expiresIn": 3600}` |
| POST | `http://localhost:8000/api/auth/logout` | User logout | `{"refreshToken": "REFRESH_TOKEN"}` | `{"message": "Logout successful"}` |

### User Management

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|-------------|----------|
| GET | `http://localhost:8000/api/users/me` | Get current user profile | - | `{"id": 1, "username": "string", "email": "string", ...}` |
| PUT | `http://localhost:8000/api/users/me` | Update current user profile | `{"username": "string", "email": "string", ...}` | `{"id": 1, "username": "string", "email": "string", ...}` |
| DELETE | `http://localhost:8000/api/users/me` | Delete current user account | - | `{"message": "Account deleted successfully"}` |

## WebSocket Authentication

To connect to WebSocket endpoints, you need to include a JWT token:

### STOMP WebSocket Headers (for chat)

```
Connect Headers:
{
  "Authorization": "Bearer YOUR_JWT_TOKEN",
  "heart-beat": "0,0"
}

Subscribe Headers:
{
  "id": "sub-0",
  "destination": "/topic/chat.room.1"
}

Send Headers:
{
  "destination": "/app/chat.message",
  "content-type": "application/json"
}

Message Body:
{
  "content": "Hello, world!",
  "roomId": 1
}
```

## Testing with Postman

### Authentication Testing

1. **Register a new user**:
    - POST to `http://localhost:8000/api/auth/register`
    - Body: `{"username": "testuser", "email": "test@example.com", "password": "securePassword123"}`

2. **Login with the user**:
    - POST to `http://localhost:8000/api/auth/login`
    - Body: `{"username": "testuser", "password": "securePassword123"}`
    - Save the returned JWT token

3. **Access protected resources**:
    - Add header: `Authorization: Bearer YOUR_JWT_TOKEN`
    - GET to `http://localhost:8000/api/users/me`

### WebSocket Testing

1. **Create a WebSocket connection**:
    - URL: `ws://localhost:8000/ws/chat`
    - Headers: `Authorization: Bearer YOUR_JWT_TOKEN`

2. **Subscribe to a channel**:
    - Use STOMP frame: `SUBSCRIBE`
    - Destination: `/topic/chat.room.1`

3. **Send a message**:
    - Use STOMP frame: `SEND`
    - Destination: `/app/chat.message`
    - Body: `{"content": "Test message", "roomId": 1}`

## Rate Limiting

API endpoints are protected by rate limiting:

- Authentication endpoints: 3 requests per minute
- User endpoints: 60 requests per minute
- WebSocket connections: 5 concurrent connections per user

## Error Responses

```json
{
  "timestamp": "2025-10-14T00:41:37-03:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error description",
  "path": "/api/resource"
}
```

## Security Considerations

- All endpoints except public authentication endpoints require valid JWT token
- Tokens expire after 1 hour and must be refreshed
- WebSocket connections require valid JWT authentication
- CORS is configured to allow requests from trusted origins only

This documentation provides a comprehensive overview of the available endpoints for testing in Postman. Actual implementation details may vary based on your specific application requirements.