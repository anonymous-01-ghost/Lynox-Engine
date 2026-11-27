# Lynox Engine — Plan maestro de desarrollo

- **Producto:** Lynox Engine
- **Repositorio:** anonymous-01-ghost/Lynox-Engine
- **Objetivo:** biblioteca propia de extracción multimedia, multiplataforma y consumida directamente por aplicaciones.
- **Plataformas:** Android, iOS y PC/JVM.
- **Modelo:** ejecución dentro de la aplicación; sin backend obligatorio, sin Google Cloud obligatorio y sin tokens de IA.

## 1. Objetivo real del producto

Lynox Engine se construye para que LynoxMusic y otras aplicaciones puedan integrar un motor propio como dependencia. El objetivo no es envolver la YouTube Data API ni crear otra API alojada.

El producto final debe resolver fuentes compatibles, normalizar sus metadatos y devolver las variantes multimedia disponibles para que el reproductor de cada plataforma las utilice cuando el acceso esté permitido.

No se debe presentar el proyecto como sustituto completo de NewPipe Extractor hasta implementar y validar al menos un adaptador real de fuente. El contrato común ya creado es solo la base de esa arquitectura.

## 2. Principios no negociables

1. Biblioteca propia, con código y versiones controlados en este repositorio.
2. Sin servidor propio obligatorio.
3. Sin dependencia central de YouTube Data API, Google Cloud, cuotas o claves.
4. Sin dependencia de NewPipe Extractor en tiempo de ejecución.
5. Kotlin Multiplatform para compartir la lógica donde sea viable.
6. Adaptadores independientes por fuente; el core no debe conocer detalles privados de una fuente.
7. No almacenar URLs firmadas o temporales en la caché de metadatos.
8. Pruebas de parser con fixtures, pruebas de red, expiración y reproducción por plataforma.
9. No implementar evasión de DRM, autenticación, paywalls ni controles de acceso.
10. No afirmar que una función existe hasta que el código y las pruebas lo demuestren.

## 3. Plataformas y entregables

| Plataforma | Integración | Artefacto |
|---|---|---|
| Android | Kotlin / Gradle | AAR |
| iOS | Swift mediante wrapper Kotlin/Native | XCFramework |
| PC | Kotlin/JVM | JAR |

La lógica común se ubicará en commonMain. Las integraciones de reproducción se mantendrán específicas de cada plataforma.

## 4. Arquitectura objetivo

- **Core:** modelos, resultados tipados, validación, configuración y contratos públicos.
- **Extractor API:** entrada URL de origen y salida de un objeto multimedia normalizado.
- **Source adapters:** resolución específica por fuente, con parsers, red y política de actualización aislados.
- **Playback integration:** entrega de una variante compatible al reproductor Android, AVFoundation en iOS o reproductor de escritorio.
- **Samples:** consumidores reales para Android, iOS y JVM.

La base del extractor incorporada ahora contiene MediaResolver, ResolvedMedia, MediaStream y MediaStreamKind. LynoxEngine expone resolveMedia(). Esto define la interfaz, pero no equivale a un adaptador funcional de YouTube u otra fuente.

## 5. Estado real del repositorio

### Ya existe
- Proyecto Kotlin Multiplatform con targets Android, iOS y JVM.
- LynoxEngine y contratos de búsqueda, metadatos y catálogo.
- Modelos de resultados y errores tipados.
- Caché LRU con TTL para resultados de metadatos/catálogo.
- Infraestructura HTTP compartida de reintentos acotados.
- Contrato MediaResolver, modelos ResolvedMedia/MediaStream y método resolveMedia().
- Pruebas del contrato de resolución y validación básica de URL.

### Código retirado
- Los adaptadores YouTube Data API y sus pruebas fueron eliminados del repositorio: no eran extractores y no correspondían al objetivo del proyecto.

### Pendiente
- Implementar un primer adaptador de fuente permitido y verificable.
- Parser de manifiestos/formatos cuando corresponda a la fuente y sus permisos.
- Modelo de expiración y renovación de URLs temporales.
- Selección de formatos por capacidades del reproductor.
- Pruebas con fixtures reales anonimizados y respuestas cambiantes.
- Muestras consumidoras Android, iOS y JVM.
- Validar build y XCFramework en CI; estado actual pendiente hasta que Actions finalice.

## 6. API pública de extracción

La interfaz común:

    interface MediaResolver {
        suspend fun resolve(sourceUrl: String): EngineResult<ResolvedMedia>
    }

El resultado normalizado incluye:
- URL de origen;
- título y duración cuando estén disponibles;
- una o más variantes multimedia;
- tipo de variante: audio, video o audio/video;
- MIME type, codecs, bitrate, resolución, frame rate y tamaño cuando estén disponibles;
- expiración opcional para URLs temporales.

Las URLs de streams no se deben guardar en la caché de metadatos porque pueden expirar o estar firmadas.

## 7. Reglas de fuentes

Cada adaptador debe declarar fuente, tipos de URL admitidos, método de acceso, autenticación, restricciones, plataformas, expiración y comportamiento ante fallos.

No se asumirá que una API de metadatos entrega streams. Para YouTube, la Data API oficial no entrega URLs multimedia directas; no debe confundirse con un extractor. El proyecto no debe prometer extracción directa de YouTube ni evadir controles de acceso. Para reproducción de YouTube se deben utilizar las integraciones admitidas por el servicio.

## 8. Fases de desarrollo

### Fase A — Corregir la dirección del proyecto
- Documentación alineada al objetivo de biblioteca extractor.
- Retirar los adaptadores de metadatos basados en YouTube Data API del core.
- Contrato común de resolución integrado en LynoxEngine.
- Pruebas unitarias del contrato y validación de URL.

**Estado:** base iniciada; build CI pendiente.

### Fase B — Primer adaptador de fuente
- Seleccionar una fuente cuyo acceso y resolución estén permitidos.
- Implementar parser aislado en su módulo.
- Añadir fixtures, tests positivos/negativos y errores de red.
- Definir expiración/renovación y compatibilidad de formatos.

**Estado:** pendiente.

### Fase C — Integración real en plataformas
- Consumidor Android que incorpora el AAR.
- Consumidor JVM que incorpora el JAR.
- Wrapper Swift e integración XCFramework.
- Prueba de reproducción de formatos permitidos en dispositivos reales.

**Estado:** pendiente.

### Fase D — Estabilidad y distribución
- API pública documentada y versionada.
- Pruebas de regresión y compatibilidad.
- Publicación local y después repositorio remoto elegido explícitamente.
- Licencia elegida antes de distribución externa.

**Estado:** pendiente.

## 9. Integración objetivo con LynoxMusic

La aplicación LynoxMusic actualmente usa NewPipe Extractor v0.27.0 desde YouTubeAudio.kt para búsqueda, metadatos, paginación y resolución de audio M4A progresivo. La especificación de migración está en docs/LYNOXMUSIC_MIGRATION.md. No se quitará la dependencia de NewPipe en LynoxMusic hasta que un adaptador propio supere las pruebas de paridad y reproducción.

## 10. Criterio de finalización

Lynox Engine solo se considerará extractor funcional cuando al menos un adaptador real pueda resolver una fuente admitida, devuelva variantes válidas, maneje errores y expiración, pase pruebas automatizadas y se integre con éxito en Android, iOS y JVM según las capacidades de cada fuente.
