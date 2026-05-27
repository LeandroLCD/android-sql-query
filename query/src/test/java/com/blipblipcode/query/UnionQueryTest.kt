package com.blipblipcode.query

import com.blipblipcode.query.builder.querySelect
import com.blipblipcode.query.operator.Limit
import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.SQLOperator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class UnionQueryTest {

    private val query1 = QuerySelect.builder("table1").where(SQLOperator.Equals("id", 1)).build()
    private val query2 = QuerySelect.builder("table2").where(SQLOperator.Equals("id", 2)).build()
    private val query3 = QuerySelect.builder("table3").where(SQLOperator.Equals("id", 3)).build()

    @Test
    fun `orderBy with single column`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Asc("name"))
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION\n${query2.asSql()}\n)\nORDER BY name ASC"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `orderBy with multiple columns`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Asc("name"))
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION\n${query2.asSql()}\n)\nORDER BY name ASC"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `orderBy with different sort directions`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Desc("name"))
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION\n${query2.asSql()}\n)\nORDER BY name DESC"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `orderBy overwriting previous clause`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Desc("age"))
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION\n${query2.asSql()}\n)\nORDER BY age DESC"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `orderBy returns the same instance`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build()
        val sameInstance = unionQuery.orderBy(OrderBy.Desc("age"))
        assertEquals(unionQuery, sameInstance)
    }

    @Test
    fun `asSql with two queries for UNION`() {
        val unionQuery = UnionQuery.builder(query1).addQuery( query2).build()
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION\n${query2.asSql()}\n)"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql with two queries for UNION ALL`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).unionAll().build()
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION ALL\n${query2.asSql()}\n)"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql with multiple queries for UNION`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).addQuery(query3).build()
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION\n${query2.asSql()}\nUNION\n${query3.asSql()}\n)"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql with multiple queries for UNION ALL`() {
        val unionQuery = UnionQuery.builder(query1).addQuery( query2).addQuery(query3).unionAll().build()
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION ALL\n${query2.asSql()}\nUNION ALL\n${query3.asSql()}\n)"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql for UNION with an orderBy clause`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Desc("name"))
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION\n${query2.asSql()}\n)\nORDER BY name DESC"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql for UNION ALL with an orderBy clause`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).unionAll().build().orderBy(OrderBy.Asc("name"))
        val expectedSql = "SELECT * FROM (\n${query1.asSql()}\nUNION ALL\n${query2.asSql()}\n)\nORDER BY name ASC"
        assertEquals(expectedSql, unionQuery.asSql())
    }


    @Test
    fun `asSql with one query throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            UnionQuery.builder(query1).build().asSql()
        }
    }

    @Test
    fun `getQueries returns all added queries`() {
        val queries = listOf(query1, query2, query3)
        val unionQuery = UnionQuery.builder(query1).addQuery( query2).addQuery(query3).build()
        assertEquals(queries, unionQuery.queries)
    }


    @Test
    fun `getQueries with minimum number of queries`() {
        val queries = listOf(query1, query2)
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build()
        assertEquals(queries, unionQuery.queries)
    }

    @Test
    fun `asSql preserve internal queries orderBy and combines it in UnionQuery`() {
        val q1 = QuerySelect.builder("table1")
            .where(SQLOperator.Equals("id", 1))
            .orderBy(OrderBy.Asc("name"))
            .build()
        val q2 = QuerySelect.builder("table2")
            .where(SQLOperator.Equals("id", 2))
            .orderBy(OrderBy.Desc("date"))
            .build()

        val unionQuery = UnionQuery.builder(q1)
            .addQuery(q2)
            .build()

        val sql = unionQuery.asSql()

        // El SQL resultante de UnionQuery debe tener los ORDER BY combinados al final
        // y las queries internas NO deben tener el ORDER BY (regla SQL para UNION)
        val expectedSql = "SELECT * FROM (\nSELECT * FROM table1 WHERE id = 1\nUNION\nSELECT * FROM table2 WHERE id = 2\n)\nORDER BY name ASC, date DESC"
        assertEquals(expectedSql, sql)

        // Verificamos que las queries originales NO fueron modificadas (no se les puso el orderBy en null)
        assertNotNull(q1.getOrderBy())
        assertNotNull(q2.getOrderBy())
        assertEquals("name", q1.getOrderBy()?.column)
        assertEquals("date", q2.getOrderBy()?.column)
    }

    @Test
    fun `asSql with UnionQuery level orderBy ignores internal queries orderBy`() {
        val q1 = QuerySelect.builder("table1")
            .where(SQLOperator.Equals("id", 1))
            .orderBy(OrderBy.Asc("name"))
            .build()
        val q2 = QuerySelect.builder("table2")
            .where(SQLOperator.Equals("id", 2))
            .build()

        val unionQuery = UnionQuery.builder(q1)
            .addQuery(q2)
            .build()
            .orderBy(OrderBy.Desc("id"))

        val sql = unionQuery.asSql()

        // Cuando se define un orderBy a nivel de UnionQuery, se ignoran los orderBy de las queries internas
        val expectedSql = "SELECT * FROM (\nSELECT * FROM table1 WHERE id = 1\nUNION\nSELECT * FROM table2 WHERE id = 2\n)\nORDER BY id DESC"
        assertEquals(expectedSql, sql)
    }

    // ---- limit() ----

    @Test
    fun should_apply_limit_to_all_queries_when_no_query_has_limit_in_limit() {
        //GIVEN
        val q1 = querySelect { withTable("table1") }
        val q2 = querySelect { withTable("table2") }
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.limit(10)

        //THEN
        assertEquals(10, q1.getLimit()?.count)
        assertEquals(10, q2.getLimit()?.count)
    }

    @Test
    fun should_apply_limit_with_offset_to_all_queries_when_no_query_has_limit_in_limit() {
        //GIVEN
        val q1 = querySelect { withTable("table1") }
        val q2 = querySelect { withTable("table2") }
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.limit(10, 5)

        //THEN
        assertEquals(10, q1.getLimit()?.count)
        assertEquals(5, q1.getLimit()?.offset)
        assertEquals(10, q2.getLimit()?.count)
        assertEquals(5, q2.getLimit()?.offset)
    }

    @Test
    fun should_not_replace_existing_limit_when_override_is_false_in_limit() {
        //GIVEN
        val q1 = querySelect { withTable("table1"); withLimit(3) }
        val q2 = querySelect { withTable("table2") }
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.limit(10, override = false)

        //THEN
        assertEquals(3, q1.getLimit()?.count)
        assertEquals(10, q2.getLimit()?.count)
    }

    @Test
    fun should_replace_existing_limit_when_override_is_true_in_limit() {
        //GIVEN
        val q1 = querySelect { withTable("table1"); withLimit(3) }
        val q2 = querySelect { withTable("table2") }
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.limit(10, override = true)

        //THEN
        assertEquals(10, q1.getLimit()?.count)
        assertEquals(10, q2.getLimit()?.count)
    }

    @Test
    fun should_apply_limit_operator_to_all_queries_when_no_query_has_limit_in_limit() {
        //GIVEN
        val q1 = querySelect { withTable("table1") }
        val q2 = querySelect { withTable("table2") }
        val limitOperator = Limit(20, 5)
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.limit(limitOperator)

        //THEN
        assertEquals(limitOperator, q1.getLimit())
        assertEquals(limitOperator, q2.getLimit())
    }

    @Test
    fun should_not_replace_existing_limit_when_using_limit_operator_and_override_is_false_in_limit() {
        //GIVEN
        val existingLimit = Limit(3)
        val q1 = querySelect { withTable("table1"); withLimit(existingLimit) }
        val q2 = querySelect { withTable("table2") }
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.limit(Limit(20), override = false)

        //THEN
        assertEquals(existingLimit, q1.getLimit())
        assertEquals(20, q2.getLimit()?.count)
    }

    @Test
    fun should_replace_existing_limit_when_using_limit_operator_and_override_is_true_in_limit() {
        //GIVEN
        val q1 = querySelect { withTable("table1"); withLimit(3) }
        val q2 = querySelect { withTable("table2"); withLimit(5) }
        val newLimit = Limit(50)
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.limit(newLimit, override = true)

        //THEN
        assertEquals(newLimit, q1.getLimit())
        assertEquals(newLimit, q2.getLimit())
    }

    @Test
    fun should_return_same_instance_when_calling_limit_in_limit() {
        //GIVEN
        val q1 = querySelect { withTable("table1") }
        val q2 = querySelect { withTable("table2") }
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        val result = unionQuery.limit(10)

        //THEN
        assertEquals(unionQuery, result)
    }

    // ---- clearLimit() ----

    @Test
    fun should_remove_limit_from_all_queries_in_clearLimit() {
        //GIVEN
        val q1 = querySelect { withTable("table1"); withLimit(10) }
        val q2 = querySelect { withTable("table2"); withLimit(5) }
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.clearLimit()

        //THEN
        assertNull(q1.getLimit())
        assertNull(q2.getLimit())
    }

    @Test
    fun should_not_fail_when_queries_have_no_limit_in_clearLimit() {
        //GIVEN
        val q1 = querySelect { withTable("table1") }
        val q2 = querySelect { withTable("table2") }
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.clearLimit()

        //THEN
        assertNull(q1.getLimit())
        assertNull(q2.getLimit())
    }

    @Test
    fun should_return_same_instance_in_clearLimit() {
        //GIVEN
        val q1 = querySelect { withTable("table1"); withLimit(10) }
        val q2 = querySelect { withTable("table2") }
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        val result = unionQuery.clearLimit()

        //THEN
        assertEquals(unionQuery, result)
    }
}