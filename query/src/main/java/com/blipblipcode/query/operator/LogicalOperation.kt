package com.blipblipcode.query.operator

/**
 * Defines the types of logical operations that can be used in a SQL WHERE clause.
 */
enum class LogicalType {
    /** Represents a logical AND operation. */
    AND,
    /** Represents a logical OR operation. */
    OR,
    /** Represents a SQL LIKE operation. */
    LIKE,
    /** Represents a SQL ALL operation. */
    ALL
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
    fun asString(): String = "${type.name} ${operator.toSQLString()}"
}
