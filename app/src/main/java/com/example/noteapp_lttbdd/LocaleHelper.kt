package com.example.noteapp_lttbdd

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"

    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }

    private fun persist(context: Context, language: String) {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(SELECTED_LANGUAGE, language)
        editor.apply()
    }

    @Suppress("DEPRECATION")
    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            val localeList = android.os.LocaleList(locale)
            android.os.LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
        } else {
            configuration.locale = locale
        }

        return context.createConfigurationContext(configuration)
    }

    fun getLanguage(context: Context): String {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return preferences.getString(SELECTED_LANGUAGE, "vi") ?: "vi"
    }
}
