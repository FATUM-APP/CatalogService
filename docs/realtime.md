# Tiempo real (STOMP sobre WebSocket)

## Qué hay

| Pieza | Archivo | Rol |
|---|---|---|
| Endpoint y broker | `realtime/WebSocketConfig.kt` | `wss://<host>/ws/catalog`, broker simple, prefijo `/user` |
| Eventos de dominio | `realtime/event/RealtimeEvents.kt` | `OrderChangedEvent`, `CalendarChangedEvent` |
| Adaptador a STOMP | `realtime/RealtimeEventListener.kt` | `@TransactionalEventListener(AFTER_COMMIT)` |
| Destinos y mensajes | `realtime/RealtimeTopics.kt` | `/topic/calendar/{providerId}`, `/queue/orders` |
| CORS | `configuration/CorsConfig.kt` | Lee `app.cors.allowed-origins` (misma propiedad del handshake) |

Los servicios publican eventos de dominio; no conocen WebSocket. Si la transacción hace
rollback, no se envía nada.

## Destinos

| Destino | Quién escucha | Cuándo |
|---|---|---|
| `/topic/calendar/{providerId}` | cualquiera | el proveedor cambió su horario o su rango no disponible, o cambió una orden |
| `/user/queue/orders` | el cliente **y** el proveedor de la orden | orden creada, aceptada, rechazada, cancelada o con una confirmación nueva |

## Mensajes

```json
// /topic/calendar/prov-123
{ "type": "CALENDAR_CHANGED", "at": "2026-11-12T15:04:00",
  "payload": { "providerId": "prov-123", "reason": "ORDER_UPDATED" } }

// /user/queue/orders
{ "type": "ORDER_UPDATED", "at": "2026-11-12T15:04:00",
  "payload": { ...mismo OrderDTO de GET /order/{id}... } }
```

`type` para órdenes: `ORDER_CREATED`, `ORDER_UPDATED`, `ORDER_CANCELLED`.
`reason` para calendario: `AVAILABILITY_CHANGED`, `UNAVAILABILITY_CHANGED`, `ORDER_CREATED`,
`ORDER_UPDATED`, `ORDER_CANCELLED`.

El calendario no viaja en el mensaje: el cliente vuelve a pedir el mes que está viendo.

## Configuración

```yaml
app:
  cors:
    allowed-origins: ${ALLOWED_ORIGINS:http://localhost:5173}
```

La misma propiedad se usa para el CORS HTTP y para el handshake del WebSocket.

## Autenticación

El `CONNECT` exige `Authorization: Bearer <token de Cognito>`. El `sub` del token es el usuario y
se usa para resolver `/user/...`. Si falta el token o no valida, la conexión se cierra.

```js
connectHeaders: { Authorization: `Bearer ${accessToken}` }
```

Con `SECURITY_ENABLED=false` (solo desarrollo) se acepta la cabecera `X-User-Id` en su lugar.

Detalle de roles y endpoints en [authorization.md](authorization.md).

## Cliente (JS)

```js
import { Client } from "@stomp/stompjs";

const client = new Client({
  brokerURL: "wss://api.tu-dominio/ws/catalog",
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  reconnectDelay: 3000,
  onConnect: () => {
    client.subscribe("/user/queue/orders", msg => upsertOrder(JSON.parse(msg.body).payload));
    client.subscribe(`/topic/calendar/${providerId}`, () => reloadCalendar());
  }
});
client.activate();
```

Se suscribe **antes** de pedir los datos y, al reconectar, vuelve a pedirlos: el canal no guarda
historial.

## Alcance

Una instancia (broker simple en memoria). Con dos o más instancias hace falta un broker relay
(RabbitMQ) para que un cliente conectado a la instancia A reciba eventos de la B.
