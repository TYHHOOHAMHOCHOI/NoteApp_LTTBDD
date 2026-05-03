package com.example.noteapp_lttbdd

import android.app.Application
import android.content.Context

class NoteApplication : Application() {
    override fun attachBaseContext(base: Context) {
        val lang = LocaleHelper.getLanguage(base)
        super.attachBaseContext(LocaleHelper.setLocale(base, lang))
    }
}
