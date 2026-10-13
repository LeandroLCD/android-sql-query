package com.blipblipcode.query.operator


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
     * @throws IllegalArgumentException if [operations] is empty or [symbol] is not a connector.
     */
    data class Multiple(
        val operations: List<LogicalOperation>,
        override val symbol: String = LogicalType.AND.sql
    ) : LogicalOperation {

        constructor(vararg operations: LogicalOperation) : this(operations.toList(), LogicalType.AND.sql)

        init {
            require(operations.isNotEmpty()) { "A logical group requires at least one operation" }
            require(symbol in GROUP_CONNECTORS) {
                "A logical group cannot use '$symbol' as connector, expected one of $GROUP_CONNECTORS"
            }
        }

        override val operator: SQLOperator<*>
            get() = operations.first().operator

        /**
         * Returns every operator contained in the group, including nested groups, in declaration
         * order.
         * @return The operators of the grouped operations, depth first.
         */
        fun operators(): List<SQLOperator<*>> = sqlOperators()

        override fun sqlOperators(): List<SQLOperator<*>> =
            operations.flatMap { it.sqlOperators() }

        override fun filterBy(predicate: (SQLOperator<*>) -> Boolean): LogicalOperation? {
            val retained = operations.mapNotNull { it.filterBy(predicate) }
            return if (retained.isEmpty()) null else Multiple(retained, symbol)
        }

        override fun asString(): String = "$symbol ${asConditionString()}"

        override fun asConditionString(): String = buildString {
            append("(")
            operations.forEachIndexed { index, logicalOperation ->
                when {
                    index == 0 -> {
                        append(logicalOperation.asConditionString())
                    }

                    else -> {
                        append(" ${logicalOperation.asString()}")
                    }
                }
            }
            append(")")
        }

        override fun clone(): LogicalOperation {
            return Multiple(operations.map { it.clone() }, symbol = symbol)
        }

        companion object {
            private val GROUP_CONNECTORS = listOf(LogicalType.AND, LogicalType.OR)
                .map { it.sql }
        }
    }

    /**
     * Converts the logical operation into its SQL string representation.
     * @return The SQL string for the logical operation (e.g., "AND age > '18'").
     */
    fun asString(): String

    /**
     * Converts the operation into its SQL fragment without the leading connector, so it can be
     * placed right after the `WHERE` keyword or after another condition.
     * @return The SQL string for the operation, e.g. "age > '18'" or "(a OR b)".
     */
    fun asConditionString(): String = operator.toSQLString()

    /**
     * Converts the logical operation into the SQL fragment anchored by the `WHERE` keyword, that
     * is, without the connector that would be dangling at the start of a clause.
     * @return The SQL string for the logical operation preceded by `WHERE`.
     */
    fun asWhereString(): String = "${LogicalType.WHERE.sql} ${asConditionString()}"

    /**
     * Returns every operator the operation holds, descending into nested groups.
     * @return The operators of this operation, in declaration order.
     */
    fun sqlOperators(): List<SQLOperator<*>> = listOf(operator)

    /**
     * Filters the operation by the given predicate, descending into nested groups.
     *
     * A group survives only if at least one of its conditions does, and the surviving conditions
     * are kept with their original connectors.
     *
     * @param predicate The predicate applied to every contained operator.
     * @return The filtered operation, or `null` when nothing is left.
     */
    fun filterBy(predicate: (SQLOperator<*>) -> Boolean): LogicalOperation? =
        if (predicate(operator)) this else null
}

/**
 * Wraps this operator into the [LogicalOperation] matching [type].
 * @param type The connector the condition must be linked with.
 * @return The logical operation holding this operator.
 * @throws IllegalArgumentException if [type] cannot link a condition.
 */
internal fun SQLOperator<*>.asLogicalOperation(type: LogicalType): LogicalOperation = when (type) {
    LogicalType.AND -> LogicalOperation.And(this)
    LogicalType.OR -> LogicalOperation.Or(this)
    LogicalType.AND_NOT -> LogicalOperation.AndNot(this)
    LogicalType.EXISTS -> LogicalOperation.Exists(this)
    LogicalType.NOT -> LogicalOperation.Not(this)
    LogicalType.ALL -> LogicalOperation.All(this)
    LogicalType.WHERE -> throw IllegalArgumentException(
        "WHERE cannot link a condition, use it as the query anchor instead"
    )
}

/**
 * Builds a [LogicalOperation.Multiple] attached to the enclosing clause with [outer].
 * @param outer The connector that attaches the group to the enclosing clause.
 * @param conditions The conditions to group, at least one.
 * @return The logical group.
 */
internal fun logicalGroup(
    outer: LogicalType,
    conditions: List<LogicalOperation>
): LogicalOperation.Multiple = LogicalOperation.Multiple(conditions, outer.sql)

/**
 * Builds a [LogicalOperation.Multiple] where every condition is linked with [inner] and the whole
 * group is attached to the enclosing clause with [outer].
 * @param outer The connector that attaches the group to the enclosing clause.
 * @param inner The connector used between the conditions inside the group.
 * @param conditions The conditions to group, at least one.
 * @return The logical group.
 * @throws IllegalArgumentException if [conditions] is empty or [inner] cannot link a condition.
 */
internal fun logicalGroup(
    outer: LogicalType,
    inner: LogicalType,
    conditions: Array<out SQLOperator<*>>
): LogicalOperation.Multiple = LogicalOperation.Multiple(
    conditions.map { it.asLogicalOperation(inner) },
    outer.sql
)