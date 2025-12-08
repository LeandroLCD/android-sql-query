package com.blipblipcode.query.operator

sealed interface OrderBy {
    val column: String
    fun asString(): String

    fun asSqlClause(): String {
        return when (this) {
            is Asc -> "$column ASC"
            is Desc -> "$column DESC"
            is Multiple -> orders.joinToString(", ") { it.asSqlClause() }
        }
    }
    fun clone(vararg params: Any?): OrderBy

    data class Asc(override val column: String) : OrderBy{
        override fun asString(): String {
            return "ORDER BY $column ASC"
        }

        override fun clone(vararg params: Any?): OrderBy {
            return this.copy(params[0] as String)
        }

        override fun toString(): String {
            return "ORDER BY $column ASC"
        }
    }
    data class Desc(override val column: String) : OrderBy{
        override fun asString(): String {
            return "ORDER BY $column DESC"
        }

        override fun toString(): String {
            return "ORDER BY $column DESC"
        }

        override fun clone(vararg params: Any?): OrderBy {
            return this.copy(params[0] as String)
        }
    }

    data class Multiple(val orders: List<OrderBy>) : OrderBy{
        override val column: String
            get() = orders.joinToString(", ") { it.column }

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

        override fun toString(): String {
            return "ORDER BY ${asSqlClause()}"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Multiple) return false
            return orders == other.orders
        }

        override fun hashCode(): Int {
            return orders.hashCode()
        }
    }

}