package com.blipblipcode.query

import com.blipblipcode.query.operator.Limit
import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.SQLOperator

/**
 * Represents a SQL UNION query construct.
 * This class is designed to build UNION and UNION ALL statements by combining multiple `QuerySelect` objects.
 * It is constructed using the associated `Builder`.
 *
 * @property queries The list of `QuerySelect` objects to be united.
 * @property useUnionAll Flag indicating whether to use UNION ALL (true) or UNION (false).
 */
class UnionQuery private constructor(
    val queries: List<QuerySelect>,
    val useUnionAll: Boolean = false
) : Queryable, Deletable {
    private var orderBy: OrderBy? = null
    override fun getSqlOperators(): List<SQLOperator<*>> {
        return queries.flatMap { it.getSqlOperators() }
    }

    override fun getTableName(): String {
        return queries.joinToString(", ") { it.getTableName() }
    }

    override fun getSqlOperation(key: String): SQLOperator<*>? {
        return queries.map { it.getSqlOperation(key) }.firstOrNull()
    }

    /**
     * Generates the SQL string for the UNION statement.
     *
     * If an ORDER BY has been explicitly set on this `UnionQuery` (via [orderBy]),
     * it will be used and any ORDER BY clauses from the individual queries will be ignored.
     * If no ORDER BY has been set on this `UnionQuery`, the ORDER BY clauses from the
     * individual queries will be collected and combined at the end of the UNION.
     *
     * @return The complete UNION SQL query as a string.
     * @throws IllegalArgumentException if less than two queries are provided.
     */
    override fun asSql(): String {
        require(queries.size >= 2) { "At least two queries are required for a UNION" }
        val orders = if (orderBy != null) {
            listOf(orderBy!!)
        } else {
            queries.mapNotNull { it.getOrderBy() }
        }

        val unionKeyword = if (useUnionAll) "UNION ALL" else "UNION"

        return buildString {
            append("SELECT * FROM (")
            appendLine()
            append(queries.joinToString("\n$unionKeyword\n") { it.asSql { op -> op !is OrderBy } })
            appendLine()
            append(")")
            if (orders.isNotEmpty()) {
                appendLine()
                append(OrderBy.Multiple(orders).asString())
            }
        }
    }
    /**
     * Generates the SQL string for the UNION statement.
     *
     * If an ORDER BY has been explicitly set on this `UnionQuery` (via [orderBy]),
     * it will be used (if it passes the predicate) and any ORDER BY clauses from the
     * individual queries will be ignored.
     * If no ORDER BY has been set on this `UnionQuery`, the ORDER BY clauses from the
     * individual queries that pass the predicate will be collected and combined at the end.
     *
     * @param predicate The predicate to filter the operators.
     * @return The complete UNION SQL query as a string.
     * @throws IllegalArgumentException if less than two queries are provided.
     */
    override fun asSql(predicate: (SQLOperator<*>) -> Boolean): String {
        require(queries.size >= 2) { "At least two queries are required for a UNION" }
        val orders = if (orderBy != null) {
            listOfNotNull(orderBy?.takeIf(predicate))
        } else {
            queries.mapNotNull { it.getOrderBy()?.takeIf(predicate) }
        }

        val unionKeyword = if (useUnionAll) "UNION ALL" else "UNION"

        return buildString {
            append("SELECT * FROM (")
            this.appendLine()
            append(queries.joinToString("\n$unionKeyword\n") { query ->
                query.asSql { op -> predicate(op) && op !is OrderBy }
            })
            this.appendLine()
            append(")")
            if (orders.isNotEmpty()) {
                appendLine()
                append(OrderBy.Multiple(orders).asString())
            }
        }
    }

    /**
     * Sets an ORDER BY clause for the entire UNION query.
     * When set, this ORDER BY takes precedence and any ORDER BY clauses
     * defined in the individual queries will be ignored.
     * If not set (null), the ORDER BY clauses from the individual queries
     * will be collected and combined at the end of the UNION statement.
     *
     * @param operator The `OrderBy` object specifying the column and direction for sorting.
     * @return This `UnionQuery` instance for chaining.
     */
    fun orderBy(operator: OrderBy): Queryable {
        orderBy = operator
        return this
    }
    /**
     * Retrieves the current ORDER BY clause for the INNER JOIN query.
     * @return The `OrderBy` object if set, otherwise null.
     */
    fun getOrderBy(): OrderBy? {
        return orderBy
    }

    fun clearOrderBy(): Queryable {
        orderBy = null
        return this
    }

    /**
     * Applies a LIMIT clause to all internal queries.
     *
     * @param count The maximum number of rows to return per query.
     * @param offset The number of rows to skip before returning results (optional).
     * @param override If true, replaces any existing LIMIT in each query.
     *                 If false (default), only applies the LIMIT to queries that do not already have one.
     * @return This `UnionQuery` instance for chaining.
     */
    fun limit(count: Int, offset: Int? = null, override: Boolean = false): UnionQuery {
        queries.forEach { query ->
            if (override || query.getLimit() == null) {
                query.limit(count, offset)
            }
        }
        return this
    }

    /**
     * Applies a LIMIT clause to all internal queries using a [Limit] object.
     *
     * @param limitOperator The [Limit] object specifying the limit parameters.
     * @param override If true, replaces any existing LIMIT in each query.
     *                 If false (default), only applies the LIMIT to queries that do not already have one.
     * @return This `UnionQuery` instance for chaining.
     */
    fun limit(limitOperator: Limit, override: Boolean = false): UnionQuery {
        queries.forEach { query ->
            if (override || query.getLimit() == null) {
                query.limit(limitOperator)
            }
        }
        return this
    }

    /**
     * Removes the LIMIT clause from all internal queries.
     *
     * @return This `UnionQuery` instance for chaining.
     */
    fun clearLimit(): UnionQuery {
        queries.forEach { it.clearLimit() }
        return this
    }
    /**
     * Creates a new `QueryBuilder` initialized with the current state of this `UnionQuery`.
     * This allows for further modifications or additions to the existing union query.
     *
     * @param consumer A lambda that receives the `QueryBuilder` for further configuration.
     * @return A new `QueryBuilder` instance initialized with the current queries and union type.
     */
    fun newBuilder(consumer:(QueryBuilder)-> Unit): QueryBuilder {
        val builder = QueryBuilder()
        builder.addQueries(queries.map { it.copy() })
        if(useUnionAll){
            builder.unionAll()
        }else{
            builder.union()
        }
        consumer(builder)
        return builder
    }

    fun copy(): UnionQuery {
        return newBuilder {  }.build()
    }

    /**
     * Converts this [UnionQuery] into a [QueryDelete] is **not supported** without extra
     * context, because a UNION spans multiple tables and the resulting DELETE needs an
     * explicit target table and a join column.
     *
     * Use [toQueryDelete] with a `targetTable` and a `keyColumn` instead.
     *
     * @throws IllegalArgumentException always.
     */
    override fun toQueryDelete(): QueryDelete {
        throw IllegalArgumentException(
            "UnionQuery cannot be converted to QueryDelete without a target table and a " +
                "key column. Call toQueryDelete(targetTable, keyColumn) instead."
        )
    }

    /**
     * Converts this [UnionQuery] into a [QueryDelete] that uses the union as a sub-query.
     *
     * The generated SQL follows the pattern:
     * ```
     * DELETE FROM <targetTable> WHERE <keyColumn> IN (
     *     SELECT <keyColumn> FROM (
     *         <union_query>
     *     )
     * )
     * ```
     *
     * `ORDER BY` / `LIMIT` defined on the inner queries are kept untouched: they only shape
     * the sub-query, not the outer DELETE.
     *
     * @param targetTable The name of the table to delete rows from.
     * @param keyColumn   The column used to match rows between `targetTable` and the union
     *                    result. The same column must exist (and be selectable) on every
     *                    inner query.
     * @return A [QueryDelete] that deletes from `targetTable` matching the union result.
     * @throws IllegalArgumentException if this [UnionQuery] contains fewer than two queries.
     */
    fun toQueryDelete(targetTable: String, keyColumn: String): QueryDelete {
        require(queries.size >= 2) { "At least two queries are required for a UNION" }
        val subQuery = asSql()
        return QueryDelete.builder(targetTable)
            .where(SQLOperator.InSubquery(column = keyColumn, value = subQuery))
            .build()
    }

    fun toQueriesDelete(): List<QueryDelete> {
        return queries.map { it.toQueryDelete() }
    }
    /**
     * A builder for creating `UnionQuery` instances.
     * This class provides a fluent API to construct a UNION query.
     */
    class QueryBuilder {
        private val queries = mutableListOf<QuerySelect>()
        private var useUnionAll = false


        /**
         * Transforms an existing logical operation by its key.
         * @param key The key of the logical operation to transform.
         * @param transform A lambda that takes the existing LogicalOperation and returns a new one.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun transformOperation(key:String, transform: (LogicalOperation) -> LogicalOperation): QueryBuilder {
            queries.forEachIndexed { index, querySelect ->
                val newQuery = querySelect.newBuilder { qb ->
                    qb.transformOperation(key, transform)
                }.build()
                queries[index] = newQuery
            }
            return this
        }

        /**
         * Retrieves the current SQL operator for a given key.
         * @param key The key of the SQL operator to retrieve.
         * @return The `SQLOperator` if found, otherwise null.
         */
        fun getSqlOperation(key: String): SQLOperator<*>? {
            return queries.map { it.getSqlOperation(key) }.firstOrNull()
        }

        /**
         * Adds a logical operation to all queries in the union.
         * @param key The key for the logical operation.
         * @param operation The `LogicalOperation` to add.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun addLogicalOperation(key: String, operation: LogicalOperation):QueryBuilder{
            queries.forEach {
                it.addLogicalOperation(key, operation)
            }
            return this
        }

        /**
         * Adds a query to the union.
         * @param query The `QuerySelect` to add.
         * @return The `Builder` instance for chaining.
         */
        fun addQuery(query: QuerySelect): QueryBuilder {
            queries.add(query)
            return this
        }

        /**
         * Adds multiple queries to the union.
         * @param queries The list of `QuerySelect` objects to add.
         * @return The `Builder` instance for chaining.
         */
        fun addQueries(queries: List<QuerySelect>): QueryBuilder {
            this.queries.addAll(queries)
            return this
        }

        /**
         * Sets the union type to UNION ALL.
         * @return The `Builder` instance for chaining.
         */
        fun unionAll(): QueryBuilder {
            useUnionAll = true
            return this
        }

        /**
         * Sets the union type to UNION (default).
         * @return The `Builder` instance for chaining.
         */
        fun union(): QueryBuilder {
            useUnionAll = false
            return this
        }

        /**
         * Builds the `UnionQuery` instance.
         * @return A new `UnionQuery` object.
         * @throws IllegalArgumentException if less than two queries have been added.
         */
        fun build(): UnionQuery {
            require(queries.size >= 2) { "At least two queries are required for a UNION" }
            return UnionQuery(queries.toList(), useUnionAll)
        }
    }

    companion object {
        /**
         * Creates a new `Builder` instance, initializing it with a base query.
         * @param baseQuery The first `QuerySelect` in the union.
         * @return A new `Builder` instance.
         */
        fun builder(baseQuery: QuerySelect): QueryBuilder {
            return QueryBuilder().also { builder ->
                builder.addQuery(baseQuery)
            }
        }

        /**
         * Creates a new `Builder` instance for UNION ALL, initializing it with a base query.
         * @param baseQuery The first `QuerySelect` in the union.
         * @return A new `Builder` instance configured for UNION ALL.
         */
        fun builderAll(baseQuery: QuerySelect): QueryBuilder {
            return QueryBuilder().also { builder ->
                builder.addQuery(baseQuery).unionAll()
            }
        }
    }
}
