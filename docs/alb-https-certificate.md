# HTTPS en el ALB para CatalogService (REST + WebSocket)

Objetivo: que el servicio quede en `https://api.fatum.example/...` y `wss://api.fatum.example/ws/catalog`
con un certificado válido. Un ALB sirve las dos cosas con el **mismo** certificado y el **mismo**
listener 443: WebSocket es un upgrade sobre HTTPS, no un protocolo aparte.

Hay dos caminos. Empieza por el A; solo si no puedes (certificado de terceros, otro proveedor de DNS)
usa el B.

---

## A. Certificado gratuito de AWS Certificate Manager (recomendado)

ACM emite y **renueva automáticamente** el certificado. No tienes que tocar nada cada 90 días.

### A.1 Requisitos previos

- Un dominio que controles (ej. `fatum.example`).
- La zona en **Route 53** (o poder crear un registro CNAME en tu proveedor de DNS).
- El ALB en la **misma región** que el certificado (ACM es regional para ALB; un certificado de
  `us-east-1` **no** se puede adjuntar a un ALB de `us-west-2`).

### A.2 Solicitar el certificado (consola)

1. ACM → **Request a certificate** → **Request a public certificate**.
2. Domain names:
   - `api.fatum.example` (el nombre exacto del API), o
   - `*.fatum.example` (comodín: cubre `api`, `ws`, `staging`… sin volver a pedir).
     El comodín **no** cubre `fatum.example` sin subdominio; si lo necesitas, agrégalo como
     segundo nombre en el mismo certificado.
3. Validation method: **DNS validation** (es la única que se puede automatizar y renovar sola).
4. Key algorithm: RSA 2048 (o ECDSA P-256 si todos tus clientes son modernos).
5. Create → ACM te muestra un **CNAME** `_xxxxx.api.fatum.example` con un valor.

Si la zona está en Route 53 y usas la consola, ACM ofrece **Create records in Route 53** y lo hace
solo. Si no:

```
CNAME  _xxxxx.api.fatum.example  ->  _yyyyy.acm-validations.aws.
```

La validación tarda entre 1 y 30 minutos. El certificado queda **Issued** cuando el registro
propaga.

### A.3 Adjuntarlo al ALB

1. EC2 → Load Balancers → tu ALB → **Listeners and rules** → **Add listener**.
2. **HTTPS : 443**, Default action = tu target group (el que tiene el servicio en el puerto 8080).
3. **Default SSL/TLS certificate** → *From ACM* → el certificado recién emitido.
4. Security policy: `ELBSecurityPolicy-TLS13-1-2-2021-06` (TLS 1.2+). Evita políticas que acepten
   TLS 1.0/1.1.
5. Save.

### A.4 Redirigir HTTP → HTTPS (opcional pero recomendado)

Listener **HTTP : 80** → Default action **Redirect** → HTTPS, puerto **443**, status **301**.

### A.5 Security group

El security group del ALB debe permitir **443** (y 80 si hiciste el redirect):

```bash
aws ec2 authorize-security-group-ingress --group-id sg-xxxx \
  --protocol tcp --port 443 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-id sg-xxxx \
  --protocol tcp --port 80 --cidr 0.0.0.0/0
```

El security group de las tareas/instancias **no** cambia: sigue aceptando tráfico del SG del ALB en
el puerto 8080.

### A.6 DNS

Route 53 → tu zona → **Create record**:

- Record name: `api`
- Type: **A**
- Alias: **Yes** → Alias to Application and Classic Load Balancer → región → tu ALB.

(Con alias, AWS resuelve directo al ALB sin costo de consultas y sin IP fija.)

### A.7 Verificar

```bash
# cadena completa + emisor
openssl s_client -connect api.fatum.example:443 -servername api.fatum.example </dev/null 2>/dev/null \
  | openssl x509 -noout -subject -issuer -dates

# REST
curl -vI https://api.fatum.example/actuator/health

# WebSocket (debe responder 101 Switching Protocols con el handshake STOMP)
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
     -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: $(openssl rand -base64 16)" \
     https://api.fatum.example/ws/catalog
```

Si `curl` devuelve `101`, el certificado, el listener y el target group están bien.

