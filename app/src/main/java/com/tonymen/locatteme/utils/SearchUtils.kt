package com.tonymen.locatteme.utils

object SearchUtils {
    fun generateSearchKeywords(nombres: String, apellidos: String): List<String> {
        val keywords = mutableListOf<String>()
        val fullName = "$nombres $apellidos".lowercase()
        val nameParts = fullName.split(" ")

        // Añadir todas las combinaciones de las partes del nombre completo
        for (i in nameParts.indices) {
            for (j in i until nameParts.size) {
                keywords.add(nameParts.subList(i, j + 1).joinToString(" "))
            }
        }

        // Añadir todas las subcadenas posibles de cada parte del nombre
        for (part in nameParts) {
            for (i in 1..part.length) {
                keywords.add(part.substring(0, i))
            }
        }

        return keywords.distinct()
    }
}
