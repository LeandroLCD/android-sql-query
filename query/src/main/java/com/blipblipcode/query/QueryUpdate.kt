package com.blipblipcode.query

import com.blipblipcode.query.operator.Field
import com.blipblipcode.query.operator.SQLOperator

/**
 * Represents a SQL UPDATE statement.
 * This class is used to construct and execute an UPDATE query on a specified table.
 * It is constructed using the associated `QueryBuilder`.
 *
 * @property table The name of the table to update.
 * @property where The main WHERE clause of the query.
 * @property fields The map of fields (columns) and their corresponding values to be updated.
 */
class QueryUpdate private constructor(
    private val table: String,
    private var where: SQLOperator<*>,
    private val fields: LinkedHashMap<String, Field<*>>
) : Queryable {

    companion object {
        /**
         * Creates a new `QueryBuilder` instance for constructing a `QueryUpdate` object.
         * @param table The name of the table to update.
         * @return A new `QueryBuilder` instance.
         */
        fun builder(
            table: String
        ): QueryBuilder {
            return QueryBuilder(table, LinkedHashMap())
        }
    }

    /**
     * Removes a field from the update query by its key.
     * @param key The name of the column to remove.
     * @return The current `QueryUpdate` instance for chaining.
     */
    fun remove(key: String): QueryUpdate {
        fields.remove(key)
        return this
    }

    /**
     * Sets or replaces the main WHERE clause of the query.
     * @param operator The new SQL operator for the WHERE clause.
     * @return The current `QueryUpdate` instance for chaining.
     */
    fun setWhere(operator: SQLOperator<*>): QueryUpdate {
        where = operator
        return this
    }

    /**
     * Sets a new value for a specific field (column).
     * @param key The name of the column.
     * @param value The new value for the column.
     * @return The current `QueryUpdate` instance for chaining.
     */
    fun set(key: String, value: Any): QueryUpdate {
        require(key.isNotBlank()) { "Field name cannot be blank." }
        fields[key] = Field(key, value)
        return this
    }

    /**
     * Sets a new value for a specific field (column).
     * @param field The `Field` object containing the column name and new value.
     * @return The current `QueryUpdate` instance for chaining.
     */
    fun set(field: Field<*>): QueryUpdate {
        require(field.name.isNotBlank()) { "Field name cannot be blank." }
        fields[field.name] = field
        return this
    }

    override fun getSqlOperators(): List<SQLOperator<*>> {
        return emptyList()
    }

    override fun getTableName(): String {
        return table
    }

    override fun getSqlOperation(key: String): SQLOperator<*>? {
        return null
    }

    /**
     * Generates the SQL string for the UPDATE statement.
     * @return The complete UPDATE SQL query as a string.
     */
    override fun asSql(): String {
        val setClause = fields.values.joinToString(", ") { it.asString() }
        return "UPDATE $table SET $setClause WHERE ${where.toSQLString()}".trim()
    }

    /**
     * A builder for creating `QueryUpdate` instances.
     * This class provides a fluent API to construct an UPDATE query.
     */
    class QueryBuilder internal constructor(
        private val table: String,
        private val fields: LinkedHashMap<String, Field<*>>
    ) {
        private var where: SQLOperator<*>? = null

        /**
         * Adds a field to be updated.
         * @param key The name of the column.
         * @param value The new value for the column.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun set(key: String, value: Any): QueryBuilder {
            require(key.isNotBlank()) { "Field name cannot be blank." }
            fields[key] = Field(key, value)
            return this
        }

        /**
         * Adds a field to be updated.
         * @param field The `Field` object to add to the SET clause.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun set(field: Field<*>): QueryBuilder {
            require(field.name.isNotBlank()) { "Field name cannot be blank." }
            fields[field.name] = field
            return this
        }


        /**
         * Removes a field from the update list by its key.
         * @param key The name of the column to remove.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun remove(key: String): QueryBuilder {
            fields.remove(key)
            return this
        }

        /**
         * Sets the WHERE clause for the query.
         * @param operator The SQL operator for the WHERE clause.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun where(operator: SQLOperator<*>): QueryBuilder {
            where = operator
            return this
        }

        /**
         * Builds the `QueryUpdate` instance.
         * @return A new `QueryUpdate` object.
         * @throws IllegalArgumentException if the WHERE clause is not set or if no fields are provided for updating.
         */
        fun build(): QueryUpdate {
            require(where != null) { "A WHERE clause must be added." }
            require(fields.isNotEmpty()) { "At least one field must be provided to update." }
            return QueryUpdate(
                where = where!!, table = table, fields = fields
            )
        }
    }
}
