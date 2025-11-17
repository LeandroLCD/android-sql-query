package com.blipblipcode.query

import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalType
import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.utils.asSQLiteQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QueryDeleteTest {

    @Test
    fun `build with where clause only`() {
        val query = QueryDelete.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val expectedSql = "DELETE FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `build with where and and clauses`() {
        val query = QueryDelete.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()
        val expectedSql = "DELETE FROM users WHERE id = 1 AND status = 'active'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `build with where and multiple logical operations`() {
        val query = QueryDelete.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .or("role", SQLOperator.Equals("role", "guest"))
            .build()
        val expectedSql = "DELETE FROM users WHERE id = 1 AND status = 'active' OR role = 'guest'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `build without where clause throws exception`() {
        val builder = QueryDelete.builder("users")

        assertThrows(IllegalArgumentException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `remove operation from builder`() {
        val query = QueryDelete.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .remove("status")
            .build()
        val expectedSql = "DELETE FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `setWhere on existing QueryDelete`() {
        val query = QueryDelete.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()

        query.setWhere(SQLOperator.Equals("id", 2))

        val expectedSql = "DELETE FROM users WHERE id = 2"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `addLogicalOperation on existing QueryDelete`() {
        val query = QueryDelete.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()

        query.addLogicalOperation("status", LogicalOperation(LogicalType.AND, SQLOperator.Equals("status", "inactive")))

        val expectedSql = "DELETE FROM users WHERE id = 1 AND status = 'inactive'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `remove on existing QueryDelete`() {
        val query = QueryDelete.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()

        query.remove("status")

        val expectedSql = "DELETE FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSql with special characters in table name`() {
        val query = QueryDelete.builder("`user accounts`")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val expectedSql = "DELETE FROM `user accounts` WHERE id = 1"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSQLiteQuery converts QueryDelete to SupportSQLiteQuery`() {
        val queryDelete = QueryDelete.builder("users")
            .where(SQLOperator.Equals("name", "John"))
            .and("status", SQLOperator.NotEquals("status", "banned"))
            .build()

        val supportSQLiteQuery = queryDelete.asSQLiteQuery()

        assertEquals(queryDelete.asSql(), supportSQLiteQuery.sql)
        assertEquals(0, supportSQLiteQuery.argCount)
    }
}
