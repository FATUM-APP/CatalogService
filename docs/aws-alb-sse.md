# Desplegar los dos servicios detrás de un ALB con HTTPS

Un solo ALB alcanza para los dos servicios. El balanceador decide a cuál manda cada petición por la
ruta: `/notifications*` va al servicio de notificaciones y todo lo demás al catálogo. Dos ALB
separados solo tendrían sentido si quisieras dominios distintos (`api.` y `notificaciones.`) o
límites de tráfico independientes; cuestan el doble y no dan nada más.

## 1. El certificado

El certificado tiene que estar en **us-east-1**, sin importar en qué región estén los servicios,
porque es la única región que el ALB acepta para certificados de ACM.

1. ACM → **Request a public certificate** → **Request a public certificate**.
2. Dominio: `api.tudominio.com` (y `*.tudominio.com` si vas a usar más subdominios).
3. Validación: **DNS validation**.
4. ACM te da un registro CNAME. **Create record in Route 53** lo crea solo si el dominio está ahí.
5. Espera a que el estado pase de *Pending validation* a *Issued* (de minutos a una hora).

El certificado no sirve hasta estar *Issued*. Si el dominio lo registraste en Route 53, el CNAME se
crea con un clic.

## 2. El listener HTTPS

En `fatum-alb` → **Listeners and rules** → **Add listener**:

| Campo | Valor |
|---|---|
| Protocol | HTTPS |
| Port | 443 |
| Default action | Forward to `catalog-tg` (el grupo del catálogo) |
| Security policy | ELBSecurityPolicy-TLS13-1-2-2021-06 |
| Default SSL/TLS certificate | El certificado del paso 1 |

Deja también el listener HTTP:80 redirigiendo a HTTPS:

1. Selecciona el listener HTTP:80 → **Manage listener** → **Edit listener**.
2. Cambia la acción por defecto a **Redirect to URL** con `HTTPS` y `#{port}`.

Así nadie entra por `http://`.

## 3. Los dos target groups

| | Catálogo | Notificaciones |
|---|---|---|
| Nombre | `catalog-tg` | `notification-tg` |
| Target type | IP | IP |
| Protocolo: puerto | HTTP : 8080 | HTTP : 8080 |
| VPC | `vpc-05c15afc7f08eb995` | la misma |
| Health check path | `/actuator/health` | `/actuator/health` |
| Healthy threshold | 2 | 2 |
| Interval | 30 s | 30 s |

Los dos servicios escuchan en 8080 dentro del contenedor (es lo que ya tiene tu tarea `catalog-service`
y lo que pone `PORT`). El target group del catálogo ya existe (`fatum-target-group`); renómbralo o
créalo de nuevo para no confundirlo con el de notificaciones.

## 4. Las reglas de la ruta

En el listener HTTPS, **Manage rules** → **Add rule**:

| Prioridad | Condición | Acción |
|---|---|---|
| 10 | Path `/notifications` y `/notifications/*` | Forward a `notification-tg` |
| 20 | Path `/actuator/health` | Forward a `catalog-tg` (opcional, el default ya lo cubre) |
| default | — | Forward a `catalog-tg` |

Con esto el front usa un solo host:

- `https://api.tudominio.com/catalog/...` → catálogo
- `https://api.tudominio.com/realtime/orders` → catálogo (SSE)
- `https://api.tudominio.com/notifications/...` → notificaciones
- `https://api.tudominio.com/notifications/stream` → notificaciones (SSE)

Si prefieres dos dominios, en vez de reglas por ruta usas reglas por host: `api.tudominio.com` al
catálogo y `notificaciones.tudominio.com` al otro, con dos registros CNAME en Route 53 apuntando al
mismo ALB.

## 5. El DNS

Route 53 → hosted zone del dominio → **Create record**:

| Campo | Valor |
|---|---|
| Record name | `api` |
| Record type | A |
| Alias | sí |
| Route traffic to | Alias to Application and Classic Load Balancer → us-east-1 → `fatum-alb` |

