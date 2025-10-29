package com.blipblipcode.query.operator

/**
 * Defines the types of logical operations that can be used in a SQL WHERE clause.
 */
enum class LogicalType(val sql: String) {
    /** Represents a logical AND operation. */
    AND("AND"),
    /** Represents a logical OR operation. */
    OR("OR"),
    /** Represents a SQL LIKE operation. */
    LIKE("LIKE"),
    /** Represents a SQL ALL operation. */
    ALL("ALL"),

    /** Represents a SQL AND NOT operation. */
    AND_NOT("AND NOT"),
    /** Represents a SQL EXISTS operation. */
    EXISTS("EXISTS"),
    /** Represents a SQL NOT operation. */
    NOT("NOT")
}