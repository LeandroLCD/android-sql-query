package com.blipblipcode.query

import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalType
import com.blipblipcode.query.operator.SQLOperator

/**
 * Represents a SQL DELETE statement.
 * This class is used to construct and execute a DELETE query on a specified table.
 * It is constructed using the associated `QueryBuilder`.
 *
 * @property where The main WHERE clause of the query.
 * @property table The name of the table to delete from.
 * @property operations A map of logical operations (AND, OR, etc.) to be appended to the WHERE clause.
 */
class QueryDelete private constructor(
    private var where: SQLOperator<*>?,
    private val table: String,
    private val operations: LinkedHashMap<String, LogicalOperation>,
):Queryable {

    companion object {
        /**
         * Creates a new `QueryBuilder` instance for constructing a `QueryDelete` object.
         * @param table The name of the table to delete from.
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
     * @return The current `QueryDelete` instance for chaining.
     */
    fun remove(key: String): QueryDelete {
        operations.remove(key)
        return this
    }

    fun clear() : QueryDelete {
        operations.clear()
        where = null
        return this
    }

    /**
     * Sets or replaces the main WHERE clause of the query.
     * @param operator The new SQL operator for the WHERE clause.
     * @return The current `QueryDelete` instance for chaining.
     */
    fun setWhere(operator: SQLOperator<*>): QueryDelete {
        where = operator
        return this
    }

    /**
     * Adds a logical operation to the WHERE clause.
     * @param key A unique key for the logical operation.
     * @param operation The logical operation to add.
     * @return The current `QueryDelete` instance for chaining.
     */
    fun addLogicalOperation(key: String, operation: LogicalOperation): QueryDelete {
        operations[key] = operation
        return this
    }

    override fun getSqlOperators(): List<SQLOperator<*>> {
        return buildList {
            require(where != null) { "A WHERE clause must be specified." }
            add(where!!)
            operations.values.forEach { add(it.operator) }
        }
    }

    override fun getTableName(): String {
        return table
    }

    override fun getSqlOperation(key: String): SQLOperator<*>? {
        return operations[key]?.operator
    }

    /**
     * Generates the SQL string for the DELETE statement.
     * @return The complete DELETE SQL query as a string.
     * @throws IllegalArgumentException if the WHERE clause is not set.
     */
    override fun asSql(): String {
        require(where != null) { "A WHERE clause must be specified." }
        return "DELETE FROM $table WHERE ${where!!.toSQLString()} ${operations.values.joinToString(" ") { it.asString() }}".trim()
    }

    /**
     * Generates the SQL string for the DELETE statement.
     * @param predicate The predicate to filter the operators.
     * @return The complete DELETE SQL query as a string.
     * @throws IllegalArgumentException if the WHERE clause is not set.
     */
    override fun asSql(predicate: (SQLOperator<*>) -> Boolean): String {
        require(where != null) { "A WHERE clause must be specified." }
        return "DELETE FROM $table WHERE ${where!!.toSQLString()} ${operations.values.filter { predicate(it.operator) }.joinToString(" ") { it.asString() }}".trim()
    }

    /**
     * A builder for creating `QueryDelete` instances.
     * This class provides a fluent API to construct a DELETE query.
     */
    class QueryBuilder internal constructor(
        private val table: String, private val operations: LinkedHashMap<String, LogicalOperation>
    ) {
        private var where: SQLOperator<*>? = null

        /**
         * Clears all conditions and resets the builder.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun clear() : QueryBuilder {
            operations.clear()
            where = null
            return this
        }

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
         * Adds an AND condition to the WHERE clause.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun and(operator: SQLOperator<*>): QueryBuilder {
            operations[operator.column] = LogicalOperation(LogicalType.AND, operator)
            return this
        }

        /**
         * Adds an AND NOT logical operation to the query.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun andNot(operator: SQLOperator<*>): QueryBuilder {
            operations[operator.column] = LogicalOperation(LogicalType.AND_NOT, operator)
            return this
        }

        /**
         * Adds an EXISTS logical operation to the query.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun exists(operator: SQLOperator<*>): QueryBuilder {
            operations[operator.column] = LogicalOperation(LogicalType.EXISTS, operator)
            return this
        }

        /**
         * Adds a NOT logical operation to the query.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun not(operator: SQLOperator<*>): QueryBuilder {
            operations[operator.column] = LogicalOperation(LogicalType.NOT, operator)
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
        fun like(key: String, operator: SQLOperator.Like): QueryBuilder {
            operations[key] = LogicalOperation(LogicalType.AND, operator)
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
         * Builds the `QueryDelete` instance.
         * @return A new `QueryDelete` object.
         * @throws IllegalArgumentException if the WHERE clause is not set.
         */
        fun build(): QueryDelete {
            require(where != null) { "A WHERE clause must be specified." }
            return QueryDelete(
                where = where!!, table = table, operations = operations
            )
        }
    }
}
