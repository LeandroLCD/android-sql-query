package com.blipblipcode.query

import com.blipblipcode.query.operator.SQLOperator

/**
 * Represents an object that can be converted to a SQL query string.
 */
interface Queryable {
    /**
     * Returns a list of all SQL operators used in the query.
     * @return A list of [SQLOperator] instances.
     */
    fun getSqlOperators(): List<SQLOperator<*>>
    /**
     * Returns the table name associated with the queryable object.
     * This is typically derived from the class name of the implementing object.
     * @return The table name as a string.
     */
    fun getTableName(): String


    /**
     * Returns the corresponding SQL operation string for a given [SQLOperator].
     *
     * @param key The [SQLOperator] enum constant.
     * @return The [SQLOperator] representation of the SQL operation or NULL if not exist.
     */
    fun getSqlOperation(key: String): SQLOperator<*>?

    /**
     * Returns the SQL query string representation of the object.
     * @return The SQL query string.
     */
    fun asSql(): String

    /**
     * Returns the SQL query string representation of the object.
     * @param predicate The predicate to filter the operators.
     * @return The SQL query string.
     */
    fun asSql(predicate: ( SQLOperator<*>) -> Boolean): String
}
