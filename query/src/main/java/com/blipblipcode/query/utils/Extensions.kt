package com.blipblipcode.query.utils

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.blipblipcode.query.InnerJoint
import com.blipblipcode.query.QuerySelect
import com.blipblipcode.query.Queryable
import com.blipblipcode.query.UnionQuery
import com.blipblipcode.query.operator.Collation
import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalOperation.All
import com.blipblipcode.query.operator.LogicalOperation.And
import com.blipblipcode.query.operator.LogicalOperation.AndNot
import com.blipblipcode.query.operator.LogicalOperation.Exists
import com.blipblipcode.query.operator.LogicalOperation.Multiple
import com.blipblipcode.query.operator.LogicalOperation.Not
import com.blipblipcode.query.operator.LogicalOperation.Or
import com.blipblipcode.query.operator.LogicalOperation.Where
import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.OrderBy.Asc
import com.blipblipcode.query.operator.OrderBy.Desc
import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.retrofit.RepeatedQueryParameters

/**
 * Creates an `InnerJoint` by joining this `QuerySelect` with another one.
 * This is a convenience function to start an INNER JOIN chain.
 *
 * @param other The `QuerySelect` to join with.
 * @param onClause The ON clause for the join.
 * @return A new `InnerJoint` instance.
 */
fun QuerySelect.innerJoin(other: QuerySelect, onClause: String): InnerJoint {
    return InnerJoint.builder(this)
        .addJoin(other, onClause)
        .build()
}

/**
 * Adds another join to an existing `InnerJoint`.
 * This allows for chaining multiple INNER JOINs together.
 *
 * @param query The `QuerySelect` to add to the join.
 * @param onClause The ON clause for this new join.
 * @return A new `InnerJoint` instance containing the new join.
 */
fun InnerJoint.join(query: QuerySelect, onClause: String): InnerJoint {
    return InnerJoint.builder(this.queries.first())
        .addJoins(this.queries.drop(1), onClauses)
        .addJoin(query, onClause)
        .build()
}

/**
 * Converts any `Queryable` object into a `SupportSQLiteQuery` that can be used with Android's Room persistence library.
 *
 * @return A `SupportSQLiteQuery` instance representing the query.
 */
fun Queryable.asSQLiteQuery(): SupportSQLiteQuery {
    return SimpleSQLiteQuery(this.asSql())
}
/**
 * Creates a `UnionQuery` by combining this `QuerySelect` with another one using UNION.
 * UNION eliminates duplicate rows from the result set.
 *
 * @param other The `QuerySelect` to union with.
 * @return A new `UnionQuery` instance configured for UNION.
 */
fun QuerySelect.union(other: QuerySelect): UnionQuery {
    return UnionQuery.builder(this)
        .addQuery(other)
        .union()
        .build()
}

/**
 * Creates a `UnionQuery` by combining this `QuerySelect` with another one using UNION ALL.
 * UNION ALL preserves all rows including duplicates from the result set.
 *
 * @param other The `QuerySelect` to union with.
 * @return A new `UnionQuery` instance configured for UNION ALL.
 */
fun QuerySelect.unionAll(other: QuerySelect): UnionQuery {
    return UnionQuery.builder(this)
        .addQuery(other)
        .unionAll()
        .build()
}

/**
 * Adds another query to an existing `UnionQuery` using the same union type.
 * This allows for chaining multiple queries in a union operation.
 *
 * @param query The `QuerySelect` to add to the union.
 * @return A new `UnionQuery` instance containing the new query.
 */
fun UnionQuery.addQuery(query: QuerySelect): UnionQuery {
    return UnionQuery.QueryBuilder()
        .addQueries(this.queries)
        .addQuery(query)
        .apply { if (this@addQuery.useUnionAll) unionAll() else union() }
        .build()
}

fun Queryable.asQueryRepeatedQueryParameters(predicate:(Pair<String, Any?>) -> Boolean = {true}):RepeatedQueryParameters{
    return getSqlOperators()
        .map { it.toPair() }
        .fold(RepeatedQueryParameters.empty()) { acc, filter ->
            when {
                predicate.invoke(filter) && filter.second != null ->{
                    if(filter.second is List<*>){
                        acc.addRepeatedParameter(filter.first, filter.second as List<*>)
                    }else{
                        acc.addParameter(filter.first, filter.second!!)
                    }
                    acc
                }

                else -> acc
            }
        }
}

/**
 * Creates a copy of this `LogicalOperation` with a new `SQLOperator`.
 *
 * @param operator The new `SQLOperator` to use.
 * @return A new `LogicalOperation` instance with the updated operator.
 */
fun LogicalOperation.copyOperation(operator: SQLOperator<*> = this.operator): LogicalOperation {
    return when (this) {
        is All -> All(operator = operator)
        is And -> And(operator = operator)
        is AndNot -> AndNot(operator = operator)
        is Exists -> Exists(operator = operator)
        is Multiple -> {
            val updatedOperations = operations.toMutableList()
            if (updatedOperations.isNotEmpty()) {
                updatedOperations[0] = updatedOperations[0].copyOperation(operator)
            }
            Multiple(updatedOperations, symbol)
        }
        is Not -> Not(operator = operator)
        is Or -> Or(operator = operator)
        is Where -> Where(operator = operator)
    }
}

/**
 * Creates a copy of this `OrderBy` clause with updated parameters.
 *
 * @param column The new column name.
 * @param collation The new collation to use.
 * @param transform An optional transformation function for the column name.
 * @return A new `OrderBy` instance with the updated parameters.
 */
fun OrderBy.copyOrderBy(
    column: String = this.column,
    collation: Collation = this.collation,
    transform: (String) -> String = this.transform
): OrderBy {
    return when (this) {
        is Asc -> Asc(column, collation, transform)
        is Desc -> Desc(column, collation, transform)
        is OrderBy.Multiple -> OrderBy.Multiple(orders.map { it.copyOrderBy(column, collation, transform) })
    }
}
