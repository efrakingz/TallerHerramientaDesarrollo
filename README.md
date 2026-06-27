# SansaWeigh Microservice

> Sistema de Gestión de Estaciones de Pesaje de Paquetes  
> **Universidad Técnica Federico Santa María**

---

## 📖 Documentación Interactiva (Docsify)

La documentación completa del proyecto está construida con **Docsify** y disponible en la carpeta `docs/`.

Para visualizarla localmente:

```bash
npx docsify-cli serve docs
```

Luego abre: `http://localhost:3000`

---

## 📡 API — Swagger UI

Con la aplicación corriendo, accede a Swagger UI:

👉 `http://localhost:8080/swagger-ui.html`

Especificación OpenAPI 3.1 estática: [`docs/openapi.yaml`](docs/openapi.yaml)

---

## 🚀 Inicio Rápido

```bash
# 1. Levantar MongoDB + Redis
docker-compose up -d

# 2. Compilar y correr
./mvnw spring-boot:run

# 3. Verificar Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## 🧪 Ejecutar las Pruebas

```bash
# Ejecutar suite de pruebas + verificar cobertura 90%
./mvnw verify

# Ver reporte de cobertura
open target/site/jacoco/index.html
```

---

## ⚖️ Reglas de Negocio Principales

- **1 Sansa = 1.337 kg** (unidad propietaria)
- **LIVIANO**: ≤ 10 Sansas | **MEDIANO**: ≤ 50 Sansas | **PESADO**: > 50 Sansas
- **Restricción nocturna**: PESADO bloqueado entre 20:00 y 06:00
- **Regla prima**: ID de balanza primo + día impar = PESADO bloqueado
- **Estados**: `INGRESADO → PESADO → APROBADO/RECHAZADO → DESPACHADO`

---

## 🛠️ Stack Tecnológico

| Tecnología | Versión | Propósito |
|---|---|---|
| Java | 17 | Lenguaje base |
| Spring Boot | 3.3.0 | Framework principal |
| Spring Data MongoDB | auto | Persistencia |
| Spring Data Redis | auto | Caché de balanzas |
| Spring Retry | auto | Reintentos exponenciales |
| Lombok | auto | Boilerplate reduction |
| SpringDoc OpenAPI | 2.5.0 | Swagger UI |
| JUnit 5 + Mockito | auto | Suite de pruebas |
| JaCoCo | 0.8.11 | Cobertura ≥ 90% |

---

## 📁 Estructura del Proyecto

```
sansaweigh/
├── src/main/java/com/sansaweigh/
│   ├── SansaWeighApplication.java
│   ├── config/          # AppConfig, OpenApiConfig
│   ├── controller/      # PesajeController
│   ├── domain/          # RegistroPesaje, WeighingState, WeightCategory, ScaleSpecification
│   ├── dto/             # CreatePesajeRequest, UpdateEstadoRequest, PesajeResponse
│   ├── exception/       # BusinessException, IllegalWeighingStateException, GlobalExceptionHandler
│   ├── integration/     # ExternalScaleClient (Retry + Redis Fallback)
│   ├── repository/      # RegistroPesajeRepository
│   └── service/         # PesajeService, WeighingBusinessRulesService, WeighingStateService
├── src/test/java/       # Tests unitarios e integración (cobertura ≥ 90%)
├── docs/                # Docsify + OpenAPI 3.1 spec
│   ├── index.html       # Entrada Docsify
│   ├── openapi.yaml     # Especificación OpenAPI 3.1
│   ├── README.md        # Inicio de la documentación
│   ├── arquitectura.md
│   ├── reglas-negocio.md
│   ├── api-reference.md
│   ├── configuracion.md
│   └── pruebas.md
├── docker-compose.yml   # MongoDB + Redis
└── pom.xml
```
