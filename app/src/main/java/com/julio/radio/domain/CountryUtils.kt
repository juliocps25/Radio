package com.julio.radio.domain

import java.util.Locale

fun countryFlag(code: String): String {
    if (code.length != 2) return "\uD83C\uDF10"
    return code.uppercase().map { ch ->
        String(Character.toChars(0x1F1E6 + (ch - 'A')))
    }.joinToString("")
}

fun localizedCountryName(code: String, fallback: String): String {
    val name = Locale.Builder().setRegion(code.uppercase()).build().displayCountry
    return name.ifBlank { fallback }
}