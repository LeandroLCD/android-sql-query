package com.blipblipcode.query.utils

import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.SQLOperator
import org.junit.Assert.*
import org.junit.Test

class CopyTest {

    @Test
    fun should_create_new_instance_of_LogicalOperation_when_copying_in_copy() {
        val operator = LogicalOperation.Where(SQLOperator.Equals("id", 1))

        val newOperator = operator.copyOperation(operator = SQLOperator.NotEquals("id", 2))

        assertNotEquals(operator, newOperator)
        assertEquals(2, newOperator.operator.value)
    }

    @Test
    fun should_create_new_instance_of_OrderBy_when_copying_in_copy() {
        val orderBy = OrderBy.Asc("id")

        val newOrderBy = orderBy.copyOrderBy(column = "name")

        assertNotEquals(orderBy, newOrderBy)
        assertEquals("name", newOrderBy.column)
    }

}