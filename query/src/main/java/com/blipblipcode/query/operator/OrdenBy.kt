package com.blipblipcode.query.operator

sealed interface OrderBy : SQLOperator<String> {
    override val column: String
    override val symbol: String
    override val value: String
    override val caseConversion: CaseConversion
        get() = CaseConversion.NONE

    val transform: (String) -> String
    val collation: Collation
    override fun asString(): String

    fun asSqlClause(): String {
        return when (this) {
            is Asc, is Desc -> collation.apply(transform(column)) + " " + value
            is Multiple -> orders.joinToString(", ") { it.asSqlClause() }
        }
    }

    fun clone(vararg params: Any?): OrderBy

    data class Asc(
        override val column: String,
        override val collation: Collation = Collation.NONE,
        override val transform: (String) -> String = { it }
    ) : OrderBy {
        override val symbol: String = "ORDER BY"
        override val value: String = "ASC"

        override fun asString(): String {
            val expr = collation.apply(transform(column))
            return "$symbol $expr $value"
        }

        override fun clone(vararg params: Any?): OrderBy {
            val newColumn = params.getOrNull(0) as? String ?: column
            val newCollation = params.getOrNull(1) as? Collation ?: collation
            @Suppress("UNCHECKED_CAST")
            val newTransform = params.getOrNull(2) as? (String) -> String ?: transform
            return this.copy(column = newColumn, transform = newTransform, collation = newCollation)
        }

        override fun toString(): String = asString()
    }

    data class Desc(
        override val column: String,
        override val collation: Collation = Collation.NONE,
        override val transform: (String) -> String = { it }
    ) : OrderBy {
        override val symbol: String = "ORDER BY"
        override val value: String = "DESC"

        override fun asString(): String {
            val expr = collation.apply(transform(column))
            return "$symbol $expr $value"
        }

        override fun clone(vararg params: Any?): OrderBy {
            val newColumn = params.getOrNull(0) as? String ?: column
            val newCollation = params.getOrNull(1) as? Collation ?: collation
            @Suppress("UNCHECKED_CAST")
            val newTransform = params.getOrNull(2) as? (String) -> String ?: transform
            return this.copy(column = newColumn, transform = newTransform, collation = newCollation)
        }

        override fun toString(): String = asString()
    }

    data class Multiple(val orders: List<OrderBy>) : OrderBy {
        override val column: String
            get() = orders.joinToString(", ") { it.column }
        override val symbol: String = "ORDER BY"
        override val value: String = orders.joinToString(", ") { it.value }
        override val collation: Collation = Collation.NONE
        override val transform: (String) -> String = { it }

        override fun asString(): String {
            return "ORDER BY ${asSqlClause()}"
        }

        override fun clone(vararg params: Any?): OrderBy {
            val newOrders = params.getOrNull(0) as? List<*>
                ?: return this.copy()

            if (newOrders.all { it is OrderBy }) {
                @Suppress("UNCHECKED_CAST")
                return this.copy(orders = newOrders as List<OrderBy>)
            }
            throw IllegalArgumentException("The parameters provided for cloning are not of the List type<OrderBy>.")
        }

        override fun toString(): String = "ORDER BY ${asSqlClause()}"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Multiple) return false
            return orders == other.orders
        }

        override fun hashCode(): Int = orders.hashCode()
    }

}