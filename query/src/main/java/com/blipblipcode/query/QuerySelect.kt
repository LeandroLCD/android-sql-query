package com.blipblipcode.query

import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalType
import com.blipblipcode.query.operator.SQLOperator

/**
 * Represents a SQL SELECT statement.
 * This class is used to construct and execute a SELECT query on a specified table.
 * It is constructed using the associated `QueryBuilder`.
 *
 * @property where The main WHERE clause of the query.
 * @property table The name of the table to select from.
 * @property operations A map of logical operations (AND, OR, etc.) to be appended to the WHERE clause.
 * @property fields The list of columns to be returned in the result set. Defaults to "*".
 */
class QuerySelect private constructor(
    private var where: SQLOperator<*>,
    private val table: String,
    private val operations: LinkedHashMap<String, LogicalOperation>,
    private val fields: List<String>
) : Queryable {

    companion object {
        /**
         * Creates a new `QueryBuilder` instance for constructing a `QuerySelect` object.
         * @param table The name of the table to select from.
         * @return A new `QueryBuilder` instance.
         */
        fun builder(
            table: String
        ): QueryBuilder {
            return QueryBuilder(table, LinkedHashMap())
        }
    }

    /**
     * Removes a logical operation by its key.
     * @param key The key of the logical operation to remove.
     * @return The current `QuerySelect` instance for chaining.
     */
    fun remove(key: String): QuerySelect {
        operations.remove(key)
        return this
    }

    /**
     * Sets or replaces the main WHERE clause of the query.
     * @param operator The new SQL operator for the WHERE clause.
     * @return The current `QuerySelect` instance for chaining.
     */
    fun setWhere(operator: SQLOperator<*>): QuerySelect {
        where = operator
        return this
    }

    /**
     * Adds a logical operation to the WHERE clause.
     * @param key A unique key for the logical operation.
     * @param operation The logical operation to add.
     * @return The current `QuerySelect` instance for chaining.
     */
    fun addLogicalOperation(key: String, operation: LogicalOperation): QuerySelect {
        operations[key] = operation
        return this
    }

    /**
     * Returns a new `QuerySelect` instance with the specified fields.
     * @param newFields The names of the columns to select. If empty, selects all columns ("*").
     * @return A new `QuerySelect` instance with the updated fields.
     */
    fun setFields(vararg newFields: String): QuerySelect {
        val fieldList = if (newFields.isEmpty()) listOf("*") else newFields.toList()
        return QuerySelect(
            table = table,
            where = where,
            operations = operations,
            fields = fieldList
        )
    }

    /**
     * Generates the SQL string for the SELECT statement.
     * @return The complete SELECT SQL query as a string.
     */
    override fun asSql(): String {
        val fieldStr = if (fields.isEmpty()) "*" else fields.joinToString(", ")
        val operationsStr = if (operations.isNotEmpty()) operations.values.joinToString(" ") { it.asString() } else ""
        return "SELECT $fieldStr FROM $table WHERE ${where.toSQLString()} $operationsStr".trim()
    }

    /**
     * A builder for creating `QuerySelect` instances.
     * This class provides a fluent API to construct a SELECT query.
     */
    class QueryBuilder internal constructor(
        private val table: String,
        private val operations: LinkedHashMap<String, LogicalOperation>
    ) {
        private var where: SQLOperator<*>? = null
        private var fields: List<String> = listOf("*" )

        /**
         * Adds an AND condition to the WHERE clause.
         * @param key A unique key for this condition.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun and(key: String, operator: SQLOperator<*>): QueryBuilder {
            operations[key] = LogicalOperation(LogicalType.AND, operator)
            return this
        }

        /**
         * Adds an OR condition to the WHERE clause.
         * @param key A unique key for this condition.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun or(key: String, operator: SQLOperator<*>): QueryBuilder {
            operations[key] = LogicalOperation(LogicalType.OR, operator)
            return this
        }

        /**
         * Adds a LIKE condition to the WHERE clause.
         * @param key A unique key for this condition.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun like(key: String, operator: SQLOperator<*>): QueryBuilder {
            operations[key] = LogicalOperation(LogicalType.LIKE, operator)
            return this
        }

        /**
         * Adds an ALL condition to the WHERE clause.
         * @param key A unique key for this condition.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun all(key: String, operator: SQLOperator<*>): QueryBuilder {
            operations[key] = LogicalOperation(LogicalType.ALL, operator)
            return this
        }

        /**
         * Removes a condition by its key.
         * @param key The key of the condition to remove.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun remove(key: String): QueryBuilder {
            operations.remove(key)
            return this
        }

        /**
         * Sets the main WHERE clause for the query.
         * @param operator The SQL operator for the WHERE clause.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun where(operator: SQLOperator<*>): QueryBuilder {
            where = operator
            return this
        }

        /**
         * Sets the fields (columns) to be returned by the query.
         * @param newFields The names of the columns. If empty or not called, defaults to all columns ("*").
         * @return The `QueryBuilder` instance for chaining.
         */
        fun setFields(vararg newFields: String): QueryBuilder {
            fields = if (newFields.isEmpty()) listOf("*") else newFields.toList()
            return this
        }

        /**
         * Builds the `QuerySelect` instance.
         * @return A new `QuerySelect` object.
         * @throws IllegalArgumentException if the WHERE clause is not set.
         */
        fun build(): QuerySelect {
            require(where != null) { "A WHERE clause must be specified." }
            return QuerySelect(
                where = where!!,
                table = table,
                operations = LinkedHashMap(operations),
                fields = fields
            )
        }
    }
}
