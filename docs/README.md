# SansaWeigh — Documentación

> Microservicio de Gestión de Estaciones de Pesaje de Paquetes  
> **Universidad Técnica Federico Santa María**

---

## ¿Qué es SansaWeigh?

SansaWeigh es un microservicio desarrollado con **Spring Boot** para la empresa de logística del mismo nombre. Permite gestionar estaciones de pesaje de paquetes con las siguientes capacidades:

- 📦 Clasificación de paquetes por peso en unidades **Sansa**
- 🔄 Máquina de estados del ciclo de vida de cada pesaje
- 🗄️ Persistencia del historial en **MongoDB**
- ⚡ Caché de especificaciones de balanzas en **Redis**
- 🔌 Integración con API externa de balanzas (con fallback resiliente)
- 📊 Cobertura de pruebas ≥ 90% (JUnit 5 + Mockito + JaCoCo)

---

## Inicio Rápido

```bash
# 1. Clonar el repositorio
git clone <repo-url>
cd sansaweigh

# 2. Levantar MongoDB + Redis con Docker
docker-compose up -d

# 3. Compilar y ejecutar
./mvnw spring-boot:run

# 4. Verificar que está corriendo
curl http://localhost:8080/actuator/health
```

Una vez iniciado, accede a:

| Recurso | URL |
|---------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Esta documentación | `npx docsify-cli serve docs` |

---

## Navegación

Usa el menú lateral para explorar:

- **[Arquitectura](arquitectura.md)** — Stack tecnológico y estructura del proyecto
- **[Reglas de Negocio](reglas-negocio.md)** — Lógica del dominio SansaWeigh
- **[API Reference](api-reference.md)** — Endpoints, ejemplos y respuestas
- **[Configuración del Entorno](configuracion.md)** — Setup de MongoDB, Redis y variables
- **[Suite de Pruebas](pruebas.md)** — Cómo ejecutar y verificar la cobertura
