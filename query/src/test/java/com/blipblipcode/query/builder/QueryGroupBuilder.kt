package com.blipblipcode.query.builder

import com.blipblipcode.query.QueryDelete
import com.blipblipcode.query.QuerySelect
import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalType
import com.blipblipcode.query.operator.SQLOperator

class QueryGroupBuilder {
    private var table: String = "tire_transfer_document"
    private var whereKey: String = "creator"
    private var whereOperation: LogicalOperation = LogicalOperation.Where(SQLOperator.Equals("creator", 1))
    private val conditions: MutableList<Pair<String, LogicalOperation>> = mutableListOf()

    fun withTable(table: String) = apply { this.table = table }

    fun withWhere(key: String, operator: SQLOperator<*>) = apply {
        this.whereKey = key
        this.whereOperation = LogicalOperation.Where(operator)
    }

    fun withAnd(key: String, operator: SQLOperator<*>) = apply {
        conditions.add(key to LogicalOperation.And(operator))
    }

    fun withOr(key: String, operator: SQLOperator<*>) = apply {
        conditions.add(key to LogicalOperation.Or(operator))
    }

    fun withAndGroup(key: String, vararg grouped: LogicalOperation) = apply {
        conditions.add(key to LogicalOperation.Multiple(grouped.toList(), LogicalType.AND.sql))
    }

    fun withOrGroup(key: String, vararg grouped: LogicalOperation) = apply {
        conditions.add(key to LogicalOperation.Multiple(grouped.toList(), LogicalType.OR.sql))
    }

    fun buildSelect(): QuerySelect {
        val builder = QuerySelect.builder(table).where(whereKey, whereOperation)
        conditions.forEach { (key, operation) -> builder.addLogicalOperation(key, operation) }
        return builder.build()
    }

    fun buildDelete(): QueryDelete {
        val builder = QueryDelete.builder(table).where(whereOperation.operator)
        conditions.forEach { (key, operation) -> builder.addLogicalOperation(key, operation) }
        return builder.build()
    }
}

fun queryGroup(block: QueryGroupBuilder.() -> Unit): QueryGroupBuilder = QueryGroupBuilder().apply(block)