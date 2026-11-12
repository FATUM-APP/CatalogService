# Despliegue en ECS

Cluster `fatum-cluster`, servicio `catalog-service`, contenedor `Main` en el puerto 8080, detrás de
`fatum-alb` con el target group `fatum-target-group` (tipo IP).

## 0. Antes de la primera tarea

El servicio está con **desired tasks = 0**. Dos cosas antes de subirlo a 1:

1. **Health check del target group** en `/actuator/health` (ver `aws-alb-https.md`).
2. **Security group de las tareas**: `fatum-alb-sg` no debe abrir el 8080 a internet. Crea
   `catalog-service-sg` con:

| Dirección | Puerto | Origen |
|---|---|---|
| Inbound | 8080 | `sg-0d23755dceb2401a1` (el del ALB) |
| Outbound | all | 0.0.0.0/0 |

El ALB y las tareas van en subredes distintas: el tráfico del ALB sale por su security group, no
por la IP del cliente.

## 1. Imagen

El `Dockerfile` ya compila el jar y lo corre en el puerto 8080.

```bash
aws ecr create-repository --repository-name catalog-service --region us-east-1

aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin 902353451847.dkr.ecr.us-east-1.amazonaws.com

mvn -B clean package -DskipTests
docker build -t catalog-service .
docker tag catalog-service:latest 902353451847.dkr.ecr.us-east-1.amazonaws.com/catalog-service:latest
docker push 902353451847.dkr.ecr.us-east-1.amazonaws.com/catalog-service:latest
```

En CI conviene etiquetar con el commit (`:$(git rev-parse --short HEAD)`) para poder volver atrás.

## 2. Roles

| Rol | Para qué |
|---|---|
| **Task execution role** (`ecsTaskExecutionRole`) | bajar la imagen de ECR, escribir en CloudWatch, leer secretos |
| **Task role** (`catalog-service-task-role`) | permisos que usa la app: SQS (`sqs:SendMessage`/`ReceiveMessage` en `fatum-catalog-queue`) |

Si la app firma llamadas a AWS (SQS con el starter de Spring Cloud AWS), usa la task role, no una
API key.

## 3. Task definition

Variables que lee `application.yaml`. Las marcadas como secreto no deben ir en texto plano.

| Variable | Ejemplo | ¿Secreto? |
|---|---|---|
| `PORT` | `8080` | no |
| `DB_URL` | `jdbc:postgresql://<rds>:5432/catalog` | no |
| `DB_USERNAME` | `catalog` | no |
| `DB_PASSWORD` | — | **sí** |
| `JPA_DDL_AUTO` | `validate` | no |
| `JPA_SHOW_SQL` | `false` | no |
| `FLYWAY_ENABLED` | `true` | no |
| `AWS_REGION` | `us-east-1` | no |
| `TOKEN_URL` | `https://us-east-19qukvptss.auth.us-east-1.amazoncognito.com/oauth2/token` | no |
| `COGNITO_M2M_CLIENT_ID` | — | no |
| `COGNITO_M2M_CLIENT_SECRET` | — | **sí** |
| `COGNITO_ISSUER_URI` | `https://cognito-idp.us-east-1.amazonaws.com/<userPoolId>` | no |
| `SECURITY_ENABLED` | `true` | no |
| `ALLOWED_ORIGINS` | `https://app.tudominio.com` | no |
| `BASE_URL` | API Gateway de usuarios | no |
| `QUEUE_NAME` | `fatum-catalog-queue` | no |
| `FILE_SERVICE_URL` | `http://<file-service>:8081` | no |
| `FILE_SERVICE_SECRET` | — | **sí** |

`COGNITO_ISSUER_URI` sale de Cognito → **User pools** → tu pool → **User pool ID**. El issuer es
`https://cognito-idp.<region>.amazonaws.com/<ese id>`.

Ejemplo de task definition (recorta cpu/memory a lo que ya uses):

