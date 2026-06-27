# Reglas de Negocio

## Unidad de Medida Propietaria — Sansa (S)

El sistema **no trabaja directamente en kilogramos**. La conversión es obligatoria:

> **1 Sansa (S) = 1.337 kg**

```
weightInSansas = weightInKg / 1.337
```

**Ejemplo:** Un paquete de 20.055 kg equivale a **15.0 Sansas**

---

## A. Clasificación de Paquetes por Peso

Los paquetes se clasifican según su peso en Sansas:

| Categoría | Condición |
|-----------|-----------|
| `LIVIANO` | peso ≤ 10 Sansas |
| `MEDIANO` | peso > 10 y ≤ 50 Sansas |
| `PESADO`  | peso > 50 Sansas |

La categoría se calcula y persiste en el momento de creación del registro.

---

## B. Restricciones para Paquetes PESADO

### B.1 — Restricción Horaria Nocturna

> **No se permite pesar ni procesar paquetes `PESADO` en horario nocturno.**

- Horario restringido: **entre las 20:00 y las 06:00** (hora del servidor)
- Si se intenta crear un pesaje `PESADO` en este horario, el sistema lanza una excepción de negocio con **código 422**

```
20:00 ──────────────────────────────── 06:00
        ← Zona prohibida para PESADO →
```

### B.2 — Regla de Balanza Prima

> Si el ID de la balanza es un **número primo** Y el día del mes es **impar**, no se pueden registrar paquetes `PESADO`.

**Ejemplos:**
- Balanza ID `7` (primo) + día `3` (impar) → ❌ Bloqueado
- Balanza ID `7` (primo) + día `4` (par)  → ✅ Permitido
- Balanza ID `4` (no primo) + día `3` (impar) → ✅ Permitido

Si se viola esta regla, el sistema lanza una excepción de negocio con **código 422**.

---

## C. Ciclo de Vida y Máquina de Estados

Cada registro de pesaje posee una máquina de estados con las siguientes transiciones válidas:

```
INGRESADO ──► PESADO ──► APROBADO  ──► DESPACHADO
                    └──► RECHAZADO ──► DESPACHADO
```

### Transiciones Permitidas

| Estado Origen | Estado Destino | Válido |
|--------------|---------------|--------|
| `INGRESADO`  | `PESADO`      | ✅ |
| `PESADO`     | `APROBADO`    | ✅ |
| `PESADO`     | `RECHAZADO`   | ✅ |
| `APROBADO`   | `DESPACHADO`  | ✅ |
| `RECHAZADO`  | `DESPACHADO`  | ✅ |
| Cualquier otra combinación | — | ❌ |

### Excepción de Transición Inválida

Cualquier intento de transición no permitida lanza:
- Excepción: `IllegalWeighingStateException`
- Código HTTP: **400 Bad Request**

---

## D. Integración con API Externa de Balanzas

El servicio consulta especificaciones técnicas de cada balanza a una **API externa** usando `ExternalScaleClient.getScaleSpecifications(String scaleId)`.

### Estructura de la Respuesta Externa

```json
{
  "id": "101",
  "name": "Balanza Central Sur",
  "brand": "SansaScale-Pro",
  "maxCapacity": 150.0,
  "precision": 0.01,
  "lastCalibrationOffset": -0.05
}
```

### Estrategia de Reintentos

El cliente implementa **reintentos exponenciales** ante errores transitorios:
- Máximo **3 reintentos**
- Espera exponencial entre intentos

### Política de Fallback (Redis)

Si la API externa no está disponible tras los reintentos:

```
1. Buscar en caché Redis (TTL: 120 segundos)
      │
      ├── [Hit] Retornar especificación cacheada ✅
      │
      └── [Miss] Retornar especificación por defecto (id = "-1") ⚠️
```

### Caché Redis

- Las especificaciones recuperadas exitosamente se guardan en Redis
- **TTL:** 120 segundos
- Evita consultas redundantes al cliente de integración
