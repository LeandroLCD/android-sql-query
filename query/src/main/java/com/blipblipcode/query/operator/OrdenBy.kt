package com.blipblipcode.query.operator

sealed interface OrderBy {
    val column: String
    fun asString(): String
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
}