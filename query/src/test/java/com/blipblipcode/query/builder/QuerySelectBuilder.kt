package com.blipblipcode.query.builder

import com.blipblipcode.query.QuerySelect
import com.blipblipcode.query.operator.Limit
import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.SQLOperator

class QuerySelectBuilder {
    private var table: String = "default_table"
    private var whereKey: String = "id"
    private var whereOperator: SQLOperator<*> = SQLOperator.Equals("id", 1)
    private var limit: Limit? = null
    private var orderBy: OrderBy? = null

    fun withTable(table: String) = apply { this.table = table }
    fun withWhere(key: String, operator: SQLOperator<*>) = apply {
        this.whereKey = key
        this.whereOperator = operator
    }
    fun withLimit(count: Int, offset: Int? = null) = apply { this.limit = Limit(count, offset) }
    fun withLimit(limit: Limit) = apply { this.limit = limit }
    fun withOrderBy(orderBy: OrderBy) = apply { this.orderBy = orderBy }

    fun build(): QuerySelect {
        val query = QuerySelect.builder(table)
            .where(whereOperator)
            .also { builder ->
                orderBy?.let { builder.orderBy(it) }
                limit?.let { builder.limit(it) }
            }
            .build()
        return query
    }
}

fun querySelect(block: QuerySelectBuilder.() -> Unit): QuerySelect =
    QuerySelectBuilder().apply(block).build()

