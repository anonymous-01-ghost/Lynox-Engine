package dev.lynox.engine.extractor

import dev.lynox.engine.model.EngineResult

/**
 * Source-specific resolver contract.
 *
 * Implementations belong in dedicated provider modules. They must use access
 * methods permitted for the source and must not bypass DRM, authentication,
 * paywalls, or other access controls.
 */
interface MediaResolver {
    suspend fun resolve(sourceUrl: String): EngineResult<ResolvedMedia>
}
