package app.nudroidlabs.nustrim.core.source.cloudstream

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import app.nudroidlabs.nustrim.BuildConfig
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.Plugin
import dalvik.system.PathClassLoader
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Minimal Android host for CloudStream .cs3 packages.
 *
 * Third-party packages are downloaded into app-private storage, optionally
 * checked against the SHA-256 value supplied by plugins.json, marked read-only,
 * then loaded with PathClassLoader.
 */
class CloudStreamRuntime(context: Context) {
    private val appContext = context.applicationContext
    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionCache = ConcurrentHashMap<String, CloudStreamProviderContainerSession>()

    fun open(
        pluginUrl: String,
        internalName: String,
        expectedHash: String = "",
        onSuccess: (CloudStreamProviderContainerSession) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val cacheKey = "$pluginUrl|$expectedHash"
        sessionCache[cacheKey]?.let {
            onSuccess(it)
            return
        }

        executor.execute {
            try {
                val pluginFile = downloadPlugin(pluginUrl, internalName, expectedHash)
                val providers = loadProviders(pluginFile)
                require(providers.isNotEmpty()) {
                    "CloudStream plugin loaded but registered no MainAPI provider: $internalName"
                }

                val session = CloudStreamProviderContainerSession(
                    pluginUrl = pluginUrl,
                    pluginName = internalName,
                    providers = providers
                )
                sessionCache[cacheKey] = session
                mainHandler.post { onSuccess(session) }
            } catch (t: Throwable) {
                mainHandler.post { onError(unwrap(t)) }
            }
        }
    }

    private fun downloadPlugin(pluginUrl: String, internalName: String, expectedHash: String): File {
        val storageRoot = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val dir = File(storageRoot, "cloudstream_plugins")
        if (!dir.exists()) require(dir.mkdirs()) { "Unable to create CloudStream plugin cache" }

        val safeName = internalName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(48).ifBlank { "provider" }
        val normalizedExpected = normalizeSha256(expectedHash)
        val identity = normalizedExpected?.take(16) ?: sha256Text(pluginUrl).take(16)
        val finalFile = File(dir, "${safeName}_$identity.cs3")
        val tempFile = File(dir, "${safeName}_$identity.tmp")

        if (finalFile.exists()) {
            val valid = normalizedExpected == null || sha256File(finalFile).equals(normalizedExpected, ignoreCase = true)
            if (valid) {
                finalFile.setReadOnly()
                return finalFile
            }
            finalFile.setWritable(true, true)
            finalFile.delete()
        }
        if (tempFile.exists()) tempFile.delete()

        val connection = URL(pluginUrl).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.requestMethod = "GET"
            connection.setRequestProperty(
                "User-Agent",
                "Nustrim/${BuildConfig.VERSION_NAME} CloudStreamHost"
            )
            connection.connect()

            val code = connection.responseCode
            require(code in 200..299) { "Plugin download failed: HTTP $code" }
            tempFile.outputStream().use { output ->
                connection.inputStream.use { input -> input.copyTo(output) }
            }
            require(tempFile.length() > 0L) { "Downloaded CloudStream plugin is empty" }
        } finally {
            connection.disconnect()
        }

        if (normalizedExpected != null) {
            val actual = sha256File(tempFile)
            require(actual.equals(normalizedExpected, ignoreCase = true)) {
                "CloudStream plugin integrity check failed for $internalName"
            }
        }

        require(tempFile.renameTo(finalFile)) { "Unable to store downloaded CloudStream plugin" }
        require(finalFile.setReadOnly()) { "Unable to mark CloudStream plugin read-only" }
        return finalFile
    }

    private fun loadProviders(file: File): List<MainAPI> {
        val filePath = file.absolutePath
        val loader = PathClassLoader(filePath, appContext.classLoader)
        val manifestJson = loader.getResourceAsStream("manifest.json")?.bufferedReader()?.use { it.readText() }
            ?: error("CloudStream plugin contains no manifest.json")
        val manifest = JSONObject(manifestJson)
        val pluginClassName = manifest.optString("pluginClassName").trim()
        require(pluginClassName.isNotBlank()) { "CloudStream manifest has no pluginClassName" }
        val requiresResources = manifest.optBoolean("requiresResources", false)

        val before = APIHolder.allProviders.toList().toSet()

        val pluginClass = loader.loadClass(pluginClassName)
        val pluginInstance = pluginClass.getDeclaredConstructor().newInstance() as? BasePlugin
            ?: error("$pluginClassName does not extend CloudStream BasePlugin")
        pluginInstance.filename = filePath

        if (requiresResources && pluginInstance is Plugin) {
            injectResources(pluginInstance, file)
        }

        if (pluginInstance is Plugin) {
            pluginInstance.load(appContext)
        } else {
            pluginInstance.load()
        }

        val providers = APIHolder.allProviders.toList()
            .filter { provider ->
                provider.sourcePlugin == filePath || provider !in before
            }
            .distinctBy { "${it.name}|${it.mainUrl}|${it::class.qualifiedName}" }
        providers.forEach { it.init() }
        return providers
    }

    @Suppress("DEPRECATION")
    private fun injectResources(plugin: Plugin, file: File) {
        val assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
        val addAssetPath = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
        addAssetPath.invoke(assets, file.absolutePath)
        plugin.resources = Resources(
            assets,
            appContext.resources.displayMetrics,
            appContext.resources.configuration
        )
    }

    private fun normalizeSha256(value: String): String? {
        val clean = value.trim().removePrefix("sha256-").removePrefix("SHA256-")
        return clean.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }
    }

    private fun sha256Text(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun unwrap(t: Throwable): Throwable {
        var current = t
        while (
            current.cause != null &&
            (current is java.lang.reflect.InvocationTargetException || current is ExceptionInInitializerError)
        ) {
            current = current.cause ?: break
        }
        return current
    }
}