```json
{
  "family": "catalog-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::902353451847:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::902353451847:role/catalog-service-task-role",
  "containerDefinitions": [
    {
      "name": "Main",
      "image": "902353451847.dkr.ecr.us-east-1.amazonaws.com/catalog-service:latest",
      "essential": true,
      "portMappings": [{ "containerPort": 8080, "protocol": "tcp" }],
      "environment": [
        { "name": "PORT", "value": "8080" },
        { "name": "DB_URL", "value": "jdbc:postgresql://<rds>:5432/catalog" },
        { "name": "DB_USERNAME", "value": "catalog" },
        { "name": "JPA_DDL_AUTO", "value": "validate" },
        { "name": "FLYWAY_ENABLED", "value": "true" },
        { "name": "AWS_REGION", "value": "us-east-1" },
        { "name": "SECURITY_ENABLED", "value": "true" },
        { "name": "COGNITO_ISSUER_URI", "value": "https://cognito-idp.us-east-1.amazonaws.com/<poolId>" },
        { "name": "ALLOWED_ORIGINS", "value": "https://app.tudominio.com" },
        { "name": "BASE_URL", "value": "https://<api-gateway>" },
        { "name": "QUEUE_NAME", "value": "fatum-catalog-queue" },
        { "name": "FILE_SERVICE_URL", "value": "http://<file-service>:8081" }
      ],
      "secrets": [
        { "name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:us-east-1:902353451847:secret:catalog/db-password" },
        { "name": "COGNITO_M2M_CLIENT_SECRET", "valueFrom": "arn:aws:secretsmanager:us-east-1:902353451847:secret:catalog/m2m-client-secret" },
        { "name": "FILE_SERVICE_SECRET", "valueFrom": "arn:aws:secretsmanager:us-east-1:902353451847:secret:catalog/file-service-secret" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/catalog-service",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "catalog"
        }
      }
    }
  ]
}
```

## 4. Servicio

En el servicio `catalog-service` → **Update**:

| Ajuste | Valor | Por qué |
|---|---|---|
| Desired tasks | **1** | el broker del WebSocket vive en la tarea (ver abajo) |
| Health check grace period | 180 s | Flyway corre migraciones al arrancar |
| Deployment circuit breaker | **activado** con rollback | si la nueva tarea no queda sana, vuelve la anterior |
| Minimum healthy percent | 100 | sin ventana sin servicio |
| Subnets | `subnet-0c940a57981c79df5`, `subnet-000e9a9a83c5d8848` | las mismas del ALB |
| Security group | `catalog-service-sg` | la del paso 0 |
| Load balancer | `fatum-alb`, listener HTTPS:443, `fatum-target-group`, `Main:8080` | al crear el listener HTTPS |

Al guardar, ECS registra la IP de la tarea en `fatum-target-group` y el ALB empieza a mandarle
tráfico cuando `/actuator/health` responde 200.

## 5. Una sola tarea mientras el broker sea simple

El canal en tiempo real usa el broker en memoria de Spring. Si hay **dos tareas**, un usuario
conectado a la tarea A no recibe los eventos que genera la tarea B: vería el calendario congelado
hasta refrescar.

Mientras eso no se resuelva con un relay (RabbitMQ), deja **desired tasks = 1**. Si más adelante
necesitas escalar, el cambio es agregar el relay y nada más: los destinos y los eventos no cambian.

## 6. Verificar el despliegue

```bash
# la tarea está sana y registrada
aws elbv2 describe-target-health --target-group-arn <arn de fatum-target-group>

# el servicio responde
curl -i https://api.tudominio.com/actuator/health

# los endpoints protegidos piden token
curl -i https://api.tudominio.com/order/client            # 401
curl -i -H "Authorization: Bearer $TOKEN" https://api.tudominio.com/order/client

# métricas: el mismo token con rol VERIFIED debe dar 403
curl -i -H "Authorization: Bearer $TOKEN_VERIFIED" https://api.tudominio.com/metrics/service/x/live
```

Los logs salen en CloudWatch → `/ecs/catalog-service`.

## 7. Volver atrás

`Update service` → task definition anterior → **Update**. Con el circuit breaker activado, un
despliegue que no pasa el health check se revierte solo.
