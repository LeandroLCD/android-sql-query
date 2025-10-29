package com.blipblipcode.query.operator



/**
 * Represents a logical operation in a SQL query, combining a [LogicalType] with a [SQLOperator].
 * For example, "AND age > 18".
 *
 * @property type The type of the logical operation (e.g., AND, OR).
 * @property operator The SQL operator that is part of the logical operation.
 */
data class LogicalOperation(
    val type: LogicalType,
    val operator: SQLOperator<*>
) {
    /**
     * Converts the logical operation into its SQL string representation.
     * @return The SQL string for the logical operation (e.g., "AND age > '18'").
     */
    fun asString(): String = "${type.sql} ${operator.toSQLString()}"
}