Es un alias, no un CNAME: apunta al ALB sin costo por consulta y sigue los cambios de IP del
balanceador.

## 6. Los dos servicios en ECS

En el cluster `fatum-cluster`, un servicio por microservicio:

| | `catalog-service` | `notification-service` |
|---|---|---|
| Task definition | la tuya, puerto 8080 | una nueva, puerto 8080 |
| Target group | `catalog-tg` | `notification-tg` |
| Desired tasks | **1** | **1** |
| Health check grace period | 120 s | 120 s |
| Load balancer | `fatum-alb`, listener 443 | el mismo |

La regla de las tareas: **una sola** por servicio mientras el tiempo real viva en memoria (ver
`realtime-sse.md`). El auto scaling se queda apagado; si algún día lo necesitas, primero hay que
mover las conexiones a Redis.

## 7. Variables de entorno

Catálogo:

| Variable | Ejemplo |
|---|---|
| `AWS_REGION` | `us-east-1` |
| `EVENTBRIDGE_BUS_ARN` | `arn:aws:events:us-east-1:902353451847:event-bus/fatum` |
| `COGNITO_ISSUER_URI` | `https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XXXX` |
| `CORS_ALLOWED_ORIGINS` | `https://app.tudominio.com` |
| `SECURITY_ENABLED` | `true` |
| `BASE_URL` | `https://api.tudominio.com` |
| `FILE_SERVICE_URL` | la URL del servicio de archivos |

Notificaciones:

| Variable | Ejemplo |
|---|---|
| `AWS_REGION` | `us-east-1` |
| `NOTIFICATION_QUEUE_NAME` | `fatum-notification-queue` |
| `NOTIFICATION_TABLE_NAME` | `fatum-notifications` |
| `COGNITO_ISSUER_URI` | el mismo del catálogo |
| `CORS_ALLOWED_ORIGINS` | `https://app.tudominio.com` |
| `SECURITY_ENABLED` | `true` |

Los dos necesitan permisos de IAM en la task role: el catálogo `events:PutEvents`, notificaciones
`sqs:ReceiveMessage`/`DeleteMessage` y `dynamodb:PutItem`/`Query`/`UpdateItem`.

## 8. Ajustes del ALB para SSE

- **Idle timeout**: por defecto son 60 segundos y el canal manda un latido cada 20, así que no hace
  falta tocarlo. Si algún día subes el latido por encima de 60, sube también el idle timeout
  (Load balancer → Attributes → Idle timeout, máximo 4000 s).
- **Sticky sessions**: no hacen falta. Cada conexión SSE se queda en la tarea que la abrió.
- **Compresión**: desactívala para `text/event-stream` si la activas en el listener; comprimir un
  stream lo retrasa.
- El ALB no acumula el stream: lo pasa tal cual en cuanto llega, así que los eventos se ven al
  instante.

## 9. Security groups

- `fatum-alb-sg` (ya lo tienes): 443 y 80 abiertos a `0.0.0.0/0`.
- El security group de las tareas: 8080 solo desde `fatum-alb-sg`. Nadie llega al contenedor sin
  pasar por el balanceador.

## 10. Comprobarlo

```bash
curl -I https://api.tudominio.com/actuator/health          # 200 del catálogo
curl -I https://api.tudominio.com/notifications/unread-count  # 401 sin token: hay seguridad
curl -N "https://api.tudominio.com/realtime/orders?access_token=$TOKEN"   # stream abierto
```

El `curl -N` se queda colgado mostrando `: ping` cada 20 segundos y los `event: order` cuando
alguien mueve una orden. Con eso está todo conectado.

## Costos

Un ALB cuesta unos 0.0225 USD por hora más lo que consuma en tráfico (LCU). Dos ALB duplican la
parte fija sin darte nada, salvo dominios o límites separados. El certificado de ACM es gratis
mientras viva en el ALB.
