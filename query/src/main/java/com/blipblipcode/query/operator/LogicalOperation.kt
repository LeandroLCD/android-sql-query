package com.blipblipcode.query.operator

import kotlin.collections.forEachIndexed


/**
 * Represents a logical operation in a SQL query, combining a [LogicalType] with a [SQLOperator].
 * For example, "AND age > 18".
 *
 * @property symbol The type of the logical operation (e.g., AND, OR).
 * @property operator The SQL operator that is part of the logical operation.
 */
sealed interface LogicalOperation: Copyable<LogicalOperation> {
    val symbol: String
    val operator: SQLOperator<*>

    data class Where(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.WHERE.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }

        override fun clone(): LogicalOperation {
            return Where(operator.clone())
        }
    }
    data class And(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.AND.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }

        override fun clone(): LogicalOperation {
           return And( operator.clone())
        }
    }
    data class Or(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.OR.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }

        override fun clone(): LogicalOperation {
            return Or(operator.clone())
        }
    }
    data class AndNot(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.AND_NOT.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }
        override fun clone(): LogicalOperation {
            return AndNot(operator.clone())
        }
    }
    data class Exists(override val operator: SQLOperator<*>) : LogicalOperation{
        override val symbol: String = LogicalType.EXISTS.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }
        override fun clone(): LogicalOperation {
            return Exists(operator.clone())
        }
    }
    data class Not(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.NOT.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }
        override fun clone(): LogicalOperation {
            return Not(operator.clone())
        }
    }
    data class All(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.ALL.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }
        override fun clone(): LogicalOperation {
            return All(operator.clone())
        }
    }
    /**
     * Groups several [LogicalOperation] into a single parenthesized unit so it can be appended to
     * a WHERE clause without its inner connectors leaking into the surrounding chain.
     *
     * [symbol] defines how the group attaches to the enclosing clause, while every grouped
     * operation keeps its own connector internally. For example,
     * `Multiple(listOf(Or(a), Or(b)), LogicalType.AND.sql)` renders as `AND (a OR b)`, so the ORs
     * cannot escape the preceding AND conditions the way a flat `joinToString(" ")` would allow.
     *
     * @property operations The grouped operations, at least one.
     * @property symbol The connector used to attach the group to the enclosing clause.
     * @throws IllegalArgumentException if [operations] is empty.
     */
    data class Multiple(
        val operations: List<LogicalOperation>,
        override val symbol: String = LogicalType.AND.sql
    ) : LogicalOperation {

        init {
            require(operations.isNotEmpty()) { "A logical group requires at least one operation" }
        }

        override val operator: SQLOperator<*>
            get() = operations.first().operator

        /**
         * Returns every operator contained in the group, in declaration order.
         * @return The operators of the grouped operations.
         */
        fun operators(): List<SQLOperator<*>> = operations.map { it.operator }

        override fun asString(): String {
            return buildString {
                append("$symbol (")
                operations.forEachIndexed { index, logicalOperation ->
                    when {
                        index == 0 && logicalOperation !is Multiple -> {
                            append(logicalOperation.operator.toSQLString())
                        }

                        index > 0 -> {
                            append(" ${logicalOperation.asString()}")
                        }

                        else -> {
                            append(logicalOperation.asString())
                        }
                    }
                }
                append(")")
            }
        }

        override fun clone(): LogicalOperation {
            return Multiple(operations.map { it.clone() }, symbol = symbol)
        }
    }

    /**
     * Converts the logical operation into its SQL string representation.
     * @return The SQL string for the logical operation (e.g., "AND age > '18'").
     */
    fun asString(): String



}
