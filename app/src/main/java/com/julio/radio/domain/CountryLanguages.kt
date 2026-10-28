package com.julio.radio.domain

object CountryLanguages {
    private val spanish = setOf(
        "ES", "MX", "AR", "CO", "PE", "VE", "CL", "EC", "GT", "CU", "BO", "DO",
        "HN", "PY", "SV", "NI", "CR", "PA", "UY", "PR", "GQ"
    )
    private val portuguese = setOf("BR", "PT", "AO", "MZ", "CV", "GW", "ST", "TL")
    private val french = setOf(
        "FR", "BE", "LU", "MC", "SN", "CI", "CM", "CD", "CG", "GA", "ML", "BF",
        "NE", "TG", "BJ", "GN", "MG", "HT", "DJ"
    )
    private val german = setOf("DE", "AT", "CH", "LI")
    private val italian = setOf("IT", "SM", "VA")

    fun forCountry(code: String?): AppLanguage {
        val c = code?.uppercase() ?: return AppLanguage.EN
        return when (c) {
            in spanish -> AppLanguage.ES
            in portuguese -> AppLanguage.PT
            in french -> AppLanguage.FR
            in german -> AppLanguage.DE
            in italian -> AppLanguage.IT
            else -> AppLanguage.EN
        }
    }
}