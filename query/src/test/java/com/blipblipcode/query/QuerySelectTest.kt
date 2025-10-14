package com.blipblipcode.query

import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalType
import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.utils.asSQLiteQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QuerySelectTest {

    @Test
    fun `remove key with an existing key`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()
        query.remove("status")
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `remove key with a non existent key`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val originalSql = query.asSql()
        query.remove("non_existent_key")
        assertEquals(originalSql, query.asSql())
    }

    @Test
    fun `remove key with an empty string key`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("", SQLOperator.Equals("status", "active"))
            .build()
        query.remove("")
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `remove key when operations map is empty`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val originalSql = query.asSql()
        query.remove("any_key")
        assertEquals(originalSql, query.asSql())
    }

    @Test
    fun `remove key chaining call`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()
        val instance = query.remove("status")
        assertEquals(query, instance)
    }

    @Test
    fun `setWhere operator to replace an existing clause`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.setWhere(SQLOperator.Equals("id", 2))
        val expectedSql = "SELECT * FROM users WHERE id = 2"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setWhere operator with a different operator type`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.setWhere(SQLOperator.GreaterThan("age", 30))
        val expectedSql = "SELECT * FROM users WHERE age > 30"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setWhere operator chaining call`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val instance = query.setWhere(SQLOperator.Equals("id", 2))
        assertEquals(query, instance)
    }

    @Test
    fun `addLogicalOperation key operation with a new key`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.addLogicalOperation("status", LogicalOperation(LogicalType.AND, SQLOperator.Equals("status", "active")))
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `addLogicalOperation key operation with a duplicate key`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "inactive"))
            .build()
        query.addLogicalOperation("status", LogicalOperation(LogicalType.AND, SQLOperator.Equals("status", "active")))
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `addLogicalOperation key operation with an empty string key`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.addLogicalOperation("", LogicalOperation(LogicalType.AND, SQLOperator.Equals("status", "active")))
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `addLogicalOperation key operation with various LogicalOperation types`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
            .addLogicalOperation("age", LogicalOperation(LogicalType.OR, SQLOperator.GreaterThan("age", 30)))
        val expectedSql = "SELECT * FROM users WHERE id = 1 OR age > 30"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `setFields newFields with multiple field names`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val newQuery = query.setFields("name", "age")
        val expectedSql = "SELECT name, age FROM users WHERE id = 1"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun `setFields newFields with a single field name`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val newQuery = query.setFields("name")
        val expectedSql = "SELECT name FROM users WHERE id = 1"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun `setFields newFields with no arguments`() {
        val queryWithFields = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
            .setFields("name", "age")

        val newQuery = queryWithFields.setFields()
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun `setFields newFields immutability check`() {
        val originalQuery = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val originalSql = originalQuery.asSql()

        originalQuery.setFields("name", "age") // This returns a new instance

        assertEquals(originalSql, originalQuery.asSql()) // Original instance is unchanged
    }

    @Test
    fun `setFields newFields with empty or blank strings`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val newQuery = query.setFields("", " ")
        val expectedSql = "SELECT ,   FROM users WHERE id = 1"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun `asSql with a basic WHERE clause and all fields`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `asSql with specific fields and no logical operations`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("name", "email")
            .build()
        val expectedSql = "SELECT name, email FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `asSql with a WHERE clause and a single logical operation`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSql with multiple logical operations`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .or("role", SQLOperator.Equals("role", "admin"))
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active' OR role = 'admin'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSql with special characters in table or field names`() {
        val query = QuerySelect.builder("`user table`")
            .where(SQLOperator.Equals("`user id`", 1))
            .setFields("`first name`", "`last name`")
            .build()
        val expectedSql = "SELECT `first name`, `last name` FROM `user table` WHERE `user id` = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `asSql after removing a logical operation`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .or("role", SQLOperator.Equals("role", "admin"))
            .remove("status")
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1 OR role = 'admin'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSql after changing the WHERE clause`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.setWhere(SQLOperator.GreaterThan("age", 18))
        val expectedSql = "SELECT * FROM users WHERE age > 18"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `asSql with empty fields list explicitly set`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields()
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `asSQLiteQuery converts QuerySelect to SupportSQLiteQuery`() {
        val querySelect = QuerySelect.builder("users")
            .where(SQLOperator.Equals("name", "John"))
            .build()

        val supportSQLiteQuery = querySelect.asSQLiteQuery()

        assertEquals(querySelect.asSql().trim(), supportSQLiteQuery.sql)
        assertEquals(0, supportSQLiteQuery.argCount)
    }
}
