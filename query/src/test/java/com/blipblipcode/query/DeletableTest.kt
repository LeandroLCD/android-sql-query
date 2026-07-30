package com.blipblipcode.query

import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.utils.toDelete
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletableTest {

    @Test
    fun should_build_query_delete_with_same_table_and_where_clause_when_converting_query_select_in_to_query_delete() {
        //GIVEN
        val select = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()

        //WHEN
        val delete = select.toQueryDelete()

        //THEN
        assertEquals("users", delete.getTableName())
        assertEquals("DELETE FROM users WHERE id = 1", delete.asSql())
    }

    @Test
    fun should_build_query_delete_with_and_clauses_when_converting_query_select_with_and_in_to_query_delete() {
        //GIVEN
        val select = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()

        //WHEN
        val delete = select.toQueryDelete()

        //THEN
        assertEquals(
            "DELETE FROM users WHERE id = 1 AND status = 'active'",
            delete.asSql()
        )
    }

    @Test
    fun should_build_query_delete_with_or_clauses_when_converting_query_select_with_or_in_to_query_delete() {
        //GIVEN
        val select = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .or("role", SQLOperator.Equals("role", "guest"))
            .build()

        //WHEN
        val delete = select.toQueryDelete()

        //THEN
        assertEquals(
            "DELETE FROM users WHERE id = 1 OR role = 'guest'",
            delete.asSql()
        )
    }

    @Test
    fun should_throw_illegal_argument_exception_when_query_select_has_no_where_clause_in_to_query_delete() {
        //GIVEN
        val select = QuerySelect.builder("users").build()

        //WHEN
        val result = runCatching { select.toQueryDelete() }

        //THEN
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun should_ignore_order_by_and_limit_when_converting_query_select_in_to_query_delete() {
        //GIVEN
        val select = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .orderBy(com.blipblipcode.query.operator.OrderBy.Asc("name"))
            .limit(10)
            .build()

        //WHEN
        val delete = select.toQueryDelete()

        //THEN
        assertEquals("DELETE FROM users WHERE id = 1", delete.asSql())
    }

    @Test
    fun should_resolve_to_query_delete_when_calling_to_delete_extension_on_query_select_in_to_delete() {
        //GIVEN
        val deletable: Deletable = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()

        //WHEN
        val delete = deletable.toDelete()

        //THEN
        assertEquals("users", delete.getTableName())
        assertEquals("DELETE FROM users WHERE id = 1", delete.asSql())
    }

    @Test
    fun should_throw_illegal_argument_exception_when_calling_to_delete_extension_on_union_query_without_target_in_to_delete() {
        //GIVEN
        val q1 = QuerySelect.builder("table1").where(SQLOperator.Equals("id", 1)).build()
        val q2 = QuerySelect.builder("table2").where(SQLOperator.Equals("id", 2)).build()
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()
        val deletable: Deletable = unionQuery

        //WHEN
        val result = runCatching { deletable.toDelete() }

        //THEN
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun should_build_query_delete_with_in_subquery_when_converting_union_query_in_to_query_delete() {
        //GIVEN
        val q1 = QuerySelect.builder("table1").where(SQLOperator.Equals("id", 1)).build()
        val q2 = QuerySelect.builder("table2").where(SQLOperator.Equals("id", 2)).build()
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        val delete = unionQuery.toQueryDelete(targetTable = "users", keyColumn = "id")

        //THEN
        assertEquals("users", delete.getTableName())
        val sql = delete.asSql()
        assertTrue(sql.startsWith("DELETE FROM users WHERE id IN ("))
        assertTrue(sql.contains("SELECT * FROM ("))
        assertTrue(sql.contains("\nUNION\n"))
        assertTrue(sql.endsWith(")"))
    }

    @Test
    fun should_build_query_delete_with_in_subquery_when_calling_to_delete_extension_on_union_query_with_target_in_to_delete() {
        //GIVEN
        val q1 = QuerySelect.builder("table1").where(SQLOperator.Equals("id", 1)).build()
        val q2 = QuerySelect.builder("table2").where(SQLOperator.Equals("id", 2)).build()
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        val delete = unionQuery.toDelete(targetTable = "users", keyColumn = "id")

        //THEN
        assertEquals("users", delete.getTableName())
        assertTrue(delete.asSql().startsWith("DELETE FROM users WHERE id IN ("))
    }

    @Test
    fun should_throw_illegal_argument_exception_when_union_query_builder_has_less_than_two_queries_in_build() {
        //GIVEN
        val q1 = QuerySelect.builder("table1").where(SQLOperator.Equals("id", 1)).build()
        val builder = UnionQuery.builder(q1)

        //WHEN
        val result = runCatching { builder.build() }

        //THEN
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun should_keep_inner_order_by_outside_the_subquery_when_converting_union_query_with_order_by_in_to_query_delete() {
        //GIVEN
        val q1 = QuerySelect.builder("table1")
            .where(SQLOperator.Equals("id", 1))
            .orderBy(com.blipblipcode.query.operator.OrderBy.Asc("name"))
            .build()
        val q2 = QuerySelect.builder("table2")
            .where(SQLOperator.Equals("id", 2))
            .build()
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        val sql = unionQuery.toQueryDelete(targetTable = "users", keyColumn = "id").asSql()

        //THEN
        assertTrue(sql.contains("ORDER BY name ASC"))
    }

    @Test
    fun should_not_modify_source_queries_when_converting_union_query_in_to_query_delete() {
        //GIVEN
        val q1 = QuerySelect.builder("table1").where(SQLOperator.Equals("id", 1)).build()
        val q2 = QuerySelect.builder("table2").where(SQLOperator.Equals("id", 2)).build()
        val unionQuery = UnionQuery.builder(q1).addQuery(q2).build()

        //WHEN
        unionQuery.toQueryDelete(targetTable = "users", keyColumn = "id")

        //THEN
        assertEquals("SELECT * FROM table1 WHERE id = 1", q1.asSql())
        assertEquals("SELECT * FROM table2 WHERE id = 2", q2.asSql())
    }
}