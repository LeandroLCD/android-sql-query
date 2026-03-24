package com.blipblipcode.query.operator

/**
 * Crea una función de transformación SQL que elimina (con REPLACE encadenado)
 * los caracteres presentes en [this].
 * Cada carácter se eliminará mediante REPLACE(..., 'c', '').
 * Si [this] está vacío, devuelve la identidad (no transforma nada).
 *
 * Ejemplo de uso:
 * ```kotlin
 * val strip = "-. ".removeCharsTransform()
 * query.orderBy(OrderBy.Asc("name", collation = Collation.NOCASE, transform = strip))
 * // -> ORDER BY REPLACE(REPLACE(REPLACE(name, '-', ''), '.', ''), ' ', '') COLLATE NOCASE ASC
 * ```
 */
fun String.removeCharsTransform(): (String) -> String {
    if (isEmpty()) return { it }
    val chars = this
    return { expr ->
        var result = expr
        for (ch in chars) {
            val escaped = if (ch == '\'') "''" else ch.toString()
            result = "REPLACE($result, '$escaped', '')"
        }
        result
    }
}
