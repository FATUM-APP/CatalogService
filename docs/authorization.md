# Autorización

Los permisos salen de los grupos de Cognito. El claim `cognito:groups` se convierte en roles de
Spring (`PROFESSIONAL` → `ROLE_PROFESSIONAL`) en `configuration/CognitoAuthorities.kt`, y las reglas
viven en `configuration/SecurityConfig.kt`.

## Matriz

| Endpoint | Método | PROFESSIONAL | VERIFIED |
|---|---|---|---|
| `/metrics/**` (todo el controller) | GET, POST | sí | no |
| `/catalog/item/create` | POST | sí | no |
| `/catalog/item/update`, `/deactivate`, `/activate` | PUT | sí | no |
| `/catalog/item/**` (lecturas) | GET | sí | sí |
| `/provider/availability/**` (cambiar, borrar) | PUT, DELETE | sí | no |
| `/provider/availability`, `/provider/calendar` (lecturas) | GET | sí | sí |
| `/provider/unavailability/**` (crear, cambiar, borrar) | POST, PUT, DELETE | sí | no |
| `/provider/unavailability/**` (lecturas) | GET | sí | sí |
| `/catalog/category/**` (crear, logo) | POST, PUT | sí | no |
| `/catalog/category/**` (lecturas) | GET | sí | sí |
| `/catalog/favorites/**` | GET, POST, DELETE | sí | sí |
| `/order/book`, `/order/respond`, `/order/cancel/**`, `/order/confirm/**`, `/order/deny/**` | POST, PUT | sí | sí |
| `/order/**` (lecturas) | GET | sí | sí |
| `/ws/catalog` (handshake) | — | público: el token se exige en el `CONNECT` |
| `/actuator/health` | GET | público |

Un PROFESSIONAL también pasa las reglas de VERIFIED, así que no necesita pertenecer a los dos
grupos.

Las categorías no estaban en la lista original; quedaron como PROFESSIONAL porque crearlas o
cambiarles el logo es una acción de administración. Si no es así, se cambia en una línea de
`SecurityConfig`.

## Configuración

```yaml
app:
  security:
    enabled: ${SECURITY_ENABLED:true}
    issuer-uri: ${COGNITO_ISSUER_URI:https://cognito-idp.us-east-1.amazonaws.com/REPLACE_ME}
```

`COGNITO_ISSUER_URI` es `https://cognito-idp.<region>.amazonaws.com/<userPoolId>`. Las llaves se
leen de `{issuer}/.well-known/jwks.json`.

Para desarrollo local sin Cognito: `SECURITY_ENABLED=false`. Con eso la cadena queda abierta y el
WebSocket vuelve a aceptar la cabecera `X-User-Id`.

## Token

Cualquier endpoint protegido necesita `Authorization: Bearer <access_token>` (o id token, ambos
traen `cognito:groups` y `sub`). El `sub` es el id que el servicio usa como `clientId` o
`providerId`.

En el WebSocket, el token va en el `CONNECT`:

```js
const client = new Client({
  brokerURL: "wss://api.tu-dominio/ws/catalog",
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  reconnectDelay: 3000,
  onConnect: () => {
    client.subscribe("/user/queue/orders", m => upsertOrder(JSON.parse(m.body).payload));
    client.subscribe(`/topic/calendar/${providerId}`, () => reloadCalendar());
  }
});
```

Si el token falta o no valida, el servidor cierra la conexión y no se entrega ningún mensaje.

## Lo que todavía no valida

- **Dueño del recurso**: un PROFESSIONAL puede pedir `/metrics/provider/{otroId}` o cambiar la
  disponibilidad de otro proveedor. Las reglas son por rol, no por pertenencia. Si lo quieres,
  se agrega comparando el `sub` del token con el id del path.
- **Cancelar la orden de otro**: `/order/cancel/{orderId}` no verifica que quien llama sea el
  cliente de esa orden.
