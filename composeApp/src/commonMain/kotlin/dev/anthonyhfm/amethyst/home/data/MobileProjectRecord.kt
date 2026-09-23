package dev.anthonyhfm.amethyst.home.data

import kotlinx.serialization.Serializable

/** Platform-independent catalog entry. Paths are resolved by MobileFileStorage. */
@Serializable
data class MobileProjectRecord(
    val id: String,
    val title: String,
    val originalPath: String,
    val importedAt: Long,
    val hubProjectId: String? = null,
    val convertedPath: String? = null,
    val sourceHash: String? = null,
    val convertedSourceHash: String? = null,
    val converterVersion: Int = 0,
)
