# Configuración del Entorno

## Prerrequisitos

| Herramienta | Versión mínima | Verificar |
|-------------|---------------|-----------|
| Java (JDK) | 17 | `java -version` |
| Maven | 3.9+ | `./mvnw -version` |
| Docker | 20.10+ | `docker --version` |
| Docker Compose | 2.x | `docker compose version` |
| Node.js | 16+ (para Docsify) | `node -v` |

---

## 1. Infraestructura con Docker Compose

El archivo `docker-compose.yml` en la raíz del proyecto levanta **MongoDB** y **Redis**:

```bash
# Levantar servicios en segundo plano
docker-compose up -d

# Verificar que están corriendo
docker-compose ps

# Ver logs
docker-compose logs -f
```

### Servicios

| Servicio | Puerto | Descripción |
|---------|--------|-------------|
| MongoDB | `27017` | Base de datos principal |
| Redis | `6379` | Caché de especificaciones de balanzas |

---

## 2. Configuración de la Aplicación

El archivo principal de configuración es `src/main/resources/application.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/sansaweigh
    redis:
      host: localhost
      port: 6379

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

---

## 3. Variables de Entorno (Opcional)

Puedes sobreescribir la configuración con variables de entorno:

| Variable | Valor por Defecto | Descripción |
|---------|------------------|-------------|
| `MONGODB_URI` | `mongodb://localhost:27017/sansaweigh` | URI de conexión MongoDB |
| `REDIS_HOST` | `localhost` | Host de Redis |
| `REDIS_PORT` | `6379` | Puerto de Redis |
| `SERVER_PORT` | `8080` | Puerto del servidor |

```bash
# Ejemplo con variables de entorno
MONGODB_URI=mongodb://prod-server:27017/sansaweigh ./mvnw spring-boot:run
```

---

## 4. Ejecutar la Aplicación

```bash
# Modo desarrollo (con Maven Wrapper)
./mvnw spring-boot:run

# En Windows
mvnw.cmd spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`

---

## 5. Verificar el Estado

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI (en el navegador)
open http://localhost:8080/swagger-ui.html
```

---

## 6. Levantar la Documentación Docsify

```bash
# Instalar Docsify CLI globalmente (una vez)
npm install -g docsify-cli

# Servir la documentación
docsify serve docs

# O con npx (sin instalación)
npx docsify-cli serve docs
```

La documentación estará disponible en `http://localhost:3000`

---

## 7. Detener los Servicios

```bash
# Detener la aplicación
Ctrl + C

# Detener Docker Compose
docker-compose down

# Detener y eliminar volúmenes (limpia datos)
docker-compose down -v
```
