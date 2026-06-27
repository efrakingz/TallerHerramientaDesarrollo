# API Reference

La especificación completa está disponible en formato OpenAPI 3.1 en [`openapi.yaml`](openapi.yaml).

Con la aplicación corriendo, accede a **Swagger UI** en:
> 👉 [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Base URL

```
http://localhost:8080
```

---

## Endpoints

### 1. Crear Registro de Pesaje

**`POST /api/pesajes`**

Crea un nuevo registro en estado `INGRESADO`. El sistema convierte automáticamente el peso a Sansas y clasifica el paquete.

#### Request Body

```json
{
  "scaleId": "101",
  "packageId": "PKG-2024-001",
  "weightInKg": 20.055
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `scaleId` | string | ✅ | ID de la balanza |
| `packageId` | string | ✅ | ID del paquete |
| `weightInKg` | number | ✅ | Peso en kg (debe ser > 0) |

#### Respuestas

**`201 Created`** — Registro creado exitosamente

```json
{
  "id": "64f1a2b3c4d5e6f7a8b9c0d1",
  "scaleId": "101",
  "packageId": "PKG-2024-001",
  "weightInSansas": 15.0,
  "weightInKg": 20.055,
  "weightCategory": "MEDIANO",
  "currentState": "INGRESADO",
  "stateHistory": [
    {
      "fromState": null,
      "toState": "INGRESADO",
      "timestamp": "2024-11-15T10:30:00Z"
    }
  ],
  "createdAt": "2024-11-15T10:30:00Z",
  "updatedAt": "2024-11-15T10:30:00Z"
}
```

**`400 Bad Request`** — Validación fallida

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "weightInKg must be positive",
  "timestamp": "2024-11-15T10:30:00Z"
}
```

**`422 Unprocessable Entity`** — Violación de regla de negocio

```json
{
  "status": 422,
  "error": "Business Rule Violation",
  "message": "No se puede procesar paquetes PESADO en horario nocturno (20:00 - 06:00)",
  "timestamp": "2024-11-15T21:00:00Z"
}
```

---

### 2. Obtener Registros por Fecha

**`GET /api/pesajes?fecha={YYYY-MM-DD}`**

Retorna todos los registros de pesaje de la fecha indicada.

#### Query Parameters

| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `fecha` | date (ISO) | ✅ | Fecha de filtro, ej: `2024-11-15` |

#### Ejemplo de Request

```bash
curl -X GET "http://localhost:8080/api/pesajes?fecha=2024-11-15" \
     -H "Accept: application/json"
```

#### Respuesta `200 OK`

```json
[
  {
    "id": "64f1a2b3c4d5e6f7a8b9c0d1",
    "scaleId": "101",
    "packageId": "PKG-2024-001",
    "weightInSansas": 15.0,
    "weightInKg": 20.055,
    "weightCategory": "MEDIANO",
    "currentState": "PESADO",
    "stateHistory": [
      { "fromState": null, "toState": "INGRESADO", "timestamp": "2024-11-15T10:30:00Z" },
      { "fromState": "INGRESADO", "toState": "PESADO", "timestamp": "2024-11-15T10:35:00Z" }
    ],
    "createdAt": "2024-11-15T10:30:00Z",
    "updatedAt": "2024-11-15T10:35:00Z"
  }
]
```

---

### 3. Actualizar Estado de un Pesaje

**`PUT /api/pesajes/{id}/estado`**

Avanza el estado del registro según las transiciones válidas.

#### Path Parameters

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `id` | string | ID del registro (MongoDB ObjectId) |

#### Request Body

```json
{
  "newState": "PESADO"
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `newState` | WeighingState | ✅ | Nuevo estado del pesaje |

**Valores válidos de `newState`:** `INGRESADO`, `PESADO`, `APROBADO`, `RECHAZADO`, `DESPACHADO`

#### Ejemplo completo de flujo

```bash
# 1. Crear pesaje
curl -X POST http://localhost:8080/api/pesajes \
  -H "Content-Type: application/json" \
  -d '{"scaleId":"101","packageId":"PKG-001","weightInKg":20.055}'

# 2. Pasar a PESADO
curl -X PUT http://localhost:8080/api/pesajes/{id}/estado \
  -H "Content-Type: application/json" \
  -d '{"newState":"PESADO"}'

# 3. Aprobar
curl -X PUT http://localhost:8080/api/pesajes/{id}/estado \
  -H "Content-Type: application/json" \
  -d '{"newState":"APROBADO"}'

# 4. Despachar
curl -X PUT http://localhost:8080/api/pesajes/{id}/estado \
  -H "Content-Type: application/json" \
  -d '{"newState":"DESPACHADO"}'
```

#### Respuestas

**`200 OK`** — Estado actualizado

**`400 Bad Request`** — Transición inválida

```json
{
  "status": 400,
  "error": "Illegal Weighing State",
  "message": "Transición inválida: no se puede pasar de INGRESADO a APROBADO",
  "timestamp": "2024-11-15T10:30:00Z"
}
```

**`404 Not Found`** — Registro inexistente

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "RegistroPesaje con id '64f1a2b3c4d5e6f7a8b9c0d1' no encontrado",
  "timestamp": "2024-11-15T10:30:00Z"
}
```

---

## Modelos de Datos

### WeighingState (Enum)

```
INGRESADO → PESADO → APROBADO  → DESPACHADO
                  └→ RECHAZADO → DESPACHADO
```

### WeightCategory (Enum)

| Valor | Rango en Sansas |
|-------|----------------|
| `LIVIANO` | ≤ 10 S |
| `MEDIANO` | > 10 y ≤ 50 S |
| `PESADO` | > 50 S |
