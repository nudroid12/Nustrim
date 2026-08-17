package app.nudroidlabs.nustrim.core.source.cloudstream

import android.content.Context
import app.nudroidlabs.nustrim.core.model.MediaItem
import java.security.MessageDigest

/** Persists per-provider enable state inside a CloudStream repository. */
class CloudStreamProviderStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "nustrim_cloudstream_provider_state",
        Context.MODE_PRIVATE
    )

    fun isEnabled(repositoryId: String, item: MediaItem): Boolean =
        preferences.getBoolean(key(repositoryId, providerIdentity(item)), true)

    fun setEnabled(repositoryId: String, item: MediaItem, enabled: Boolean) {
        preferences.edit().putBoolean(key(repositoryId, providerIdentity(item)), enabled).apply()
    }

    private fun providerIdentity(item: MediaItem): String = providerIdentityInternal(item)

    private fun key(repositoryId: String, providerId: String): String =
        "enabled_${sha256("$repositoryId|$providerId")}"

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    companion object {
        fun providerIdentityInternal(item: MediaItem): String =
            item.ref?.metaId?.takeIf { it.isNotBlank() }
                ?: item.ref?.mediaType?.takeIf { it.isNotBlank() }
                ?: item.id.ifBlank { item.title }
    }
}
