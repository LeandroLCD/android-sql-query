package com.blipblipcode.query

import com.blipblipcode.query.operator.Collation
import com.blipblipcode.query.operator.and
import com.blipblipcode.query.operator.removeCharsTransform
import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.SQLOperator
import org.junit.Assert.assertEquals
import org.junit.Test

class CollationUtilsTest {

    @Test
    fun `removeCharsTransform builds REPLACE chain with COLLATE NOCASE`() {
        val q = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()

        // eliminar espacios, guiones y puntos; ignorar mayúsculas con COLLATE NOCASE
        val strip = " -.".removeCharsTransform()

        q.orderBy(OrderBy.Asc("name", collation = Collation.NOCASE, transform = strip))

        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY REPLACE(REPLACE(REPLACE(name, ' ', ''), '-', ''), '.', '') COLLATE NOCASE ASC"
        assertEquals(expectedSql, q.asSql().trim())
    }

    @Test
    fun `REPLACE with COLLATE NOCASE produces correct SQL for vehicle identification`() {
        val q = QuerySelect.builder("vehicle")
            .where(SQLOperator.Equals("active", true))
            .build()

        // Eliminar guiones y aplicar COLLATE NOCASE
        val strip = "-".removeCharsTransform()
        q.orderBy(OrderBy.Asc("identification", collation = Collation.NOCASE, transform = strip))

        val expectedSql = "SELECT * FROM vehicle WHERE active = true ORDER BY REPLACE(identification, '-', '') COLLATE NOCASE ASC"
        assertEquals(expectedSql, q.asSql().trim())
    }

    @Test
    fun `REPLACE with COLLATE BINARY produces correct SQL`() {
        val q = QuerySelect.builder("products")
            .where(SQLOperator.Equals("status", "available"))
            .build()

        val strip = " ".removeCharsTransform()
        q.orderBy(OrderBy.Desc("code", collation = Collation.BINARY, transform = strip))

        val expectedSql = "SELECT * FROM products WHERE status = 'available' ORDER BY REPLACE(code, ' ', '') COLLATE BINARY DESC"
        assertEquals(expectedSql, q.asSql().trim())
    }

    @Test
    fun `COLLATE NOCASE alone is sufficient for case-insensitive ordering`() {
        val q = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()

        q.orderBy(OrderBy.Asc("name", collation = Collation.NOCASE))

        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY name COLLATE NOCASE ASC"
        assertEquals(expectedSql, q.asSql().trim())
    }

    @Test
    fun `empty string removeCharsTransform returns identity`() {
        val transform = "".removeCharsTransform()
        assertEquals("col", transform("col"))
    }

    @Test
    fun `removeCharsTransform escapes single quotes`() {
        val transform = "'".removeCharsTransform()
        assertEquals("REPLACE(name, '''', '')", transform("name"))
    }

    @Test
    fun `RTRIM and NOCASE composite collation with transform`() {
        val q = QuerySelect.builder("items")
            .where(SQLOperator.Equals("type", "A"))
            .build()

        val strip = "-".removeCharsTransform()
        q.orderBy(OrderBy.Asc("code", collation = Collation.RTRIM and Collation.NOCASE, transform = strip))

        val expectedSql = "SELECT * FROM items WHERE type = 'A' ORDER BY RTRIM(REPLACE(code, '-', '')) COLLATE NOCASE ASC"
        assertEquals(expectedSql, q.asSql().trim())
    }

    @Test
    fun `Multiple OrderBy with different transforms and collations`() {
        val q = QuerySelect.builder("vehicle")
            .where(SQLOperator.Equals("active", true))
            .build()

        val stripDash = "-".removeCharsTransform()
        q.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Asc("identification", collation = Collation.NOCASE, transform = stripDash),
            OrderBy.Desc("name")
        )))

        val expectedSql = "SELECT * FROM vehicle WHERE active = true ORDER BY REPLACE(identification, '-', '') COLLATE NOCASE ASC, name DESC"
        assertEquals(expectedSql, q.asSql().trim())
    }
}
