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
    data class Asc(override val column: String) : OrderBy{
        override fun asString(): String {
            return "ORDER BY $column ASC"
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
    }

    data class Multiple(val orders: List<OrderBy>) : OrderBy{
        override val column: String
            get() = orders.joinToString(", ") { it.column }

        override fun asString(): String {
            return "ORDER BY ${asSqlClause()}"
        }

        override fun toString(): String {
            return "ORDER BY ${asSqlClause()}"
        }
    }
}