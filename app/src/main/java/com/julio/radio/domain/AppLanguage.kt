package com.julio.radio.domain

enum class AppLanguage(val tag: String, val displayName: String) {
    ES("es", "Español"),
    EN("en", "English"),
    PT("pt", "Português"),
    FR("fr", "Français"),
    DE("de", "Deutsch"),
    IT("it", "Italiano");

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }
    }
}