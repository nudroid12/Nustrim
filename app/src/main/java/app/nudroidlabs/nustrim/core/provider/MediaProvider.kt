package app.nudroidlabs.nustrim.core.provider

import app.nudroidlabs.nustrim.core.model.MediaCatalog

interface MediaProvider {
    val id: String
    val displayName: String

    fun loadCatalog(
        source: String,
        onSuccess: (MediaCatalog) -> Unit,
        onError: (Throwable) -> Unit
    )
}