---

## B. Certificado propio o de terceros (importado en ACM)

Úsalo si el certificado lo emite tu empresa, otro proveedor, o si necesitas Let's Encrypt porque
no quieres usar ACM. **Ojo:** ACM **no renueva** los certificados importados; la renovación es tuya
(hoy puedes configurar renovación gestionada en ACM para importados con DNS validation, pero no
asumas que está activa).

### B.1 Generar una clave y un CSR

```bash
openssl req -new -newkey rsa:2048 -nodes \
  -keyout api.fatum.example.key \
  -out api.fatum.example.csr \
  -subj "/C=CO/ST=Cundinamarca/L=Bogota/O=Fatum/CN=api.fatum.example"
```

Envía el `.csr` a tu CA (o a Let's Encrypt, ver B.3) y guarda la clave privada: **nunca** se sube a
ACM, solo se usa para generar el certificado y luego se guarda en un secreto.

### B.2 Importar en ACM

```bash
aws acm import-certificate \
  --certificate fileb://api.fatum.example.crt \
  --certificate-chain fileb://chain.pem \
  --private-key fileb://api.fatum.example.key \
  --region us-east-1
```

Devuelve un `CertificateArn`; ese ARN es el que adjuntas al listener 443 (paso A.3).

### B.3 Let's Encrypt con validación DNS (sin abrir el puerto 80)

```bash
# una sola vez
sudo snap install certbot
sudo certbot certonly --manual --preferred-challenges dns \
  -d api.fatum.example
# certbot pide crear un TXT _acme-challenge.api.fatum.example y espera la propagación

# archivos resultantes
# /etc/letsencrypt/live/api.fatum.example/fullchain.pem
# /etc/letsencrypt/live/api.fatum.example/privkey.pem
```

Luego importas `fullchain.pem` + `privkey.pem` en ACM (B.2). Renovación cada 90 días: lo más simple
es un Lambda programado (EventBridge `rate(30 days)`) que corre `certbot renew` + `aws acm
import-certificate`; o `certbot` con el plugin `dns-route53` en una instancia pequeña.

---

## C. Errores típicos

| Síntoma | Causa habitual |
|---|---|
| `SSL_ERROR_NO_CYPHER_OVERLAP` / handshake falla | Security policy demasiado vieja o cliente muy viejo |
| `certificate is not valid for this name` | El dominio del certificado no coincide con el `Host`/SNI (típico con comodín `*.fatum.example` y `fatum.example` a secas) |
| El certificado nunca pasa a *Issued* | El CNAME de validación no se creó en la zona correcta, o hay un CNAME previo en el mismo nombre |
| El ALB no acepta el certificado | Está en otra región, o es de CloudFront (`us-east-1`) y el ALB está en otra |
| HTTPS funciona pero el WebSocket se corta a los 60 s | Idle timeout del ALB. Los heartbeats de 25 s lo cubren; si igual pasa, súbelo a 300 s en el target group |
| `502` en `/ws` | Target group en HTTP/2 hacia el backend, o health check apuntando a otra ruta; el backend debe hablar HTTP/1.1 en 8080 |
| Redirección infinita 80 → 443 | El listener 80 redirige a HTTPS y el 443 apunta por error al listener 80 |

---

## D. Equivalentes en GCP (si mueves el servicio)

El mismo diseño funciona sin cambios en el código; solo cambia quién termina TLS:

- **Google-managed certificate** + `gcloud compute ssl-certificates create` + un `target-https-proxy`
  delante de un backend (MIG o NEG de Cloud Run).
- **Cloud Run**: soporta WebSocket nativamente; el certificado lo emite Google para el dominio
  `*.run.app`, y para dominio propio se mapea con `gcloud run domain-mappings create`. Ojo con el
  `--timeout` (por defecto 300 s) y con `--session-affinity` si usas broker simple.
- En GKE: `cert-manager` + Let's Encrypt (equivalente a B.3, pero automatizado con CRDs).

Lo único que **no** es portable hoy es la integración con AWS (SQS `@SqsListener` y el starter de
S3). El canal en tiempo real no depende de AWS en absoluto (ver la sección de portabilidad en
`realtime-stomp.md`).
