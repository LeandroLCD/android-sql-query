package com.blipblipcode.query.operator

/**
 * A sealed interface representing a SQL operator for use in WHERE clauses.
 * It defines the common properties of a SQL operator, such as the column, the value, and the symbol.
 *
 * @param T The type of the value being compared.
 */
sealed interface SQLOperator<T> {
    val symbol: String
    val column: String
    val value: T

    /**
     * Returns a `Pair` of the column name and its value.
     */
    fun toPair(): Pair<String, T> = column to value

    /**
     * Converts the operator into its SQL string representation.
     * This default implementation handles basic cases and should be overridden for complex types.
     * @return The SQL string for the operator.
     */
    fun toSQLString(): String {
        val valueStr = when (value) {
            is String -> "'$value'"
            else -> value.toString()
        }
        return "$column $symbol $valueStr"
    }

    /**
     * Provides a simple string representation of the operator.
     */
    fun asString(): String = "$column $symbol $value"

    /** Represents an "=" operation. */
    data class Equals<T>(override val column: String, override val value: T) : SQLOperator<T> {
        override val symbol: String = "="
    }

    /** Represents a "!=" operation. */
    data class NotEquals<T>(override val column: String, override val value: T) : SQLOperator<T> {
        override val symbol: String = "!="
    }

    /** Represents a ">" operation. */
    data class GreaterThan<T>(override val column: String, override val value: T) : SQLOperator<T> {
        override val symbol: String = ">"
    }

    /** Represents a "<" operation. */
    data class LessThan<T>(override val column: String, override val value: T) : SQLOperator<T> {
        override val symbol: String = "<"
    }

    /** Represents a ">=" operation. */
    data class GreaterThanOrEqual<T>(override val column: String, override val value: T) : SQLOperator<T> {
        override val symbol: String = ">="
    }

    /** Represents a "<=" operation. */
    data class LessThanOrEqual<T>(override val column: String, override val value: T) : SQLOperator<T> {
        override val symbol: String = "<="
    }

    /** Represents a "LIKE" operation. */
    data class Like(override val column: String, override val value: String) : SQLOperator<String> {
        override val symbol: String = "LIKE"
        override fun toSQLString(): String {
            return "$column $symbol '%$value%'"
        }
    }

    /** Represents an "IN" operation. */
    data class In<T>(override val column: String, override val value: List<T>) : SQLOperator<List<T>> {
        override val symbol: String = "IN"
        override fun toSQLString(): String {
            val list = value.joinToString(", ") { if (it is String) "'$it'" else it.toString() }
            return "$column $symbol ($list)"
        }
    }

    /** Represents a "NOT IN" operation. */
    data class NotIn<T>(override val column: String, override val value: List<T>) : SQLOperator<List<T>> {
        override val symbol: String = "NOT IN"
        override fun toSQLString(): String {
            val list = value.joinToString(", ") { if (it is String) "'$it'" else it.toString() }
            return "$column $symbol ($list)"
        }
    }

    /** Represents an "IS NULL" operation. */
    data class IsNull(override val column: String) : SQLOperator<Unit> {
        override val symbol: String = "IS NULL"
        override val value: Unit = Unit
        override fun toSQLString(): String = "$column $symbol"
        override fun asString(): String = "$column $symbol"
    }

    /** Represents an "IS NOT NULL" operation. */
    data class IsNotNull(override val column: String) : SQLOperator<Unit> {
        override val symbol: String = "IS NOT NULL"
        override val value: Unit = Unit
        override fun toSQLString(): String = "$column $symbol"
        override fun asString(): String = "$column $symbol"
    }

    /** Represents a "BETWEEN" operation. */
    data class Between<T>(override val column: String, val start: T, val end: T) : SQLOperator<Pair<T, T>> {
        override val symbol: String = "BETWEEN"
        override val value: Pair<T, T> = start to end
        override fun toSQLString(): String {
            val startStr = if (start is String) "'$start'" else start.toString()
            val endStr = if (end is String) "'$end'" else end.toString()
            return "$column $symbol $startStr AND $endStr"
        }
        override fun asString(): String = "$column $symbol $start AND $end"
    }
}
