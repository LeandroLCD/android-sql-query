package com.blipblipcode.query.operator

/**
 * Represents a SQL LIMIT clause.
 * This class is used to limit the number of rows returned by a SELECT query.
 */
data class Limit(
    val count: Int,
    val offset: Int? = null,
): SQLOperator<Int>{

    override val symbol: String = "LIMIT"
    override val column: String = ""
    override val value: Int = count
    override val caseConversion: CaseConversion = CaseConversion.NONE

    /**
     * Returns the SQL string representation of the LIMIT clause.
     * @return The LIMIT clause as a SQL string.
     */
    override fun asString(): String {
        return if (offset != null && offset != 0) {
            "$symbol $count OFFSET $offset"
        } else {
            "$symbol $count"
        }
    }

    override fun toString(): String {
        return asString()
    }
}
