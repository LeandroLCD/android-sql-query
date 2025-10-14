package com.blipblipcode.query.operator

/**
 * Represents a field (or column) in a database table, consisting of a name and a value.
 * This is typically used for INSERT or UPDATE operations.
 *
 * @param T The type of the field's value.
 * @property name The name of the column.
 * @property value The value of the field.
 */
data class Field<T>(
    val name: String,
    val value: T?,
){
    init {
        require(name.isNotBlank()) { "Field name cannot be blank" }

    }
    /**
     * Returns the field as a SQL assignment string (e.g., "name = 'value'").
     * It correctly formats the value based on its type (e.g., quoting strings).
     * @return A SQL assignment string.
     */
    fun asString(): String{
        val valueStr = valueString()
        return "$name = $valueStr"
    }

    /**
     * Returns the string representation of the field's value, formatted for use in a SQL query.
     * Strings are enclosed in single quotes; other types are converted to their string representation.
     * @return The formatted value as a string.
     */
    fun valueString(): String{
        return when(value){
            is String -> "'$value'"
            else -> value.toString()
        }
    }
}