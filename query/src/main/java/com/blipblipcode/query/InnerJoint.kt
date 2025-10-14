package com.blipblipcode.query

/**
 * Represents a SQL INNER JOIN query construct.
 * This class is designed to build complex INNER JOIN statements by combining multiple `QuerySelect` objects.
 * It is constructed using the associated `Builder`.
 *
 * @property queries The list of `QuerySelect` objects to be joined. The first is the base query.
 * @property onClauses The list of 'ON' clauses for each join. The first element is a placeholder and is ignored.
 */
class InnerJoint private constructor(
    val queries: List<QuerySelect>,
    val onClauses: List<String>
) : Queryable {


    /**
     * Generates the SQL string for the INNER JOIN statement.
     * @return The complete INNER JOIN SQL query as a string.
     * @throws IllegalArgumentException if no queries are provided or if the number of queries does not match the number of on-clauses.
     */
    override fun asSql(): String {
        require(queries.isNotEmpty()) { "At least one query is required for an INNER JOIN" }
        require(queries.size == onClauses.size) { "The number of queries must be equal to the number of ON clauses (including a placeholder for the base query)" }

        val baseQuery = queries.first()
        val joins = queries.drop(1).zip(onClauses.drop(1)) { query, onClause ->
            "INNER JOIN (${query.asSql()}) ON $onClause"
        }

        return "(${baseQuery.asSql()}) ${joins.joinToString(" ")}"
    }

    /**
     * A builder for creating `InnerJoint` instances.
     * This class provides a fluent API to construct an INNER JOIN query.
     */
    class Builder {
        private val queries = mutableListOf<QuerySelect>()
        private val onClauses = mutableListOf<String>()

        /**
         * Adds a query to the join.
         * @param query The `QuerySelect` to add.
         * @param onClause The 'ON' clause for this join. For the first query, this is a placeholder and should be empty.
         * @return The `Builder` instance for chaining.
         */
        fun addJoin(query: QuerySelect, onClause: String): Builder {
            queries.add(query)
            onClauses.add(onClause)
            return this
        }
        /**
         * Adds multiple queries and their corresponding 'ON' clauses to the join.
         * @param queries The list of `QuerySelect` objects to add.
         * @param onClauses The list of 'ON' clauses.
         * @return The `Builder` instance for chaining.
         * @throws IllegalArgumentException if the number of queries does not match the number of 'ON' clauses.
         */
        fun addJoins(queries: List<QuerySelect>, onClauses: List<String>): Builder {
            require(queries.size == onClauses.size) { "The number of queries must be equal to the number of ON clauses" }
            this.queries.addAll(queries)
            this.onClauses.addAll(onClauses)
            return this
        }

        /**
         * Builds the `InnerJoint` instance.
         * @return A new `InnerJoint` object.
         * @throws IllegalArgumentException if no queries have been added.
         */
        fun build(): InnerJoint {
            require(queries.isNotEmpty()) { "At least one query is required for an INNER JOIN" }
            return InnerJoint(queries.toList(), onClauses.toList())
        }
    }

    companion object {
        /**
         * Creates a new `Builder` instance, initializing it with a base query.
         * A placeholder empty 'onClause' is added for the base query.
         * @param baseQuery The first `QuerySelect` in the join.
         * @return A new `Builder` instance.
         */
        fun builder(baseQuery: QuerySelect): Builder {
            return Builder().also { builder ->
                builder.addJoin(baseQuery, "")
            }
        }
    }
}
