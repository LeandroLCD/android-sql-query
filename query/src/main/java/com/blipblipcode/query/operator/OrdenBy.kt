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

    override fun clone(): OrderBy

    fun asSqlClause(): String {
        return when (this) {
            is Asc, is Desc -> collation.apply(transform(column)) + " " + value
            is Multiple -> orders.joinToString(", ") { it.asSqlClause() }
        }
    }

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

        override fun clone(): OrderBy {
            return this.copy()
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

        override fun clone(): OrderBy {
            return this.copy()
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

        override fun clone(): OrderBy {
           return this.copy()
        }

        override fun toString(): String = "ORDER BY ${asSqlClause()}"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Multiple) return false
            return orders == other.orders
        }

        override fun hashCode(): Int = orders.hashCode() + 31 * symbol.hashCode()
    }

}