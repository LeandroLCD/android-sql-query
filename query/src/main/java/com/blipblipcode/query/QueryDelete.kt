package com.blipblipcode.query

import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalType
import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.operator.logicalGroup

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
    private var where: Pair<String, LogicalOperation>?,
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
        where = operator.column to LogicalOperation.Where(operator)
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

    /**
     * Adds a parenthesized group of conditions attached to the WHERE clause with AND.
     * @param key A unique key for this condition.
     * @param conditions The logical operations to group, at least one.
     * @return The current `QueryDelete` instance for chaining.
     * @throws IllegalArgumentException if [conditions] is empty.
     */
    fun andGroup(key: String, vararg conditions: LogicalOperation): QueryDelete {
        operations[key] = logicalGroup(LogicalType.AND, conditions.toList())
        return this
    }

    /**
     * Adds a parenthesized group of conditions attached to the WHERE clause with AND, linking the
     * conditions inside the group with [connector].
     * @param key A unique key for this condition.
     * @param connector The connector used between the conditions inside the group.
     * @param conditions The conditions to group, at least one.
     * @return The current `QueryDelete` instance for chaining.
     * @throws IllegalArgumentException if [conditions] is empty.
     */
    fun andGroup(key: String, connector: LogicalType, vararg conditions: SQLOperator<*>): QueryDelete {
        operations[key] = logicalGroup(LogicalType.AND, connector, conditions)
        return this
    }

    /**
     * Adds a parenthesized group of conditions attached to the WHERE clause with OR.
     * @param key A unique key for this condition.
     * @param conditions The logical operations to group, at least one.
     * @return The current `QueryDelete` instance for chaining.
     * @throws IllegalArgumentException if [conditions] is empty.
     */
    fun orGroup(key: String, vararg conditions: LogicalOperation): QueryDelete {
        operations[key] = logicalGroup(LogicalType.OR, conditions.toList())
        return this
    }

    /**
     * Adds a parenthesized group of conditions attached to the WHERE clause with OR, linking the
     * conditions inside the group with [connector].
     * @param key A unique key for this condition.
     * @param connector The connector used between the conditions inside the group.
     * @param conditions The conditions to group, at least one.
     * @return The current `QueryDelete` instance for chaining.
     * @throws IllegalArgumentException if [conditions] is empty.
     */
    fun orGroup(key: String, connector: LogicalType, vararg conditions: SQLOperator<*>): QueryDelete {
        operations[key] = logicalGroup(LogicalType.OR, connector, conditions)
        return this
    }

    override fun getSqlOperators(): List<SQLOperator<*>> {
        return buildList {
            require(where != null) { "A WHERE clause must be specified." }
            addAll(where!!.second.sqlOperators())
            operations.values.forEach { addAll(it.sqlOperators()) }
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
        return "DELETE FROM $table ${where!!.second.asWhereString()} ${operations.values.joinToString(" ") { it.asString() }}".trim()
    }

    /**
     * Generates the SQL string for the DELETE statement.
     *
     * The predicate descends into grouped conditions, so a group keeps the conditions the predicate
     * accepts instead of being dropped as a whole. When the WHERE clause does not survive the
     * filter, the first surviving condition is promoted to the WHERE position so the statement
     * never starts with a dangling connector.
     *
     * @param predicate The predicate to filter the operators.
     * @return The complete DELETE SQL query as a string.
     * @throws IllegalArgumentException if the WHERE clause is not set.
     */
    override fun asSql(predicate: (SQLOperator<*>) -> Boolean): String {
        require(where != null) { "A WHERE clause must be specified." }
        val anchor = where!!.second.filterBy(predicate)
        val retained = operations.values.mapNotNull { it.filterBy(predicate) }
        val whereClause = anchor ?: retained.firstOrNull() ?: return "DELETE FROM $table"
        val trailing = if (anchor != null) retained else retained.drop(1)
        return "DELETE FROM $table ${whereClause.asWhereString()} ${trailing.joinToString(" ") { it.asString() }}".trim()
    }

    /**
     * A builder for creating `QueryDelete` instances.
     * This class provides a fluent API to construct a DELETE query.
     */
    class QueryBuilder internal constructor(
        private val table: String, private val operations: LinkedHashMap<String, LogicalOperation>
    ) {
        private var where: Pair<String, LogicalOperation>? = null

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
            operations[key] = LogicalOperation.And(operator)
            return this
        }
        /**
         * Adds an AND condition to the WHERE clause.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun and(operator: SQLOperator<*>): QueryBuilder {
            operations[operator.column] = LogicalOperation.And(operator)
            return this
        }

        /**
         * Adds an AND NOT logical operation to the query.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun andNot(operator: SQLOperator<*>): QueryBuilder {
            operations[operator.column] = LogicalOperation.And(operator)
            return this
        }

        /**
         * Adds an EXISTS logical operation to the query.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun exists(operator: SQLOperator<*>): QueryBuilder {
            operations[operator.column] = LogicalOperation.Exists( operator)
            return this
        }

        /**
         * Adds a NOT logical operation to the query.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun not(operator: SQLOperator<*>): QueryBuilder {
            operations[operator.column] = LogicalOperation.Not(operator)
            return this
        }

        /**
         * Adds an OR condition to the WHERE clause.
         * @param key A unique key for this condition.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun or(key: String, operator: SQLOperator<*>): QueryBuilder {
            operations[key] = LogicalOperation.Or(operator)
            return this
        }

        /**
         * Adds a LIKE condition to the WHERE clause.
         * @param key A unique key for this condition.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun like(key: String, operator: SQLOperator.Like): QueryBuilder {
            operations[key] = LogicalOperation.And( operator)
            return this
        }

        /**
         * Adds an ALL condition to the WHERE clause.
         * @param key A unique key for this condition.
         * @param operator The SQL operator for this condition.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun all(key: String, operator: SQLOperator<*>): QueryBuilder {
            operations[key] = LogicalOperation.All(operator)
            return this
        }

        /**
         * Adds a parenthesized group of conditions attached to the WHERE clause with AND.
         *
         * The group renders as `AND (<first> <second> ...)`, where each condition keeps its own
         * connector. Combining it with `LogicalOperation.Or` entries builds
         * `AND (a OR b OR c)` without the ORs escaping the preceding AND conditions.
         *
         * @param key A unique key for this condition.
         * @param conditions The logical operations to group, at least one.
         * @return The `QueryBuilder` instance for chaining.
         * @throws IllegalArgumentException if [conditions] is empty.
         */
        fun andGroup(key: String, vararg conditions: LogicalOperation): QueryBuilder {
            operations[key] = logicalGroup(LogicalType.AND, conditions.toList())
            return this
        }

        /**
         * Adds a parenthesized group of conditions attached to the WHERE clause with AND, linking
         * the conditions inside the group with [connector].
         *
         * @param key A unique key for this condition.
         * @param connector The connector used between the conditions inside the group.
         * @param conditions The conditions to group, at least one.
         * @return The `QueryBuilder` instance for chaining.
         * @throws IllegalArgumentException if [conditions] is empty.
         */
        fun andGroup(key: String, connector: LogicalType, vararg conditions: SQLOperator<*>): QueryBuilder {
            operations[key] = logicalGroup(LogicalType.AND, connector, conditions)
            return this
        }

        /**
         * Adds a parenthesized group of conditions attached to the WHERE clause with OR.
         *
         * The group renders as `OR (<first> <second> ...)`, where each condition keeps its own
         * connector. Combining it with `LogicalOperation.And` entries builds
         * `OR (a AND b AND c)` without the group leaking into the surrounding chain.
         *
         * @param key A unique key for this condition.
         * @param conditions The logical operations to group, at least one.
         * @return The `QueryBuilder` instance for chaining.
         * @throws IllegalArgumentException if [conditions] is empty.
         */
        fun orGroup(key: String, vararg conditions: LogicalOperation): QueryBuilder {
            operations[key] = logicalGroup(LogicalType.OR, conditions.toList())
            return this
        }

        /**
         * Adds a parenthesized group of conditions attached to the WHERE clause with OR, linking
         * the conditions inside the group with [connector].
         *
         * @param key A unique key for this condition.
         * @param connector The connector used between the conditions inside the group.
         * @param conditions The conditions to group, at least one.
         * @return The `QueryBuilder` instance for chaining.
         * @throws IllegalArgumentException if [conditions] is empty.
         */
        fun orGroup(key: String, connector: LogicalType, vararg conditions: SQLOperator<*>): QueryBuilder {
            operations[key] = logicalGroup(LogicalType.OR, connector, conditions)
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
            where = operator.column to LogicalOperation.Where(operator)
            return this
        }

        /**
         * Sets the main WHERE clause from an already built logical operation, preserving it as is.
         * Use this overload to carry over a grouped clause without unwrapping it into its first
         * operator.
         * @param key The key the clause is registered under.
         * @param operation The logical operation to use as the WHERE clause.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun where(key: String, operation: LogicalOperation): QueryBuilder {
            where = key to operation
            return this
        }

        /**
         * Adds a logical operation, overwriting any previous operation registered under [key].
         * @param key A unique key for the logical operation.
         * @param operation The logical operation to add.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun addLogicalOperation(key: String, operation: LogicalOperation): QueryBuilder {
            operations[key] = operation
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
