package com.blipblipcode.query

import com.blipblipcode.query.operator.Limit
import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalType
import com.blipblipcode.query.operator.OrderBy
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
    private var orderBy: OrderBy? = null
    private var limit: Limit? = null

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
        ).apply {
            this.orderBy = this@QuerySelect.orderBy
            this.limit = this@QuerySelect.limit
        }
    }

    override fun getSqlOperators(): List<SQLOperator<*>> {
        return buildList {
            add(where)
            operations.values.forEach { add(it.operator) }
        }
    }

    override fun getTableName(): String {
        return table
    }

    override fun getSqlOperation(key: String): SQLOperator<*>? {
        return operations.get(key)?.operator
    }

    /**
     * Generates the SQL string for the SELECT statement.
     * @return The complete SELECT SQL query as a string.
     */
    override fun asSql(): String {
        val fieldStr = if (fields.isEmpty()) "*" else fields.joinToString(", ")
        val operationsStr = if (operations.isNotEmpty()) operations.values.joinToString(" ") { it.asString() } else ""
        return buildString {
            append("SELECT $fieldStr FROM $table WHERE ${where.toSQLString()} $operationsStr".trim())
            if (orderBy != null) {
                append(" ")
                append(orderBy!!.asString())
            }
            if (limit != null) {
                append(" ")
                append(limit!!.asString())
            }
        }
    }

    /**
     * Appends an ORDER BY clause to the entire UNION query.
     * Note that in most SQL dialects, an ORDER BY clause can only be applied to the final result of a UNION, not to individual `SELECT` statements within it.
     *
     * @param operator A vararg of `[OrderBy]` objects specifying the columns and direction for sorting.
     * @return A new `QuerySelect` instance representing the UNION query with the added ORDER BY clause.
     */
    fun orderBy(operator: OrderBy): Queryable {
        orderBy = operator
        return this
    }

    /**
     * Adds a LIMIT clause to the query to limit the number of rows returned.
     *
     * @param count The maximum number of rows to return.
     * @param offset The number of rows to skip before returning results (optional).
     * @return The current `QuerySelect` instance for chaining.
     */
    fun limit(count: Int, offset: Int? = null): QuerySelect {
        limit = Limit(count, offset)
        return this
    }

    /**
     * Adds a LIMIT clause to the query using a Limit object.
     *
     * @param limitOperator The Limit object specifying the limit parameters.
     * @return The current `QuerySelect` instance for chaining.
     */
    fun limit(limitOperator: Limit): QuerySelect {
        limit = limitOperator
        return this
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
        private var fields: List<String> = listOf("*")
        private var orderBy: OrderBy? = null
        private var limit: Limit? = null

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
         * Sets the ORDER BY clause for the query.
         * @param orderBy The OrderBy object specifying the column and direction for sorting.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun orderBy(orderBy: OrderBy): QueryBuilder {
            this.orderBy = orderBy
            return this
        }

        /**
         * Sets a LIMIT clause for the query to limit the number of rows returned.
         * @param count The maximum number of rows to return.
         * @param offset The number of rows to skip before returning results (optional).
         * @return The `QueryBuilder` instance for chaining.
         */
        fun limit(count: Int, offset: Int? = null): QueryBuilder {
            this.limit = Limit(count, offset)
            return this
        }

        /**
         * Sets a LIMIT clause for the query using a Limit object.
         * @param limit The Limit object specifying the limit parameters.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun limit(limit: Limit): QueryBuilder {
            this.limit = limit
            return this
        }

        /**
         * Builds the `QuerySelect` instance.
         * @return A new `QuerySelect` object.
         * @throws IllegalArgumentException if the WHERE clause is not set.
         */
        fun build(): QuerySelect {
            if(where == null){
                val w = operations.firstNotNullOfOrNull{it}.let {
                    it ?: throw IllegalArgumentException("WHERE clause is required for QuerySelect")
                }
                operations.remove(w.key)

                where = w.value.operator
            }
            return QuerySelect(
                where = where!!,
                table = table,
                operations = LinkedHashMap(operations),
                fields = fields
            ).apply {
                this@apply.orderBy = this@QueryBuilder.orderBy
                this@apply.limit = this@QueryBuilder.limit
            }
        }
    }
}
