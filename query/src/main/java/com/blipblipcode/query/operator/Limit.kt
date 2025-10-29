package com.blipblipcode.query.operator

/**
 * Represents a SQL LIMIT clause.
 * This class is used to limit the number of rows returned by a SELECT query.
 */
data class Limit(
    val count: Int,
    val offset: Int? = null
) {

    /**
     * Returns the SQL string representation of the LIMIT clause.
     * @return The LIMIT clause as a SQL string.
     */
    fun asString(): String {
        return if (offset != null && offset != 0) {
            "LIMIT $count OFFSET $offset"
        } else {
            "LIMIT $count"
        }
    }

    override fun toString(): String {
        return asString()
    }
}
