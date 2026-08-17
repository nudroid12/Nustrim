package com.lagradost.cloudstream3.plugins

import android.content.Context
import android.content.res.Resources

/**
 * Minimal Android host ABI used by CloudStream .cs3 extensions.
 * Shared provider APIs come from the official CloudStream library dependency.
 */
abstract class Plugin : BasePlugin() {
    @Throws(Throwable::class)
    open fun load(context: Context) {
        load()
    }

    var resources: Resources? = null
    var openSettings: ((context: Context) -> Unit)? = null
}
