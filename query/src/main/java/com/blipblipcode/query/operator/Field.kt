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
    override val value: T,
): SQLOperator<T>{

    override val symbol: String= "="
    override val column: String = name
    override val caseConversion: CaseConversion = CaseConversion.NONE
    init {
        require(name.isNotBlank()) { "Field name cannot be blank" }

    }
    /**
     * Returns the field as a SQL assignment string (e.g., "name = 'value'").
     * It correctly formats the value based on its type (e.g., quoting strings).
     * @return A SQL assignment string.
     */
    override fun asString(): String{
        val valueStr = valueString()
        return "$name $symbol $valueStr"
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