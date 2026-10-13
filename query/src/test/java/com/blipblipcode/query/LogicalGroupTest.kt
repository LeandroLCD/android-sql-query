package com.blipblipcode.query

import com.blipblipcode.query.builder.queryGroup
import com.blipblipcode.query.operator.CaseConversion
import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.LogicalType
import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.utils.copyOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicalGroupTest {

    @Test
    fun should_keep_or_conditions_inside_the_and_chain_when_group_is_added_in_asSql() {
        //GIVEN
        val query = queryGroup {
            withWhere("creator", SQLOperator.Equals("creator", 1))
            withAnd("status", SQLOperator.Equals("status", 1))
            withAnd("responsible", SQLOperator.NotEquals("responsible", 1))
            withAndGroup(
                "search",
                LogicalOperation.Or(SQLOperator.Like("doc_number", "cal", CaseConversion.LOWER)),
                LogicalOperation.Or(SQLOperator.Like("terminal_source_display", "cal", CaseConversion.LOWER)),
                LogicalOperation.Or(SQLOperator.Like("terminal_dest_display", "cal", CaseConversion.LOWER))
            )
        }.buildSelect()

        //WHEN
        val actualSql = query.asSql()

        //THEN
        val expectedSql =
            "SELECT * FROM tire_transfer_document WHERE creator = 1 AND status = 1 AND responsible != 1 " +
                "AND (LOWER(doc_number) LIKE LOWER('%cal%') OR LOWER(terminal_source_display) LIKE LOWER('%cal%') " +
                "OR LOWER(terminal_dest_display) LIKE LOWER('%cal%'))"
        assertEquals(expectedSql, actualSql)
    }

    @Test
    fun should_attach_group_with_and_when_and_group_is_added_in_asSql() {
        //GIVEN
        val query = queryGroup {
            withAndGroup(
                "range",
                LogicalOperation.And(SQLOperator.GreaterThan("age", 18)),
                LogicalOperation.And(SQLOperator.LessThan("age", 65))
            )
        }.buildSelect()

        //WHEN
        val actualSql = query.asSql()

        //THEN
        assertEquals("SELECT * FROM tire_transfer_document WHERE creator = 1 AND (age > 18 AND age < 65)", actualSql)
    }

    @Test
    fun should_attach_group_with_or_when_or_group_is_added_in_asSql() {
        //GIVEN
        val query = queryGroup {
            withAnd("status", SQLOperator.Equals("status", "active"))
            withOrGroup(
                "owner",
                LogicalOperation.And(SQLOperator.Equals("role", "admin")),
                LogicalOperation.And(SQLOperator.Equals("role", "root"))
            )
        }.buildSelect()

        //WHEN
        val actualSql = query.asSql()

        //THEN
        assertEquals(
            "SELECT * FROM tire_transfer_document WHERE creator = 1 AND status = 'active' OR (role = 'admin' AND role = 'root')",
            actualSql
        )
    }

    @Test
    fun should_quote_string_values_when_group_is_rendered_in_asString() {
        //GIVEN
        val group = LogicalOperation.Multiple(
            listOf(
                LogicalOperation.And(SQLOperator.Equals("name", "Ada")),
                LogicalOperation.And(SQLOperator.Equals("city", "London"))
            ),
            LogicalType.AND.sql
        )

        //WHEN
        val actualString = group.asString()

        //THEN
        assertEquals("AND (name = 'Ada' AND city = 'London')", actualString)
    }

    @Test
    fun should_render_nested_group_when_group_contains_another_group_in_asString() {
        //GIVEN
        val group = LogicalOperation.Multiple(
            listOf(
                LogicalOperation.And(SQLOperator.Equals("status", "active")),
                LogicalOperation.Multiple(
                    listOf(
                        LogicalOperation.Or(SQLOperator.Equals("role", "admin")),
                        LogicalOperation.Or(SQLOperator.Equals("role", "root"))
                    ),
                    LogicalType.OR.sql
                )
            ),
            LogicalType.AND.sql
        )

        //WHEN
        val actualString = group.asString()

        //THEN
        assertEquals("AND (status = 'active' OR (role = 'admin' OR role = 'root'))", actualString)
    }

    @Test
    fun should_return_every_operator_when_group_is_inspected_in_operators() {
        //GIVEN
        val group = LogicalOperation.Multiple(
            listOf(
                LogicalOperation.Or(SQLOperator.Equals("doc_number", "cal")),
                LogicalOperation.Or(SQLOperator.Equals("terminal_dest_display", "cal"))
            ),
            LogicalType.AND.sql
        )

        //WHEN
        val operators = group.operators()

        //THEN
        assertEquals(
            listOf("doc_number" to "cal", "terminal_dest_display" to "cal"),
            operators.map { it.toPair() }
        )
    }

    @Test
    fun should_throw_IllegalArgumentException_when_group_has_no_conditions_in_andGroup() {
        //GIVEN
        val builder = QuerySelect.builder("users").where(SQLOperator.Equals("id", 1))

        //WHEN
        val result = runCatching { builder.andGroup("search") }

        //THEN
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun should_keep_flat_rendering_when_no_group_is_added_in_asSql() {
        //GIVEN
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .or("role", SQLOperator.Equals("role", "admin"))
            .build()

        //WHEN
        val actualSql = query.asSql()

        //THEN
        assertEquals("SELECT * FROM users WHERE id = 1 AND status = 'active' OR role = 'admin'", actualSql)
    }

    @Test
    fun should_keep_or_conditions_inside_the_and_chain_when_group_is_added_in_asSql_for_delete() {
        //GIVEN
        val query = queryGroup {
            withWhere("creator", SQLOperator.Equals("creator", 1))
            withAndGroup(
                "search",
                LogicalOperation.Or(SQLOperator.Like("doc_number", "cal", CaseConversion.LOWER)),
                LogicalOperation.Or(SQLOperator.Like("terminal_dest_display", "cal", CaseConversion.LOWER))
            )
        }.buildDelete()

        //WHEN
        val actualSql = query.asSql()

        //THEN
        val expectedSql =
            "DELETE FROM tire_transfer_document WHERE creator = 1 " +
                "AND (LOWER(doc_number) LIKE LOWER('%cal%') OR LOWER(terminal_dest_display) LIKE LOWER('%cal%'))"
        assertEquals(expectedSql, actualSql)
    }

    @Test
    fun should_keep_or_conditions_inside_the_and_chain_when_group_is_added_in_asSql_with_predicate() {
        //GIVEN
        val query = queryGroup {
            withAnd("status", SQLOperator.Equals("status", 1))
            withAndGroup(
                "search",
                LogicalOperation.Or(SQLOperator.Like("doc_number", "cal", CaseConversion.LOWER)),
                LogicalOperation.Or(SQLOperator.Like("terminal_dest_display", "cal", CaseConversion.LOWER))
            )
        }.buildSelect()

        //WHEN
        val actualSql = query.asSql { it.column != "status" }

        //THEN
        val expectedSql =
            "SELECT * FROM tire_transfer_document WHERE creator = 1 " +
                "AND (LOWER(doc_number) LIKE LOWER('%cal%') OR LOWER(terminal_dest_display) LIKE LOWER('%cal%'))"
        assertEquals(expectedSql, actualSql)
    }

    @Test
    fun should_clone_grouped_operations_when_query_is_copied_in_copy() {
        //GIVEN
        val query = queryGroup {
            withAndGroup("search", LogicalOperation.Or(SQLOperator.Equals("doc_number", "cal")))
        }.buildSelect()

        //WHEN
        val copy = query.copy()

        //THEN
        val originalOperation = query.getOperations()["search"]
        val copiedOperation = copy.getOperations()["search"]
        assertTrue(originalOperation !== copiedOperation)
        assertEquals(query.asSql(), copy.asSql())
    }

    @Test
    fun should_render_is_null_condition_when_group_contains_it_in_asSql_for_delete() {
        //GIVEN
        val query = QueryDelete.builder("sessions")
            .where(SQLOperator.LessThan("expires_at", 1700000000))
            .andGroup(
                "stale",
                LogicalOperation.Or(SQLOperator.IsNull("refreshed_at")),
                LogicalOperation.Or(SQLOperator.LessThan("refreshed_at", 1600000000))
            )
            .build()

        //WHEN
        val actualSql = query.asSql()

        //THEN
        assertEquals(
            "DELETE FROM sessions WHERE expires_at < 1700000000 " +
                "AND (refreshed_at IS NULL OR refreshed_at < 1600000000)",
            actualSql
        )
    }

    @Test
    fun should_remove_grouped_operation_when_key_is_removed_in_remove() {
        //GIVEN
        val query = queryGroup {
            withAnd("status", SQLOperator.Equals("status", 1))
            withAndGroup("search", LogicalOperation.Or(SQLOperator.Equals("doc_number", "cal")))
        }.buildSelect()

        //WHEN
        val removedQuery = query.remove("search")

        //THEN
        assertEquals("SELECT * FROM tire_transfer_document WHERE creator = 1 AND status = 1", removedQuery.asSql())
    }

    @Test
    fun should_anchor_group_without_connector_when_group_is_the_where_clause_in_asSql() {
        //GIVEN
        val query = QuerySelect.builder("users")
            .where(
                "search",
                LogicalOperation.Multiple(
                    listOf(
                        LogicalOperation.Or(SQLOperator.Equals("role", "admin")),
                        LogicalOperation.Or(SQLOperator.Equals("role", "root"))
                    ),
                    LogicalType.AND.sql
                )
            )
            .and("status", SQLOperator.Equals("status", "active"))
            .build()

        //WHEN
        val actualSql = query.asSql()

        //THEN
        assertEquals("SELECT * FROM users WHERE (role = 'admin' OR role = 'root') AND status = 'active'", actualSql)
    }

    @Test
    fun should_keep_retained_conditions_when_predicate_excludes_one_inside_group_in_asSql() {
        //GIVEN
        val query = queryGroup {
            withAndGroup(
                "search",
                LogicalOperation.Or(SQLOperator.Like("doc_number", "cal", CaseConversion.LOWER)),
                LogicalOperation.Or(SQLOperator.Like("terminal_source_display", "cal", CaseConversion.LOWER)),
                LogicalOperation.Or(SQLOperator.Like("terminal_dest_display", "cal", CaseConversion.LOWER))
            )
        }.buildSelect()

        //WHEN
        val actualSql = query.asSql { it.column != "terminal_dest_display" }

        //THEN
        val expectedSql =
            "SELECT * FROM tire_transfer_document WHERE creator = 1 " +
                "AND (LOWER(doc_number) LIKE LOWER('%cal%') OR LOWER(terminal_source_display) LIKE LOWER('%cal%'))"
        assertEquals(expectedSql, actualSql)
    }

    @Test
    fun should_drop_group_when_predicate_excludes_every_condition_in_asSql() {
        //GIVEN
        val query = queryGroup {
            withAndGroup("search", LogicalOperation.Or(SQLOperator.Equals("doc_number", "cal")))
        }.buildSelect()

        //WHEN
        val actualSql = query.asSql { false }

        //THEN
        assertEquals("SELECT * FROM tire_transfer_document", actualSql)
    }

    @Test
    fun should_promote_first_survivor_when_where_clause_is_filtered_out_in_asSql() {
        //GIVEN
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("creator", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()

        //WHEN
        val actualSql = query.asSql { it.column != "creator" }

        //THEN
        assertEquals("SELECT * FROM users WHERE status = 'active'", actualSql)
    }

    @Test
    fun should_promote_first_survivor_when_where_clause_is_filtered_out_in_asSql_for_delete() {
        //GIVEN
        val query = QueryDelete.builder("users")
            .where(SQLOperator.Equals("creator", 1))
            .andGroup("search", LogicalType.OR, SQLOperator.Equals("role", "admin"))
            .build()

        //WHEN
        val actualSql = query.asSql { it.column != "creator" }

        //THEN
        assertEquals("DELETE FROM users WHERE (role = 'admin')", actualSql)
    }

    @Test
    fun should_list_every_operator_of_nested_group_when_group_is_inspected_in_operators() {
        //GIVEN
        val nested = LogicalOperation.Multiple(
            listOf(
                LogicalOperation.And(SQLOperator.Equals("status", "active")),
                LogicalOperation.Multiple(
                    LogicalOperation.Or(SQLOperator.Equals("role", "admin")),
                    LogicalOperation.Or(SQLOperator.Equals("role", "root"))
                )
            ),
            LogicalType.AND.sql
        )

        //WHEN
        val operators = nested.operators()

        //THEN
        assertEquals(
            listOf("status" to "active", "role" to "admin", "role" to "root"),
            operators.map { it.toPair() }
        )
    }

    @Test
    fun should_list_every_operator_of_group_when_query_is_exposed_in_getSqlOperators() {
        //GIVEN
        val query = queryGroup {
            withAndGroup(
                "search",
                LogicalOperation.Or(SQLOperator.Equals("doc_number", "cal")),
                LogicalOperation.Or(SQLOperator.Equals("terminal_dest_display", "cal"))
            )
        }.buildSelect()

        //WHEN
        val columns = query.getSqlOperators().map { it.column }

        //THEN
        assertEquals(listOf("creator", "doc_number", "terminal_dest_display"), columns)
    }

    @Test
    fun should_throw_IllegalArgumentException_when_symbol_is_not_a_connector_in_Multiple() {
        //GIVEN
        val conditions = listOf(LogicalOperation.And(SQLOperator.Equals("a", 1)))

        //WHEN
        val result = runCatching { LogicalOperation.Multiple(conditions, "XOR") }

        //THEN
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun should_link_conditions_with_connector_when_typed_group_is_added_in_andGroup() {
        //GIVEN
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .andGroup(
                "search",
                LogicalType.OR,
                SQLOperator.Like("doc_number", "cal"),
                SQLOperator.Like("terminal_dest_display", "cal")
            )
            .build()

        //WHEN
        val actualSql = query.asSql()

        //THEN
        assertEquals(
            "SELECT * FROM users WHERE status = 'active' AND (doc_number LIKE '%cal%' OR terminal_dest_display LIKE '%cal%')",
            actualSql
        )
    }

    @Test
    fun should_add_group_to_built_query_when_andGroup_is_called_on_instance() {
        //GIVEN
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()

        //WHEN
        val grouped = query.andGroup("search", LogicalType.OR, SQLOperator.Equals("role", "admin"))

        //THEN
        assertEquals(
            "SELECT * FROM users WHERE status = 'active' AND (role = 'admin')",
            grouped.asSql()
        )
    }

    @Test
    fun should_keep_grouped_where_when_query_is_converted_in_toQueryDelete() {
        //GIVEN
        val query = QuerySelect.builder("users")
            .where(
                "search",
                LogicalOperation.Multiple(
                    LogicalOperation.Or(SQLOperator.Equals("role", "admin")),
                    LogicalOperation.Or(SQLOperator.Equals("role", "root"))
                )
            )
            .and("status", SQLOperator.Equals("status", "active"))
            .build()

        //WHEN
        val actualSql = query.toQueryDelete().asSql()

        //THEN
        assertEquals(
            "DELETE FROM users WHERE (role = 'admin' OR role = 'root') AND status = 'active'",
            actualSql
        )
    }

    @Test
    fun should_replace_first_leaf_when_group_is_copied_in_copyOperation() {
        //GIVEN
        val nested = LogicalOperation.Multiple(
            listOf(
                LogicalOperation.And(SQLOperator.Equals("status", "active")),
                LogicalOperation.Multiple(
                    LogicalOperation.Or(SQLOperator.Equals("role", "admin")),
                    LogicalOperation.Or(SQLOperator.Equals("role", "root"))
                )
            ),
            LogicalType.AND.sql
        )

        //WHEN
        val copied = nested.copyOperation(SQLOperator.Equals("status", "archived"))

        //THEN
        assertEquals("AND (status = 'archived' AND (role = 'admin' OR role = 'root'))", copied.asString())
    }
}