package app.nudroidlabs.nustrim.core.source.cloudstream

import android.util.Base64
import app.nudroidlabs.nustrim.core.model.MediaItem
import app.nudroidlabs.nustrim.core.model.MediaRef
import org.json.JSONObject

/** Stable route from a CloudStream search result back to its .cs3 provider. */
data class CloudStreamProviderLocator(
    val pluginUrl: String,
    val internalName: String,
    val expectedHash: String,
    val providerName: String,
) {
    fun encode(): String = Base64.encodeToString(
        JSONObject()
            .put("pluginUrl", pluginUrl)
            .put("internalName", internalName)
            .put("expectedHash", expectedHash)
            .put("providerName", providerName)
            .toString()
            .toByteArray(),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    fun attach(item: MediaItem): MediaItem {
        val ref = item.ref ?: MediaRef(
            sourceKind = "cloudstream-item",
            mediaType = item.type.name,
            metaId = item.id,
        )
        return item.copy(ref = ref.copy(providerLocator = encode()))
    }

    companion object {
        fun fromPlugin(plugin: MediaItem, providerName: String): CloudStreamProviderLocator? {
            val ref = plugin.ref ?: return null
            val pluginUrl = ref.metaId.takeIf { it.isNotBlank() } ?: return null
            return CloudStreamProviderLocator(
                pluginUrl = pluginUrl,
                internalName = ref.mediaType.ifBlank { plugin.id.ifBlank { plugin.title } },
                expectedHash = ref.integrity,
                providerName = providerName,
            )
        }

        fun decode(value: String): CloudStreamProviderLocator? = runCatching {
            val raw = String(Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            val root = JSONObject(raw)
            CloudStreamProviderLocator(
                pluginUrl = root.getString("pluginUrl"),
                internalName = root.getString("internalName"),
                expectedHash = root.optString("expectedHash"),
                providerName = root.getString("providerName"),
            ).takeIf { it.pluginUrl.isNotBlank() && it.providerName.isNotBlank() }
        }.getOrNull()
    }
}
