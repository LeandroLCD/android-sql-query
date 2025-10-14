package com.blipblipcode.query

import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.SQLOperator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UnionQueryTest {

    private val query1 = QuerySelect.builder("table1").where(SQLOperator.Equals("id", 1)).build()
    private val query2 = QuerySelect.builder("table2").where(SQLOperator.Equals("id", 2)).build()
    private val query3 = QuerySelect.builder("table3").where(SQLOperator.Equals("id", 3)).build()

    @Test
    fun `orderBy with single column`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Asc("name"))
        val expectedSql = "${query1.asSql()}\nUNION\n${query2.asSql()}\nORDER BY name ASC"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `orderBy with multiple columns`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Asc("name"))
        val expectedSql = "${query1.asSql()}\nUNION\n${query2.asSql()}\nORDER BY name ASC"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `orderBy with different sort directions`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Desc("name"))
        val expectedSql = "${query1.asSql()}\nUNION\n${query2.asSql()}\nORDER BY name DESC"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `orderBy overwriting previous clause`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Desc("age"))
        val expectedSql = "${query1.asSql()}\nUNION\n${query2.asSql()}\nORDER BY age DESC"
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
        val expectedSql = "${query1.asSql()}\nUNION\n${query2.asSql()}"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql with two queries for UNION ALL`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).unionAll().build()
        val expectedSql = "${query1.asSql()}\nUNION ALL\n${query2.asSql()}"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql with multiple queries for UNION`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).addQuery(query3).build()
        val expectedSql = "${query1.asSql()}\nUNION\n${query2.asSql()}\nUNION\n${query3.asSql()}"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql with multiple queries for UNION ALL`() {
        val unionQuery = UnionQuery.builder(query1).addQuery( query2).addQuery(query3).unionAll().build()
        val expectedSql = "${query1.asSql()}\nUNION ALL\n${query2.asSql()}\nUNION ALL\n${query3.asSql()}"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql for UNION with an orderBy clause`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).build().orderBy(OrderBy.Desc("name"))
        val expectedSql = "${query1.asSql()}\nUNION\n${query2.asSql()}\nORDER BY name DESC"
        assertEquals(expectedSql, unionQuery.asSql())
    }

    @Test
    fun `asSql for UNION ALL with an orderBy clause`() {
        val unionQuery = UnionQuery.builder(query1).addQuery(query2).unionAll().build().orderBy(OrderBy.Asc("name"))
        val expectedSql = "${query1.asSql()}\nUNION ALL\n${query2.asSql()}\nORDER BY name ASC"
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
}