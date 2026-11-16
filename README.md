# Servicio Asistente Clínico

Permite a los médicos consultar los protocolos clínicos internos en lenguaje natural (RAG sobre un corpus de protocolos, con Gemini),
con respuestas que **siempre citan su fuente** y sin contenido inventado, y ofrece la cantidad de pacientes activos por nivel de triaje,
reconstruida únicamente con los eventos que publica el servicio de Triage.

La documentación completa (cómo ejecutar todo con un comando, arquitectura, supuestos y ADR) está en el repositorio
[`vidacruz-platform`](https://github.com/otalvaroandres570-arch/vidacruz-platform).

## Ejecutar y probar

./gradlew bootRun # necesita Redis y Kafka (ver la plataforma) y las variables de abajo
./gradlew test # requiere Docker (Testcontainers); el LLM está simulado
./gradlew geminiTest # contra Gemini real; necesita GEMINI_API_KEY


## Configuración (variables de entorno)

| Variable | Por defecto | Descripción |
|---|---|---|
| `ASSISTANT_SECURITY_APIKEYS` | (obligatoria) | API keys aceptadas, separadas por comas, de 16 o más caracteres |
| `GEMINI_API_KEY` | vacía | Key de Gemini (secreto); sin ella, las consultas responden `503` |
| `GEMINI_CHAT_MODEL`, `GEMINI_EMBEDDING_MODEL` | `gemini-3.5-flash`, `gemini-embedding-001` | Modelos (los de Gemini se retiran a menudo) |
| `REDIS_HOST`, `REDIS_PORT` | `localhost`, `6379` | Vista de urgencias |
| `ASSISTANT_KAFKA_BOOTSTRAPSERVERS`, `ASSISTANT_KAFKA_TOPIC`, `ASSISTANT_KAFKA_GROUPID` | `localhost:9092`, `triaje.eventos`, `assistant-service` | Consumo de eventos |
| `ASSISTANT_RAG_SIMILARITYTHRESHOLD` | `0.65` | Umbral de relevancia; recalibrar si cambia el modelo de embeddings |
| `ASSISTANT_RESILIENCE_TIMEOUT` | `20s` | Tiempo máximo de cada llamada a Gemini |
| `ASSISTANT_CORPUS_LOCATION` | `protocols/protocols.json` | Corpus de protocolos (ampliable sin tocar código) |
| `SPRING_PROFILES_ACTIVE` | | `json` para logs en JSON |

Puerto: `8082`. Probes públicos: `/actuator/health/liveness` y `/actuator/health/readiness`.