package com.blipblipcode.query

import com.blipblipcode.query.operator.Field
import com.blipblipcode.query.utils.asSQLiteQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class QueryInsertTest {

    @Test
    fun `add key value with a new valid key and value`() {
        val query = QueryInsert.builder("users").add("name", "John").build()
        val expectedSql = "INSERT INTO users (name) VALUES ('John')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `add key value with an existing key to test overwrite`() {
        val query = QueryInsert.builder("users")
            .add("name", "John")
            .add("name", "Jane")
            .build()
        val expectedSql = "INSERT INTO users (name) VALUES ('Jane')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `add key value with various data types for value`() {
        val query = QueryInsert.builder("users")
            .add("name", "John")
            .add("age", 30)
            .add("height", 1.80)
            .add("is_active", true)
            .build()
        val expectedSql = "INSERT INTO users (name, age, height, is_active) VALUES ('John', 30, 1.8, true)"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `add key value with an empty string as a key`() {
        assertThrows(IllegalArgumentException::class.java) {
            QueryInsert.builder("users").add("", "value").build()
        }
    }

    @Test
    fun `add key value with a null value`() {
        val query = QueryInsert.builder("users").add("name", null).build()
        val expectedSql = "INSERT INTO users (name) VALUES (null)"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `add key value chaining multiple calls`() {
        val query = QueryInsert.builder("users")
            .add("name", "John")
            .add("age", 30)
            .build()
        val expectedSql = "INSERT INTO users (name, age) VALUES ('John', 30)"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `add Field with a new valid Field object`() {
        val query = QueryInsert.builder("users").add(Field("name", "John")).build()
        val expectedSql = "INSERT INTO users (name) VALUES ('John')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `add Field with an existing field name to test overwrite`() {
        val query = QueryInsert.builder("users")
            .add(Field("name", "John"))
            .add(Field("name", "Jane"))
            .build()
        val expectedSql = "INSERT INTO users (name) VALUES ('Jane')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `add Field with various data types in the Field object`() {
        val query = QueryInsert.builder("users")
            .add(Field("name", "John"))
            .add(Field("age", 30))
            .add(Field("height", 1.80))
            .add(Field("is_active", true))
            .build()
        val expectedSql = "INSERT INTO users (name, age, height, is_active) VALUES ('John', 30, 1.8, true)"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `add Field with a Field object having an empty name`() {
        assertThrows(IllegalArgumentException::class.java) {
            QueryInsert.builder("users").add(Field("", "value")).build()
        }
    }

    @Test
    fun `add Field with a Field object containing a null value`() {
        val query = QueryInsert.builder("users").add(Field("name", null)).build()
        val expectedSql = "INSERT INTO users (name) VALUES (null)"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `add Field chaining multiple calls`() {
        val query = QueryInsert.builder("users")
            .add(Field("name", "John"))
            .add(Field("age", 30))
            .build()
        val expectedSql = "INSERT INTO users (name, age) VALUES ('John', 30)"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `remove key with an existing key`() {
        val query = QueryInsert.builder("users")
            .add("name", "John")
            .add("age", 30)
            .remove("age")
            .build()
        val expectedSql = "INSERT INTO users (name) VALUES ('John')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `remove key with a non existent key`() {
        val query = QueryInsert.builder("users")
            .add("name", "John")
            .remove("non_existent")
            .build()
        val expectedSql = "INSERT INTO users (name) VALUES ('John')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `remove key with an empty string as a key`() {
        assertThrows(IllegalArgumentException::class.java) {
             QueryInsert.builder("users").add("", "value").remove("").build()
        }
    }

    @Test
    fun `remove key called on an empty set of fields`() {
        val query = QueryInsert.builder("users").remove("anyKey")
        assertThrows(IllegalArgumentException::class.java) {
            query.build().asSql()
        }
    }

    @Test
    fun `remove key chaining multiple calls`() {
        val query = QueryInsert.builder("users")
            .add("name", "John")
            .add("age", 30)
            .remove("name")
            .remove("age")

        assertThrows(IllegalArgumentException::class.java) {
            query.build().asSql()
        }
    }


    @Test
    fun `asSql with a single field`() {
        val query = QueryInsert.builder("users").add("name", "John").build()
        val expectedSql = "INSERT INTO users (name) VALUES ('John')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSql with multiple fields`() {
        val query = QueryInsert.builder("users")
            .add("name", "John")
            .add("age", 30)
            .build()
        val expectedSql = "INSERT INTO users (name, age) VALUES ('John', 30)"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSql with no fields added`() {
        val query = QueryInsert.builder("users")
        assertThrows(IllegalArgumentException::class.java) {
            query.build().asSql()
        }
    }

    @Test
    fun `asSql after adding and then removing all fields`() {
        val query = QueryInsert.builder("users")
            .add("name", "John")
            .remove("name")
        assertThrows(IllegalArgumentException::class.java) {
            query.build().asSql()
        }
    }

    @Test
    fun `asSql with different data types`() {
        val query = QueryInsert.builder("users")
            .add("name", "John")
            .add("age", 30)
            .add("height", 1.8)
            .add("is_active", true)
            .add("last_login", null)
            .build()
        val expectedSql = "INSERT INTO users (name, age, height, is_active, last_login) VALUES ('John', 30, 1.8, true, null)"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSql with special characters in table or field names`() {
        val query = QueryInsert.builder("`my table`")
            .add("`my field`", "value")
            .build()
        val expectedSql = "INSERT INTO `my table` (`my field`) VALUES ('value')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSql with SQL injection like strings in values`() {
        val query = QueryInsert.builder("users")
            .add("name", "John'; DROP TABLE users; --")
            .build()
        val expectedSql = "INSERT INTO users (name) VALUES ('John'; DROP TABLE users; --')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun `asSQLiteQuery converts QueryInsert to SupportSQLiteQuery`() {
        val queryInsert = QueryInsert.builder("users")
            .add("name", "John")
            .add("age", 30)
            .build()

        val supportSQLiteQuery = queryInsert.asSQLiteQuery()

        assertEquals(queryInsert.asSql(), supportSQLiteQuery.sql)
        assertEquals(0, supportSQLiteQuery.argCount)
    }
}
