package com.blipblipcode.query.operator

sealed interface OrderBy:SQLOperator<String> {
    override val column: String
    override val symbol: String
    override val value: String
    override val caseConversion: CaseConversion
    override fun asString(): String

    fun asSqlClause(): String {
        return when (this) {
            is Asc , is Desc -> "${caseConversion.asSqlFunction(column)} $value"
            is Multiple -> orders.joinToString(", ") { it.asSqlClause() }
        }
    }
    fun clone(vararg params: Any?): OrderBy

    data class Asc(override val column: String) : OrderBy{
        override val symbol: String = "ORDER BY"
        override val value: String = "ASC"
        override val caseConversion: CaseConversion = CaseConversion.NONE
        override fun asString(): String {
            return "$symbol ${caseConversion.asSqlFunction(column)} $value"
        }

        override fun clone(vararg params: Any?): OrderBy {
            return this.copy(column = params[0] as String)
        }

        override fun toString(): String {
            return asString()
        }
    }
    data class Desc(override val column: String) : OrderBy{
        override val symbol: String = "ORDER BY"
        override val value: String = "DESC"
        override val caseConversion: CaseConversion = CaseConversion.NONE

        override fun asString(): String {
            return "$symbol ${caseConversion.asSqlFunction(column)} $value"
        }

        override fun clone(vararg params: Any?): OrderBy {
            return this.copy(column = params[0] as String)
        }

        override fun toString(): String {
            return asString()
        }
    }

    data class Multiple(val orders: List<OrderBy>) : OrderBy{
        override val column: String
            get() = orders.joinToString(", ") { it.column }
        override val symbol: String = "ORDER BY"
        override val value: String = orders.joinToString(", ") { it.value }
        override val caseConversion: CaseConversion = CaseConversion.NONE


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