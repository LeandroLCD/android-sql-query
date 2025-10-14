package com.blipblipcode.query

/**
 * Represents an object that can be converted to a SQL query string.
 */
interface Queryable {
    /**
     * Returns the SQL query string representation of the object.
     * @return The SQL query string.
     */
    fun asSql(): String
}
