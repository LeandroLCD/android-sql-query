package com.blipblipcode.query.operator

enum class CaseConversion {
    NONE,
    LOWER,
    UPPER;
    fun asSqlFunction(value: Any): String {
        return when (this) {
            NONE -> value.toString()
            LOWER -> "LOWER($value)"
            UPPER -> "UPPER($value)"
        }
    }
}