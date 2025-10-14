package com.blipblipcode.query

import com.blipblipcode.query.operator.Field
import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.utils.asSQLiteQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QueryUpdateTest {

    @Test
    fun `build with where clause and one field`() {
        val query = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .set("name", "John Doe")
            .build()
        val expectedSql = "UPDATE users SET name = 'John Doe' WHERE id = 1"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `build with where clause and multiple fields`() {
        val query = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .set("name", "John Doe")
            .set("status", "active")
            .build()
        val expectedSql = "UPDATE users SET name = 'John Doe', status = 'active' WHERE id = 1"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `build without where clause throws exception`() {
        val builder = QueryUpdate.builder("users")
            .set("name", "John Doe")

        assertThrows(IllegalArgumentException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `build without fields throws exception`() {
        val builder = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 1))

        assertThrows(IllegalArgumentException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `set field with blank name throws exception`() {
        val builder = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 1))

        assertThrows(IllegalArgumentException::class.java) {
            builder.set("", "value")
        }
    }

    @Test
    fun `set Field with blank name throws exception`() {
        val builder = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 1))

        assertThrows(IllegalArgumentException::class.java) {
            builder.set(Field("", "value"))
        }
    }

    @Test
    fun `remove field from builder`() {
        val query = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .set("name", "John Doe")
            .set("status", "active")
            .remove("status")
            .build()
        val expectedSql = "UPDATE users SET name = 'John Doe' WHERE id = 1"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `setWhere on existing QueryUpdate`() {
        val query = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .set("name", "John Doe")
            .build()

        query.setWhere(SQLOperator.Equals("id", 2))

        val expectedSql = "UPDATE users SET name = 'John Doe' WHERE id = 2"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `set on existing QueryUpdate`() {
        val query = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .set("name", "John Doe")
            .build()

        query.set("status", "inactive")

        val expectedSql = "UPDATE users SET name = 'John Doe', status = 'inactive' WHERE id = 1"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `remove on existing QueryUpdate`() {
        val query = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .set("name", "John Doe")
            .set("status", "active")
            .build()

        query.remove("status")

        val expectedSql = "UPDATE users SET name = 'John Doe' WHERE id = 1"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSQLiteQuery converts QueryUpdate to SupportSQLiteQuery`() {
        val queryUpdate = QueryUpdate.builder("users")
            .where(SQLOperator.Equals("id", 123))
            .set("name", "Jane Doe")
            .set("age", 30)
            .build()

        val supportSQLiteQuery = queryUpdate.asSQLiteQuery()

        assertEquals(queryUpdate.asSql(), supportSQLiteQuery.sql)
        assertEquals(0, supportSQLiteQuery.argCount)
    }
}