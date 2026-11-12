# Tiempo real por SSE

Dos canales, uno por servicio. Los dos son `text/event-stream` y los dos piden el token de Cognito,
porque `EventSource` no puede mandar cabeceras y el usuario tiene que salir de algo: se acepta
`?access_token=` además de `Authorization`.

| Servicio | Ruta | Qué llega |
|---|---|---|
| CatalogService | `GET /realtime/orders` | Los cambios de las órdenes del usuario del token |
| CatalogService | `GET /realtime/calendar?providerId=X` | Los cambios del calendario de ese proveedor |
| NotificationService | `GET /notifications/stream` | Las notificaciones nuevas del usuario del token |

En los de órdenes y notificaciones el usuario sale del `sub` del token: nadie puede pedir el canal
de otra persona cambiando un parámetro. El calendario es de un proveedor concreto y lo abren él y
sus clientes, así que ahí sí va el `providerId` en la URL; el token sigue siendo obligatorio.

## Qué eventos llegan por `/realtime/orders`

Cada mensaje viene con un `type` y el contenido del evento que se publicó a EventBridge.

```json
event: order
data: {"type":"ORDER_STATUS_CHANGED","orderId":"order-1","clientId":"client-1",
       "providerId":"provider-1","status":"ACCEPTED","reason":null}
```

| `type` | Cuándo | A quién |
|---|---|---|
| `ORDER_CREATED` | Se creó una orden | Al proveedor |
| `ORDER_STATUS_CHANGED` | Aceptada, rechazada, cancelada | A las dos partes |
| `ORDER_CONFIRMATION_CHANGED` | Alguien confirmó o negó un paso | A las dos partes |

Los tres llevan `orderId`, así que el front puede actualizar la fila concreta de la lista en vez de
recargarla entera.


## Qué llega por `/realtime/calendar`

El calendario lo miran el proveedor y sus clientes al mismo tiempo, y los dos comparten un mismo
tema: cuando el proveedor cambia sus horarios, todos lo ven en el mismo instante.

```json
event: calendar
data: {"type":"CALENDAR_CHANGED","providerId":"provider-1",
       "change":"UNAVAILABLE_ADDED","start":"2026-11-20","end":"2026-11-22"}
```

| `change` | Cuándo | `start` / `end` |
|---|---|---|
| `WEEKLY_SCHEDULE_CHANGED` | Cambió, borró o puso a jornada completa un día de la semana | `null` |
| `UNAVAILABLE_ADDED` | Bloqueó unos días | Las fechas del bloqueo |
| `UNAVAILABLE_CHANGED` | Movió un bloqueo | Las fechas nuevas |
| `UNAVAILABLE_REMOVED` | Quitó un bloqueo | `null` |
| `ORDER_UPDATED` | Se creó una orden o cambió de estado: el hueco se ocupó o se liberó | `null` |

Con `start` y `end` el front puede pedir solo esos días y repintarlos; si vienen en `null`, pide el
mes que está mostrando. Es más simple y siempre correcto que intentar adivinar el estado nuevo.

```js
const calendar = new EventSource(
  `${base}/realtime/calendar?providerId=${providerId}&access_token=${token}`
);
calendar.addEventListener('calendar', (e) => {
  const change = JSON.parse(e.data);
  if (change.start && change.end) reloadDays(change.start, change.end);
  else reloadCurrentMonth();
});
```

## Qué llega por `/notifications/stream`

```json
event: notification
data: {"id":"...","type":"ORDER_STATUS_CHANGED","title":"Tu orden fue aceptada",
       "body":"...","read":false,"createdAt":"..."}
```

Es el mismo objeto que devuelve `GET /notifications`, así que el front puede meterlo en la lista sin
transformarlo.

## Cómo se conecta el front

```js
const base = 'https://api.tudominio.com';
const token = await getAccessToken();   // el mismo que ya usas para la API

const orders = new EventSource(`${base}/realtime/orders?access_token=${token}`);
orders.addEventListener('order', (e) => {
  const event = JSON.parse(e.data);
  applyOrderChange(event);              // actualiza la lista o el detalle
});

const inbox = new EventSource(`${base}/notifications/stream?access_token=${token}`);
inbox.addEventListener('notification', (e) => {
  const notification = JSON.parse(e.data);
  showToast(notification);
  addToInbox(notification);
});

// Cuando el usuario cierra sesión o el token expira:
orders.close();
inbox.close();
```

`EventSource` reconecta solo si la conexión se corta. Lo único que hay que hacer es cerrarlo al salir.

## Latido y reconexión

Cada 20 segundos el servidor manda un comentario (`: ping`) que el navegador ignora. Sirve para que
el ALB no corte la conexión por inactividad: su timeout por defecto son 60 segundos. Si cambias el
latido, que siga por debajo del idle timeout del balanceador.

La conexión dura 30 minutos y luego el servidor la cierra; el navegador abre otra sola. No hay que
hacer nada en el front.

## Variables

| Variable | Servicio | Por defecto |
|---|---|---|
| `fatum.realtime.timeout-ms` | Catálogo | `1800000` (30 min) |
| `fatum.realtime.heartbeat-ms` | Catálogo | `20000` |
| `fatum.realtime.max-connections-per-user` | Catálogo | `5` |
| `fatum.sse.timeout-ms` | Notificaciones | `1800000` |
| `fatum.sse.heartbeat-ms` | Notificaciones | `20000` |
| `fatum.sse.max-connections-per-user` | Notificaciones | `5` |

Si un usuario abre más pestañas que el máximo, se cierra la más vieja en vez de rechazar la nueva.

## Una tarea por servicio

Las conexiones viven en memoria de la instancia que las atiende. Con una sola tarea por servicio
alcanza y es lo que hay que tener mientras esto sea pequeño:

- En el catálogo, la tarea que guarda la orden es la que avisa a los navegadores conectados a ella.
- En notificaciones, SQS le entrega cada mensaje a una sola tarea, y solo los usuarios conectados a
  esa tarea lo ven.

Con dos tareas cada servicio, la mitad de los usuarios no recibiría la mitad de los eventos. Si algún
día hace falta escalar, el paso es publicar en Redis (ElastiCache) y que cada instancia se suscriba y
repita a sus propias conexiones. El `SseHub` es el único sitio que habría que tocar.
