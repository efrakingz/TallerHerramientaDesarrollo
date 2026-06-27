# Suite de Pruebas

## Estructura de Tests

```
src/test/java/com/sansaweigh/
├── controller/
│   ├── PesajeControllerTest.java          # Tests unitarios del controller
│   └── PesajeControllerIntegrationTest.java  # Tests de integración REST
├── exception/
│   └── GlobalExceptionHandlerTest.java    # Tests del handler de errores
├── integration/
│   └── ExternalScaleClientTest.java       # Tests del cliente externo + retry
└── service/
    ├── PesajeServiceTest.java             # Tests del servicio principal
    ├── WeighingBusinessRulesServiceTest.java  # Tests de reglas de negocio
    └── WeighingStateServiceTest.java      # Tests de la máquina de estados
```

---

## Ejecutar los Tests

### Ejecutar todos los tests

```bash
./mvnw test
```

### Ejecutar y verificar cobertura 90%

```bash
# Ejecuta tests + genera reporte + verifica threshold 90%
./mvnw verify
```

Si la cobertura es menor al 90%, el build **fallará** automáticamente gracias a JaCoCo.

### Ejecutar un test específico

```bash
./mvnw test -Dtest=PesajeServiceTest
```

---

## Ver el Reporte de Cobertura

Después de ejecutar `./mvnw verify`, el reporte HTML se genera en:

```
target/site/jacoco/index.html
```

Abre en el navegador:

```bash
# macOS/Linux
open target/site/jacoco/index.html

# Windows
start target/site/jacoco/index.html
```

---

## Cobertura Requerida

| Métrica | Mínimo Requerido |
|---------|-----------------|
| Line Coverage | **≥ 90%** |
| Capas cubiertas | Todas (service, controller, integration, exception) |

La regla está configurada en `pom.xml`:

```xml
<limit>
  <counter>LINE</counter>
  <value>COVEREDRATIO</value>
  <minimum>0.90</minimum>
</limit>
```

---

## Tecnologías de Testing

| Tecnología | Uso |
|-----------|-----|
| **JUnit 5** | Framework base de tests |
| **Mockito** | Mocking de dependencias |
| **AssertJ** | Assertions fluidas |
| **Spring Boot Test** | Tests de integración con contexto Spring |
| **Flapdoodle Embed Mongo** | MongoDB embebido para tests |
| **Testcontainers Redis** | Redis en Docker para tests de integración |
| **JaCoCo** | Medición de cobertura de líneas |

---

## Casos de Test Relevantes

### Reglas de Negocio (`WeighingBusinessRulesServiceTest`)
- ✅ Paquete PESADO en horario nocturno → excepción
- ✅ Paquete PESADO con balanza prima en día impar → excepción
- ✅ Paquete LIVIANO sin restricciones → sin excepción
- ✅ Conversión correcta de kg a Sansas (÷ 1.337)
- ✅ Clasificación correcta de cada categoría

### Máquina de Estados (`WeighingStateServiceTest`)
- ✅ INGRESADO → PESADO (válido)
- ✅ PESADO → APROBADO (válido)
- ✅ PESADO → RECHAZADO (válido)
- ✅ APROBADO → DESPACHADO (válido)
- ✅ INGRESADO → APROBADO (inválido → IllegalWeighingStateException)

### Cliente Externo (`ExternalScaleClientTest`)
- ✅ API disponible → retorna especificaciones
- ✅ API caída → usa caché Redis (cache hit)
- ✅ API caída + sin caché → retorna especificación por defecto (id="-1")
- ✅ Reintentos exponenciales hasta 3 veces

### Tests de Integración REST (`PesajeControllerIntegrationTest`)
- ✅ POST /api/pesajes → 201
- ✅ PUT /api/pesajes/{id}/estado → 200
- ✅ GET /api/pesajes?fecha=... → 200
- ✅ Transición inválida → 400
- ✅ ID inexistente → 404
