package com.blipblipcode.query

import com.blipblipcode.query.operator.CaseConversion
import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalType
import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.utils.asSQLiteQuery
import org.junit.Assert.assertEquals
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
    fun `asSql with a basic WHERE `() {
        val query = QuerySelect.builder("users")
            .build()
        val expectedSql = "SELECT * FROM users"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `asSql with a WHERE clause and uppercase`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active", caseConversion = CaseConversion.UPPER))
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND UPPER(status) = UPPER('active')"
        assertEquals(expectedSql, query.asSql())
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

    @Test
    fun `limit results with a single limit value`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `limit results with offset`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(5, 10)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 5 OFFSET 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `limit results with chaining call`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10)
            .build()
        val instance = query.limit(5, 10)
        assertEquals(query, instance)
    }

    @Test
    fun `limit results with various conditions`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10, 5)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 10 OFFSET 5"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `limit results with no offset`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10, 0)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `limit results with zero limit`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(0)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 0"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `limit results with negative limit`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(-5)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT -5"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `limit results with negative offset`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10, -5)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 10 OFFSET -5"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with ascending order`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Asc("name"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY name ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with descending order`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Desc("created_at"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY created_at DESC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with multiple columns ascending`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Asc("name"),
            OrderBy.Asc("age")
        )))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY name ASC, age ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with multiple columns mixed directions`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Asc("status"),
            OrderBy.Desc("created_at")
        )))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY status ASC, created_at DESC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with descending then ascending`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Desc("priority"),
            OrderBy.Asc("name")
        )))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY priority DESC, name ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with ascending and descending columns`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Asc("department"),
            OrderBy.Desc("salary")
        )))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY department ASC, salary DESC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with limit and ascending order`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10)
            .build()
        query.orderBy(OrderBy.Asc("name"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY name ASC LIMIT 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with limit and descending order`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(5, 10)
            .build()
        query.orderBy(OrderBy.Desc("created_at"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY created_at DESC LIMIT 5 OFFSET 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy chaining call`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        val instance = query.orderBy(OrderBy.Asc("name"))
        assertEquals(query, instance)
    }

    @Test
    fun `orderBy replacing previous order`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Desc("created_at"))
        query.orderBy(OrderBy.Asc("name"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY name ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with null value`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(null)
        val expectedSql = "SELECT * FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with three columns mixed directions`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Asc("department"),
            OrderBy.Desc("created_at"),
            OrderBy.Asc("name")
        )))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY department ASC, created_at DESC, name ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with special characters in column names`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Asc("`first name`"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY `first name` ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy with multiple columns and special characters`() {
        val query = QuerySelect.builder("`user table`")
            .where(SQLOperator.Equals("`user id`", 1))
            .build()
        query.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Asc("`first name`"),
            OrderBy.Desc("`last name`")
        )))
        val expectedSql = "SELECT * FROM `user table` WHERE `user id` = 1 ORDER BY `first name` ASC, `last name` DESC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `orderBy ascending with multiple logical operations`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .or("role", SQLOperator.Equals("role", "admin"))
            .build()
        query.orderBy(OrderBy.Asc("created_at"))
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active' OR role = 'admin' ORDER BY created_at ASC"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `orderBy descending with multiple logical operations`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .or("role", SQLOperator.Equals("role", "admin"))
            .build()
        query.orderBy(OrderBy.Desc("created_at"))
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active' OR role = 'admin' ORDER BY created_at DESC"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `orderBy multiple with specific fields`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("name", "email", "created_at")
            .build()
        query.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Asc("name"),
            OrderBy.Desc("created_at")
        )))
        val expectedSql = "SELECT name, email, created_at FROM users WHERE status = 'active' ORDER BY name ASC, created_at DESC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with single field alias`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("name AS full_name")
            .build()
        val expectedSql = "SELECT name AS full_name FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with multiple fields with aliases`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("name AS full_name", "email AS user_email", "created_at AS registration_date")
            .build()
        val expectedSql = "SELECT name AS full_name, email AS user_email, created_at AS registration_date FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with mixed fields and aliases`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("id", "name AS full_name", "email")
            .build()
        val expectedSql = "SELECT id, name AS full_name, email FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with function and alias`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("COUNT(*) AS total_users", "name AS user_name")
            .build()
        val expectedSql = "SELECT COUNT(*) AS total_users, name AS user_name FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with uppercase alias`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("name AS NAME", "email AS EMAIL")
            .build()
        val expectedSql = "SELECT name AS NAME, email AS EMAIL FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with backtick quoted alias`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("`name` AS `full name`", "`email` AS `user email`")
            .build()
        val expectedSql = "SELECT `name` AS `full name`, `email` AS `user email` FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with table prefix and alias`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("users.id", 1))
            .setFields("users.name AS full_name", "users.email AS user_email")
            .build()
        val expectedSql = "SELECT users.name AS full_name, users.email AS user_email FROM users WHERE users.id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with aliases and orderBy`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("name AS full_name", "created_at AS registration_date")
            .build()
        query.orderBy(OrderBy.Asc("full_name"))
        val expectedSql = "SELECT name AS full_name, created_at AS registration_date FROM users WHERE status = 'active' ORDER BY full_name ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with aliases and limit`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("name AS full_name", "email AS user_email")
            .limit(10)
            .build()
        val expectedSql = "SELECT name AS full_name, email AS user_email FROM users WHERE status = 'active' LIMIT 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with aliases orderBy and limit combined`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("name AS full_name", "created_at AS registration_date", "email AS user_email")
            .limit(5, 10)
            .build()
        query.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Asc("registration_date"),
            OrderBy.Desc("full_name")
        )))
        val expectedSql = "SELECT name AS full_name, created_at AS registration_date, email AS user_email FROM users WHERE status = 'active' ORDER BY registration_date ASC, full_name DESC LIMIT 5 OFFSET 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with CASE statement and alias`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("CASE WHEN status = 'active' THEN 'Active User' ELSE 'Inactive' END AS user_status")
            .build()
        val expectedSql = "SELECT CASE WHEN status = 'active' THEN 'Active User' ELSE 'Inactive' END AS user_status FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with aggregate functions and aliases`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("COUNT(*) AS total", "SUM(salary) AS total_salary", "AVG(salary) AS average_salary")
            .build()
        val expectedSql = "SELECT COUNT(*) AS total, SUM(salary) AS total_salary, AVG(salary) AS average_salary FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with mathematical expression and alias`() {
        val query = QuerySelect.builder("products")
            .where(SQLOperator.Equals("category", "electronics"))
            .setFields("name", "price", "price * 0.1 AS discount_amount")
            .build()
        val expectedSql = "SELECT name, price, price * 0.1 AS discount_amount FROM products WHERE category = 'electronics'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun `setFields with alias immutability`() {
        val originalQuery = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val originalSql = originalQuery.asSql()

        originalQuery.setFields("name AS full_name", "email AS user_email")

        assertEquals(originalSql, originalQuery.asSql())
    }

    @Test
    fun `setFields replacing previous fields with aliases`() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("id", "name")
            .build()
        val newQuery = query.setFields("name AS full_name", "email AS user_email", "created_at AS registration_date")
        val expectedSql = "SELECT name AS full_name, email AS user_email, created_at AS registration_date FROM users WHERE status = 'active'"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun `setFields with multiple aliases using builder`() {
        val query = QuerySelect.builder("employees")
            .where(SQLOperator.Equals("department", "sales"))
            .setFields("employee_id AS id", "first_name AS fname", "last_name AS lname", "salary AS monthly_salary")
            .build()
        val expectedSql = "SELECT employee_id AS id, first_name AS fname, last_name AS lname, salary AS monthly_salary FROM employees WHERE department = 'sales'"
        assertEquals(expectedSql, query.asSql().trim())
    }
}
