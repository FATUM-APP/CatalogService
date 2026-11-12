# HTTPS en `fatum-alb`

Estado actual (según la consola):

| Recurso | Valor |
|---|---|
| ALB | `fatum-alb` (internet-facing, IPv4, us-east-1) |
| VPC | `vpc-05c15afc7f08eb995` |
| Subredes | `subnet-0c940a57981c79df5` (us-east-1b), `subnet-000e9a9a83c5d8848` (us-east-1a) |
| Security group | `sg-0d23755dceb2401a1` (`fatum-alb-sg`) con 80 y 443 abiertos |
| Listener | solo HTTP:80 → `fatum-target-group` |
| Target group | `fatum-target-group`, tipo IP, HTTP:8080 |
| ECS | cluster `fatum-cluster`, servicio `catalog-service`, contenedor `Main:8080` |

El puerto 443 ya está abierto en el security group, así que solo falta el certificado y el listener.

## Requisito: un dominio

ACM solo emite certificados para dominios que controlas. No sirve para
`fatum-alb-377854245.us-east-1.elb.amazonaws.com`.

- Si el dominio está en Route 53: la validación se crea con un clic.
- Si está en otro registrador: se agrega un CNAME a mano y se espera.

Si todavía no tienes dominio, registra uno (por ejemplo `fatum.app`) o usa un subdominio de uno
que ya tengas. La guía asume `api.tudominio.com`; cámbialo por el tuyo.

## 1. Pedir el certificado

Consola → **Certificate Manager** (región **us-east-1**, la del ALB; si el certificado se pide en
otra región no aparece en el ALB).

1. **Request a certificate** → **Request a public certificate**.
2. **Domain names**: `api.tudominio.com`. Si también quieres `www`, agrégalo como segundo nombre.
3. **Validation method**: **DNS validation**.
4. **Key algorithm**: RSA 2048.
5. **Request**.

En la lista, el certificado aparece **Pending validation**.

## 2. Validar

- **Dominio en Route 53**: abre el certificado → **Create records in Route 53** → **Create records**.
  Route 53 crea el CNAME `_xxxx.api.tudominio.com`.
- **Dominio en otro registrador**: copia el nombre y el valor del CNAME que muestra ACM y agrégalo
  en el panel de tu proveedor.

La validación tarda de minutos a una hora. Cuando el estado sea **Issued**, sigue.

## 3. Crear el listener HTTPS

EC2 → **Load balancers** → `fatum-alb` → pestaña **Listeners and rules** → **Add listener**.

| Campo | Valor |
|---|---|
| Protocol:Port | **HTTPS:443** |
| Default action | **Forward to** `fatum-target-group` |
| Security policy | `ELBSecurityPolicy-TLS13-1-2-2021-06` (acepta TLS 1.3 y 1.2) |
| Default SSL/TLS certificate | **From ACM** → el certificado recién emitido |

**Add**.

## 4. Redirigir HTTP a HTTPS

En el listener **HTTP:80** → **Manage listener** → **Edit listener**:

- Default action: **Redirect to URL**.
- Protocol **HTTPS**, port **443**, status **301 (Permanently moved)**.

Guardar. A partir de aquí el 80 ya no sirve contenido, solo redirige.

## 5. Apuntar el dominio al ALB

**Route 53** → tu hosted zone → **Create record**:

| Campo | Valor |
|---|---|
| Record name | `api` |
| Record type | **A** |
| Alias | **sí** |
| Route traffic to | **Alias to Application and Classic Load Balancer** → us-east-1 → `fatum-alb` |

Si el dominio no está en Route 53, crea en tu proveedor un CNAME `api` →
`fatum-alb-377854245.us-east-1.elb.amazonaws.com` (con CNAME no puedes usar el dominio raíz, pero
`api.tudominio.com` sí funciona).

## 6. Verificar

```bash
# el certificado es válido y responde
curl -I https://api.tudominio.com/actuator/health

# la cadena completa
openssl s_client -connect api.tudominio.com:443 -servername api.tudominio.com </dev/null 2>/dev/null \
  | openssl x509 -noout -subject -issuer -dates

# el 80 redirige
curl -I http://api.tudominio.com/actuator/health     # 301 + Location: https://...
```

Si `curl` falla con `SSL: no alternative certificate subject name matches`, el registro DNS
todavía apunta al ALB por su nombre o el certificado no cubre ese dominio.

## 7. Lo que el ALB necesita saber para el WebSocket

El ALB soporta WebSocket de forma nativa, sin configuración extra. Dos ajustes sí importan:

| Ajuste | Dónde | Valor |
|---|---|---|
| **Idle timeout** | `fatum-alb` → **Attributes** → Edit | 300 s (el default son 60 s) |
| **Health check** | `fatum-target-group` → **Health checks** → Edit | Path `/actuator/health`, matcher 200, interval 30 s, timeout 5 s |

El idle timeout importa porque una conexión WebSocket sin tráfico se corta a los 60 s. El broker
manda heartbeats cada 10 s, así que en la práctica no se cae, pero 300 s deja margen para clientes
que tarden en suscribirse.

Las **sticky sessions** no hacen falta: la conexión WebSocket ya queda pegada a una tarea mientras
vive.

## 8. Comprobar el handshake

Con el servicio levantado y el listener listo:

```bash
curl -i -N \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  https://api.tudominio.com/ws/catalog
```

Debe responder `HTTP/1.1 101 Switching Protocols`. Un `400` con `Can "Upgrade" only to "websocket"`
significa que la petición no llevaba las cabeceras correctas; un `502` significa que no hay ninguna
tarea sana detrás del target group.

## Errores típicos

| Síntoma | Causa |
|---|---|
| El certificado no aparece en el ALB | se pidió en otra región; debe ser us-east-1 |
| `Pending validation` eterno | el CNAME no está publicado o el dominio no está delegado a esa zona |
| `502 Bad Gateway` | no hay tareas sanas en `fatum-target-group` (revisa el health check) |
| `504 Gateway Timeout` | el health check apunta a `/` y la tarea responde 401; usa `/actuator/health` |
| El WebSocket se corta a los 60 s | idle timeout del ALB |
