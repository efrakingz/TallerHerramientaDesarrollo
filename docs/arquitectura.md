# Arquitectura del Sistema

## Stack Tecnológico

| Tecnología | Versión | Propósito |
|---|---|---|
| **Java** | 17 | Lenguaje base |
| **Spring Boot** | 3.3.0 | Framework principal |
| **Spring Web** | (auto) | API REST |
| **Spring Data MongoDB** | (auto) | Persistencia |
| **Spring Data Redis** | (auto) | Caché de balanzas |
| **Spring Retry** | (auto) | Reintentos exponenciales |
| **Lombok** | (auto) | Reducción de boilerplate |
| **SpringDoc OpenAPI** | 2.5.0 | Swagger UI |
| **JUnit 5 + Mockito** | (auto) | Suite de pruebas |
| **JaCoCo** | 0.8.11 | Cobertura ≥ 90% |

---

## Arquitectura en Capas

El proyecto sigue una **arquitectura en capas** estándar de Spring Boot:

```
┌─────────────────────────────────┐
│       PesajeController           │  ← Capa de presentación (REST)
├─────────────────────────────────┤
│  PesajeService                   │  ← Lógica de aplicación
│  WeighingBusinessRulesService    │  ← Reglas de negocio
│  WeighingStateService            │  ← Máquina de estados
├─────────────────────────────────┤
│  ExternalScaleClient             │  ← Integración externa (Redis fallback)
├─────────────────────────────────┤
│  RegistroPesajeRepository        │  ← Acceso a datos (MongoDB)
└─────────────────────────────────┘
```

---

## Estructura del Proyecto

```
sansaweigh/
├── src/
│   ├── main/
│   │   ├── java/com/sansaweigh/
│   │   │   ├── SansaWeighApplication.java       # Entry point
│   │   │   ├── config/
│   │   │   │   ├── AppConfig.java               # Configuración general
│   │   │   │   └── OpenApiConfig.java           # Configuración Swagger
│   │   │   ├── controller/
│   │   │   │   └── PesajeController.java        # Endpoints REST
│   │   │   ├── domain/
│   │   │   │   ├── RegistroPesaje.java          # Entidad principal (MongoDB)
│   │   │   │   ├── ScaleSpecification.java      # Specs de balanza (caché)
│   │   │   │   ├── StateTransition.java         # Historial de estados
│   │   │   │   ├── WeighingState.java           # Enum de estados
│   │   │   │   └── WeightCategory.java          # Enum de categorías
│   │   │   ├── dto/
│   │   │   │   ├── CreatePesajeRequest.java     # Request de creación
│   │   │   │   ├── UpdateEstadoRequest.java     # Request de cambio estado
│   │   │   │   └── PesajeResponse.java          # Response unificado
│   │   │   ├── exception/
│   │   │   │   ├── BusinessException.java       # Excepción de negocio base
│   │   │   │   ├── IllegalWeighingStateException.java  # Transición inválida
│   │   │   │   ├── ResourceNotFoundException.java      # Recurso no encontrado
│   │   │   │   └── GlobalExceptionHandler.java  # Handler centralizado
│   │   │   ├── integration/
│   │   │   │   └── ExternalScaleClient.java     # Cliente API externa + retry
│   │   │   ├── repository/
│   │   │   │   └── RegistroPesajeRepository.java # Repositorio MongoDB
│   │   │   └── service/
│   │   │       ├── PesajeService.java           # Servicio principal
│   │   │       ├── WeighingBusinessRulesService.java # Reglas de negocio
│   │   │       └── WeighingStateService.java    # Transiciones de estado
│   │   └── resources/
│   │       └── application.yml                  # Configuración
│   └── test/
│       ├── java/com/sansaweigh/
│       │   ├── controller/                      # Tests de integración REST
│       │   ├── exception/                       # Tests del exception handler
│       │   ├── integration/                     # Tests del cliente externo
│       │   └── service/                         # Tests unitarios de servicios
│       └── resources/
│           └── application-test.yml             # Configuración de tests
├── docs/                                        # Esta documentación (Docsify)
│   ├── index.html
│   ├── openapi.yaml                             # Spec OpenAPI 3.1
│   ├── README.md
│   ├── arquitectura.md
│   ├── reglas-negocio.md
│   ├── api-reference.md
│   ├── configuracion.md
│   └── pruebas.md
├── docker-compose.yml                           # MongoDB + Redis
└── pom.xml
```

---

## Flujo de una Petición

```
HTTP Request
     │
     ▼
PesajeController
     │ valida DTO (@Valid)
     ▼
PesajeService
     │ orquesta la lógica
     ├──► WeighingBusinessRulesService  (reglas horaria + balanza prima)
     ├──► ExternalScaleClient           (consulta balanza → Redis → default)
     ├──► WeighingStateService          (gestiona transiciones)
     └──► RegistroPesajeRepository     (persiste en MongoDB)
     │
     ▼
PesajeResponse (JSON)
```
