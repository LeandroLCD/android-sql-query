package com.blipblipcode.query.operator

/**
 * Defines the types of logical operations that can be used in a SQL WHERE clause.
 */
enum class LogicalType(val sql: String) {
    /** Represents a logical AND operation. */
    AND("AND"),
    /** Represents a logical OR operation. */
    OR("OR"),
    /** Represents a SQL LIKE operation. */
    LIKE("LIKE"),
    /** Represents a SQL ALL operation. */
    ALL("ALL"),

    /** Represents a SQL AND NOT operation. */
    AND_NOT("AND NOT"),
    /** Represents a SQL EXISTS operation. */
    EXISTS("EXISTS"),
    /** Represents a SQL NOT operation. */
    NOT("NOT")
}

/**
 * Represents a logical operation in a SQL query, combining a [LogicalType] with a [SQLOperator].
 * For example, "AND age > 18".
 *
 * @property type The type of the logical operation (e.g., AND, OR).
 * @property operator The SQL operator that is part of the logical operation.
 */
class LogicalOperation(
    val type: LogicalType,
    val operator: SQLOperator<*>
) {
    /**
     * Converts the logical operation into its SQL string representation.
     * @return The SQL string for the logical operation (e.g., "AND age > '18'").
     */
    fun asString(): String = "${type.sql} ${operator.toSQLString()}"
}
