package com.blipblipcode.query

import com.blipblipcode.query.operator.Field

/**
 * Represents a SQL INSERT statement.
 * This class is used to construct and execute an INSERT query on a specified table.
 * It is constructed using the associated `QueryBuilder`.
 *
 * @property table The name of the table to insert data into.
 * @property fields The map of fields (columns) and their corresponding values to be inserted.
 */
class QueryInsert private constructor(
    private val table: String,
    private val fields: LinkedHashMap<String, Field<*>>
) : Queryable {
    companion object {
        /**
         * Creates a new `QueryBuilder` instance for constructing a `QueryInsert` object.
         * @param table The name of the table to insert data into.
         * @return A new `QueryBuilder` instance.
         */
        fun builder(table: String): QueryBuilder {
            return QueryBuilder(table, LinkedHashMap())
        }
    }

    /**
     * Adds a new field and its value to the insert query.
     * @param key The name of the column.
     * @param value The value to be inserted.
     * @return The current `QueryInsert` instance for chaining.
     */
    fun add(key: String, value: Any): QueryInsert {
        require(key.isNotBlank()) { "Field name cannot be blank." }
        fields[key] = Field(key, value)
        return this
    }

    /**
     * Adds a new field to the insert query.
     * @param field The `Field` object to add.
     * @return The current `QueryInsert` instance for chaining.
     */
    fun add(field: Field<*>): QueryInsert {
        require(field.name.isNotBlank()) { "Field name cannot be blank." }
        fields[field.name] = field
        return this
    }

    /**
     * Removes a field from the insert query by its key.
     * @param key The name of the column to remove.
     * @return The current `QueryInsert` instance for chaining.
     */
    fun remove(key: String): QueryInsert {
        fields.remove(key)
        return this
    }

    /**
     * Generates the SQL string for the INSERT statement.
     * @return The complete INSERT SQL query as a string.
     * @throws IllegalArgumentException if no fields are provided for insertion.
     */
    override fun asSql(): String {
        require(fields.isNotEmpty()) { "At least one field must be provided for insertion." }
        val columns = fields.values.joinToString(", ") { it.name }
        val values = fields.values.joinToString(", ") { it.valueString() }
        return "INSERT INTO $table ($columns) VALUES ($values)"
    }

    /**
     * A builder for creating `QueryInsert` instances.
     * This class provides a fluent API to construct an INSERT query.
     */
    class QueryBuilder internal constructor(
        private val table: String,
        private val fields: LinkedHashMap<String, Field<*>>
    ) {
        /**
         * Adds a new field and its value to the insert query.
         * @param key The name of the column.
         * @param value The value to be inserted.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun add(key: String, value: Any?): QueryBuilder {
            require(key.isNotBlank()) { "Field name cannot be blank." }
            fields[key] = Field(key, value)
            return this
        }
        /**
         * Adds a new field to the insert query.
         * @param field The `Field` object to add.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun add(field: Field<*>): QueryBuilder {
            require(field.name.isNotBlank()) { "Field name cannot be blank." }
            fields[field.name] = field
            return this
        }
        /**
         * Removes a field from the insert query by its key.
         * @param key The name of the column to remove.
         * @return The `QueryBuilder` instance for chaining.
         */
        fun remove(key: String): QueryBuilder {
            fields.remove(key)
            return this
        }
        /**
         * Builds the `QueryInsert` instance.
         * @return A new `QueryInsert` object.
         * @throws IllegalArgumentException if no fields are provided for insertion.
         */
        fun build(): QueryInsert {
            require(fields.isNotEmpty()) { "At least one field must be provided for insertion." }
            return QueryInsert(table, fields)
        }
    }
}
