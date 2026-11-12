# Tiempo real en el frontend

## 1. Dependencia

```bash
npm install @stomp/stompjs
```

`@stomp/stompjs` usa el `WebSocket` nativo del navegador. No necesitas `sockjs-client`: el ALB
habla WebSocket real y el servidor no tiene fallback SockJS.

## 2. El token

El `CONNECT` exige el access token de Cognito (`Authorization: Bearer ...`). Con Amplify:

```ts
import { fetchAuthSession } from "aws-amplify/auth";

const session = await fetchAuthSession();
const token = session.tokens?.accessToken?.toString();
```

Cualquier forma de obtenerlo sirve, siempre que sea el **access token** (o el id token): ambos
traen `cognito:groups` y `sub`.

## 3. Crear el cliente

```ts
import { Client, IMessage } from "@stomp/stompjs";

const client = new Client({
  brokerURL: import.meta.env.VITE_WS_URL, // wss://api.tudominio.com/ws/catalog
  connectHeaders: { Authorization: `Bearer ${token}` },
  reconnectDelay: 3000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  debug: () => {},
});

client.onConnect = () => {
  client.subscribe("/user/queue/orders", (msg: IMessage) => {
    const { type, payload } = JSON.parse(msg.body);
    upsertOrder(payload);            // payload es el mismo OrderDTO de GET /order/{id}
    if (type === "ORDER_CREATED") toast("Tienes una orden nueva");
  });

  client.subscribe(`/topic/calendar/${providerId}`, () => {
    reloadCalendar();                // vuelve a pedir el mes que está en pantalla
  });
};

client.onWebSocketClose = () => console.warn("canal cerrado, reintentando…");
client.onStompError = frame => console.error(frame.headers["message"]);

client.activate();
```

Tres reglas que evitan casi todos los problemas:

1. **Suscríbete antes de pedir los datos.** Si pides primero y te suscribes después, un cambio que
   ocurra entre las dos cosas se pierde.
2. **Al reconectar, vuelve a pedir.** El canal no guarda historial. `onConnect` se ejecuta en cada
   reconexión, así que ahí van las suscripciones **y** el refetch.
3. **Desactiva al salir.** `await client.deactivate()` cuando el usuario cierra sesión o cambia de
   pantalla, si el cliente no es global.

## 4. Hook de React

```tsx
import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

export function useCatalogRealtime({
  token,
  providerId,
  onOrder,
  onCalendar,
}: {
  token: string | null;
  providerId?: string;
  onOrder: (order: unknown) => void;
  onCalendar: () => void;
}) {
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!token) return;

    const client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = () => {
      client.subscribe("/user/queue/orders", m => onOrder(JSON.parse(m.body).payload));
      if (providerId) {
        client.subscribe(`/topic/calendar/${providerId}`, () => onCalendar());
      }
    };

    client.activate();
    clientRef.current = client;

    return () => { void client.deactivate(); };
  }, [token, providerId]);

  return clientRef;
}
```

## 5. Mensajes

```json
// /user/queue/orders
{
  "type": "ORDER_UPDATED",
  "at": "2026-11-12T15:04:00",
  "payload": { "id": "...", "status": "DELIVERING", "startConfirmation": { "...": "..." } }
}

// /topic/calendar/prov-123
{
  "type": "CALENDAR_CHANGED",
  "at": "2026-11-12T15:04:00",
  "payload": { "providerId": "prov-123", "reason": "ORDER_UPDATED" }
}
```

`type` de órdenes: `ORDER_CREATED`, `ORDER_UPDATED`, `ORDER_CANCELLED`.
El `payload` es idéntico a lo que devuelve `GET /order/{id}`, así que el mismo modelo y el mismo
componente sirven para las dos cosas: reemplaza la orden en la lista por `payload.id`.

## 6. Variables de entorno del frontend

```env
VITE_WS_URL=wss://api.tudominio.com/ws/catalog
VITE_API_URL=https://api.tudominio.com
```

En local, con el servicio en `localhost:8080` y `SECURITY_ENABLED=false`:

```env
VITE_WS_URL=ws://localhost:8080/ws/catalog
```

## 7. Diagnóstico

| Síntoma | Causa probable |
|---|---|
| `401`/cierre inmediato al conectar | token vencido o ausente en `connectHeaders` |
| Conecta pero no llega nada a `/user/queue/orders` | el `sub` del token no es el mismo id que el de las órdenes |
| El calendario no se mueve | el frontend escucha otro `providerId` o no se suscribió antes de cargar |
| Se corta cada 60 s | idle timeout del ALB (`aws-alb-https.md`) |
| Con dos tareas de ECS unos ven cambios y otros no | broker simple sin relay: dejar una sola tarea |
