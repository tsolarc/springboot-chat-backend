# Backend de mensajería

Servicio Spring Boot con Java 17, PostgreSQL, RabbitMQ y Redis. Puerto HTTP local: **8001**. WebSocket/SockJS: **`/dm`**; autenticación JWT en el frame STOMP `CONNECT`.

La guía de instalación y pruebas está en el [README principal](../README.md). El contrato vigente está en [docs/API.md](../docs/API.md); las variables, credenciales de monitoreo y procedimientos de recuperación están en [docs/OPERACION.md](../docs/OPERACION.md).

Desde esta carpeta:

```bash
./mvnw test
./mvnw -Pintegration verify  # requiere Docker activo
```
