package app.nudroidlabs.nustrim.core.source.stremio

import org.json.JSONArray
import org.json.JSONObject

data class StremioResource(
    val name: String,
    val types: Set<String> = emptySet(),
    val idPrefixes: List<String> = emptyList()
) {
    fun supports(type: String, id: String): Boolean {
        if (types.isNotEmpty() && type !in types) return false
        if (idPrefixes.isNotEmpty() && idPrefixes.none { id.startsWith(it) }) return false
        return true
    }
}

data class StremioCatalogDef(
    val type: String,
    val id: String,
    val name: String,
    val requiresExtra: Boolean,
    val supportsSearch: Boolean
)

data class StremioManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val types: Set<String>,
    val resources: List<StremioResource>,
    val catalogs: List<StremioCatalogDef>,
    val configurable: Boolean,
    val configurationRequired: Boolean
) {
    fun supports(resource: String, type: String, id: String): Boolean =
        resources.filter { it.name == resource }.any { it.supports(type, id) }

    fun hasResource(resource: String): Boolean = resources.any { it.name == resource }

    companion object {
        fun parse(root: JSONObject): StremioManifest {
            val resources = buildList {
                val array = root.optJSONArray("resources") ?: JSONArray()
                for (i in 0 until array.length()) {
                    when (val value = array.opt(i)) {
                        is String -> add(StremioResource(name = value))
                        is JSONObject -> add(
                            StremioResource(
                                name = value.optString("name"),
                                types = value.optJSONArray("types").toStringSet(),
                                idPrefixes = value.optJSONArray("idPrefixes").toStringList()
                            )
                        )
                    }
                }
            }.filter { it.name.isNotBlank() }

            val catalogs = buildList {
                val array = root.optJSONArray("catalogs") ?: JSONArray()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val extra = item.optJSONArray("extra")
                    var required = (item.optJSONArray("extraRequired")?.length() ?: 0) > 0
                    var supportsSearch = false

                    if (extra != null) {
                        for (j in 0 until extra.length()) {
                            val extraItem = extra.optJSONObject(j) ?: continue
                            if (extraItem.optBoolean("isRequired", false)) required = true
                            if (extraItem.optString("name").equals("search", ignoreCase = true)) {
                                supportsSearch = true
                            }
                        }
                    }

                    val requiredNames = item.optJSONArray("extraRequired")
                    if (requiredNames != null) {
                        for (j in 0 until requiredNames.length()) {
                            if (requiredNames.optString(j).equals("search", ignoreCase = true)) {
                                supportsSearch = true
                            }
                        }
                    }

                    val type = item.optString("type")
                    val id = item.optString("id")
                    if (type.isNotBlank() && id.isNotBlank()) {
                        add(
                            StremioCatalogDef(
                                type = type,
                                id = id,
                                name = item.optString("name", id),
                                requiresExtra = required,
                                supportsSearch = supportsSearch
                            )
                        )
                    }
                }
            }

            val behaviorHints = root.optJSONObject("behaviorHints")
            return StremioManifest(
                id = root.getString("id"),
                name = root.getString("name"),
                version = root.getString("version"),
                description = root.optString("description", "Stremio addon"),
                types = root.optJSONArray("types").toStringSet(),
                resources = resources,
                catalogs = catalogs,
                configurable = behaviorHints?.optBoolean("configurable", false) == true,
                configurationRequired = behaviorHints?.optBoolean("configurationRequired", false) == true
            )
        }
    }
}

private fun JSONArray?.toStringSet(): Set<String> = toStringList().toSet()

private fun JSONArray?.toStringList(): List<String> = buildList {
    val array = this@toStringList ?: return@buildList
    for (i in 0 until array.length()) {
        val value = array.optString(i)
        if (value.isNotBlank()) add(value)
    }
}
