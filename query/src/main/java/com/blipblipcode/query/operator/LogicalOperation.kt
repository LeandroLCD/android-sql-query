package com.blipblipcode.query.operator

import kotlin.collections.forEachIndexed


/**
 * Represents a logical operation in a SQL query, combining a [LogicalType] with a [SQLOperator].
 * For example, "AND age > 18".
 *
 * @property symbol The type of the logical operation (e.g., AND, OR).
 * @property operator The SQL operator that is part of the logical operation.
 */
sealed interface LogicalOperation{
    val symbol: String
    val operator: SQLOperator<*>

    data class Where(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.WHERE.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }

        override fun clone(vararg arg: Any?): LogicalOperation {
            return Where(arg[0] as SQLOperator<*>)
        }
    }
    data class And(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.AND.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }

        override fun clone(vararg arg: Any?): LogicalOperation {
           return And(arg[0] as SQLOperator<*>)
        }
    }
    data class Or(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.OR.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }

        override fun clone(vararg arg: Any?): LogicalOperation {
            return Or(arg[0] as SQLOperator<*>)
        }
    }
    data class AndNot(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.AND_NOT.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }
        override fun clone(vararg arg: Any?): LogicalOperation {
            return And(arg[0] as SQLOperator<*>)
        }
    }
    data class Exists(override val operator: SQLOperator<*>) : LogicalOperation{
        override val symbol: String = LogicalType.EXISTS.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }
        override fun clone(vararg arg: Any?): LogicalOperation {
            return Exists(arg[0] as SQLOperator<*>)
        }
    }
    data class Not(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.NOT.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }
        override fun clone(vararg arg: Any?): LogicalOperation {
            return Not(arg[0] as SQLOperator<*>)
        }
    }
    data class All(override val operator: SQLOperator<*>) : LogicalOperation {
        override val symbol: String = LogicalType.ALL.sql
        override fun asString(): String {
            return "$symbol ${operator.toSQLString()}"
        }
        override fun clone(vararg arg: Any?): LogicalOperation {
            return All(arg[0] as SQLOperator<*>)
        }
    }
    @Suppress("UNCHECKED_CAST")
    data class Multiple(val operations: List<LogicalOperation>, override val symbol: String = "AND") : LogicalOperation {
        override val operator: SQLOperator<*>
            get() = operations.first().operator

        override fun asString(): String {
            return buildString {
                append("$symbol (")
                operations.forEachIndexed { index, logicalOperation ->
                    if (index == 0) {
                        append(logicalOperation.operator.asString())
                    } else {
                        append(" ${logicalOperation.asString()}")
                    }
                }
                append(")")

            }
        }
        override fun clone(vararg arg: Any?): LogicalOperation {
            return Multiple(arg[0] as List<LogicalOperation>, symbol = arg[1] as? String ?: symbol)
        }
    }

    /**
     * Converts the logical operation into its SQL string representation.
     * @return The SQL string for the logical operation (e.g., "AND age > '18'").
     */
    fun asString(): String

    fun clone(vararg arg: Any?): LogicalOperation
}
