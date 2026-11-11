package com.miniproject.marbleshooter.game.render

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** Resolves the player's selected language independently of the device locale. */
internal class GameTextResources(context: Context) {
    private val appContext = context.applicationContext
    private val russianResources = resourcesForLanguage("ru")
    private val englishResources = resourcesForLanguage("en")

    fun getString(language: String, resourceId: Int, vararg formatArgs: Any): String {
        val selected = if (language == "en") englishResources else russianResources
        return if (formatArgs.isEmpty()) selected.getString(resourceId)
        else selected.getString(resourceId, *formatArgs)
    }

    private fun resourcesForLanguage(languageTag: String): Resources {
        val configuration = Configuration(appContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return appContext.createConfigurationContext(configuration).resources
    }
}
