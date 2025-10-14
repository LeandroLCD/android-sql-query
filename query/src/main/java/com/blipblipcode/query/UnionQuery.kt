package com.blipblipcode.query

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
) : Queryable {

    /**
     * Generates the SQL string for the UNION statement.
     * @return The complete UNION SQL query as a string.
     * @throws IllegalArgumentException if less than two queries are provided.
     */
    override fun asSql(): String {
        require(queries.size >= 2) { "At least two queries are required for a UNION" }

        val unionKeyword = if (useUnionAll) "UNION ALL" else "UNION"
        
        return queries.joinToString(" $unionKeyword ") { "(${it.asSql()})" }
    }

    /**
     * A builder for creating `UnionQuery` instances.
     * This class provides a fluent API to construct a UNION query.
     */
    class Builder {
        private val queries = mutableListOf<QuerySelect>()
        private var useUnionAll = false

        /**
         * Adds a query to the union.
         * @param query The `QuerySelect` to add.
         * @return The `Builder` instance for chaining.
         */
        fun addQuery(query: QuerySelect): Builder {
            queries.add(query)
            return this
        }

        /**
         * Adds multiple queries to the union.
         * @param queries The list of `QuerySelect` objects to add.
         * @return The `Builder` instance for chaining.
         */
        fun addQueries(queries: List<QuerySelect>): Builder {
            this.queries.addAll(queries)
            return this
        }

        /**
         * Sets the union type to UNION ALL.
         * @return The `Builder` instance for chaining.
         */
        fun unionAll(): Builder {
            useUnionAll = true
            return this
        }

        /**
         * Sets the union type to UNION (default).
         * @return The `Builder` instance for chaining.
         */
        fun union(): Builder {
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
        fun builder(baseQuery: QuerySelect): Builder {
            return Builder().also { builder ->
                builder.addQuery(baseQuery)
            }
        }

        /**
         * Creates a new `Builder` instance for UNION ALL, initializing it with a base query.
         * @param baseQuery The first `QuerySelect` in the union.
         * @return A new `Builder` instance configured for UNION ALL.
         */
        fun builderAll(baseQuery: QuerySelect): Builder {
            return Builder().also { builder ->
                builder.addQuery(baseQuery).unionAll()
            }
        }
    }
}
