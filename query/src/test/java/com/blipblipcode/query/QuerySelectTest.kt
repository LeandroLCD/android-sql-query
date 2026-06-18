package com.blipblipcode.query

import com.blipblipcode.query.operator.CaseConversion
import com.blipblipcode.query.operator.LogicalOperation
import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.utils.asSQLiteQuery
import com.blipblipcode.query.utils.copyOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuerySelectTest {

    @Test
    fun should_new_instance_when_copying_in_transformOperation() {

        val key = "status"

        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and(key, SQLOperator.Equals(key, "active"))
            .build()
        val cloneQuery = query.newBuilder {
            it.transformOperation(key = key) { op ->
                op.copyOperation(SQLOperator.NotEquals(key, "active"))
            }
        }.build()

        assertNotEquals(query.hashCode(), cloneQuery.hashCode())
        assertNotEquals(query.asSql(), cloneQuery.asSql())
        assertTrue(cloneQuery.getSqlOperation(key) is SQLOperator.NotEquals)
    }

    @Test
    fun should_create_new_instance_when_copying_in_copy(){
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val cloneQuery = query.copy()

        assertNotEquals(query.hashCode(), cloneQuery.hashCode())
    }

    @Test
    fun should_maintain_state_and_create_new_instance_when_consumer_is_empty_in_newBuilder() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()

        val newQuery = query.newBuilder { }.build()

        assertEquals(query.asSql(), newQuery.asSql())
        assertTrue("La nueva consulta debería ser una instancia diferente", query !== newQuery)
        
        val originalOp = query.getSqlOperation("status")
        val newOp = newQuery.getSqlOperation("status")
        assertTrue("Las operaciones internas también deberían ser instancias diferentes (clones)", originalOp !== newOp)
    }

    @Test
    fun should_remove_operation_when_key_exists_in_remove() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()
        query.remove("status")
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_do_nothing_when_key_does_not_exist_in_remove() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val originalSql = query.asSql()
        query.remove("non_existent_key")
        assertEquals(originalSql, query.asSql())
    }

    @Test
    fun should_remove_operation_when_key_is_empty_in_remove() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("", SQLOperator.Equals("status", "active"))
            .build()
        query.remove("")
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_do_nothing_when_operations_map_is_empty_in_remove() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val originalSql = query.asSql()
        query.remove("any_key")
        assertEquals(originalSql, query.asSql())
    }

    @Test
    fun should_return_same_instance_for_chaining_in_remove() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()
        val instance = query.remove("status")
        assertEquals(query, instance)
    }

    @Test
    fun should_replace_existing_clause_with_new_operator_in_setWhere() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.setWhere(SQLOperator.Equals("id", 2))
        val expectedSql = "SELECT * FROM users WHERE id = 2"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_replace_existing_clause_with_different_operator_type_in_setWhere() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.setWhere(SQLOperator.GreaterThan("age", 30))
        val expectedSql = "SELECT * FROM users WHERE age > 30"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_return_same_instance_for_chaining_in_setWhere() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val instance = query.setWhere(SQLOperator.Equals("id", 2))
        assertEquals(query, instance)
    }

    @Test
    fun should_add_new_logical_operation_when_key_is_new_in_addLogicalOperation() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.addLogicalOperation("status", LogicalOperation.And(SQLOperator.Equals("status", "active")))
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun should_overwrite_logical_operation_when_key_is_duplicate_in_addLogicalOperation() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "inactive"))
            .build()
        query.addLogicalOperation("status", LogicalOperation.And(SQLOperator.Equals("status", "active")))
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun should_add_logical_operation_when_key_is_empty_in_addLogicalOperation() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.addLogicalOperation("", LogicalOperation.And(SQLOperator.Equals("status", "active")))
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun should_add_various_logical_operation_types_in_addLogicalOperation() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
            .addLogicalOperation("age", LogicalOperation.Or(SQLOperator.GreaterThan("age", 30)))
        val expectedSql = "SELECT * FROM users WHERE id = 1 OR age > 30"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun should_set_multiple_fields_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val newQuery = query.setFields("name", "age")
        val expectedSql = "SELECT name, age FROM users WHERE id = 1"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun should_set_single_field_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val newQuery = query.setFields("name")
        val expectedSql = "SELECT name FROM users WHERE id = 1"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun should_reset_to_all_fields_when_no_arguments_provided_in_setFields() {
        val queryWithFields = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
            .setFields("name", "age")

        val newQuery = queryWithFields.setFields()
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun should_maintain_immutability_of_original_instance_in_setFields() {
        val originalQuery = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val originalSql = originalQuery.asSql()

        originalQuery.setFields("name", "age") // This returns a new instance

        assertEquals(originalSql, originalQuery.asSql()) // Original instance is unchanged
    }

    @Test
    fun should_handle_empty_or_blank_field_names_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val newQuery = query.setFields("", " ")
        val expectedSql = "SELECT ,   FROM users WHERE id = 1"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun should_generate_sql_with_basic_where_clause_and_all_fields_in_asSql() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_generate_sql_without_where_clause_in_asSql() {
        val query = QuerySelect.builder("users")
            .build()
        val expectedSql = "SELECT * FROM users"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_generate_sql_with_uppercase_conversion_in_asSql() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active", caseConversion = CaseConversion.UPPER))
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND UPPER(status) = UPPER('active')"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun should_generate_sql_with_specific_fields_in_asSql() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("name", "email")
            .build()
        val expectedSql = "SELECT name, email FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_generate_sql_with_single_logical_operation_in_asSql() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun should_generate_sql_with_multiple_logical_operations_in_asSql() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .and("status", SQLOperator.Equals("status", "active"))
            .or("role", SQLOperator.Equals("role", "admin"))
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1 AND status = 'active' OR role = 'admin'"
        assertEquals(expectedSql, query.asSql())
    }

    @Test
    fun should_handle_special_characters_in_table_or_field_names_in_asSql() {
        val query = QuerySelect.builder("user table")
            .where(SQLOperator.Equals("user id", 1))
            .setFields("first name", "last name")
            .build()
        val expectedSql = "SELECT first name, last name FROM user table WHERE user id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_reflect_removed_operation_in_asSql() {
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
    fun should_reflect_changed_where_clause_in_asSql() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        query.setWhere(SQLOperator.GreaterThan("age", 18))
        val expectedSql = "SELECT * FROM users WHERE age > 18"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_explicitly_set_empty_fields_in_asSql() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields()
            .build()
        val expectedSql = "SELECT * FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_convert_to_support_sqlite_query_in_asSQLiteQuery() {
        val querySelect = QuerySelect.builder("users")
            .where(SQLOperator.Equals("name", "John"))
            .build()

        val supportSQLiteQuery = querySelect.asSQLiteQuery()

        assertEquals(querySelect.asSql().trim(), supportSQLiteQuery.sql)
        assertEquals(0, supportSQLiteQuery.argCount)
    }

    @Test
    fun should_limit_results_with_single_value_in_limit() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_limit_results_with_offset_in_limit() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(5, 10)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 5 OFFSET 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_return_same_instance_for_chaining_in_limit() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10)
            .build()
        val instance = query.limit(5, 10)
        assertEquals(query, instance)
    }

    @Test
    fun should_handle_various_limit_and_offset_conditions_in_limit() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10, 5)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 10 OFFSET 5"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_zero_offset_in_limit() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10, 0)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_zero_limit_in_limit() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(0)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 0"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_negative_limit_in_limit() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(-5)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT -5"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_negative_offset_in_limit() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10, -5)
            .build()
        val expectedSql = "SELECT * FROM users WHERE status = 'active' LIMIT 10 OFFSET -5"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_order_by_ascending_in_orderBy() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Asc("name"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY name ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_order_by_descending_in_orderBy() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Desc("created_at"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY created_at DESC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_order_by_multiple_columns_ascending_in_orderBy() {
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
    fun should_order_by_multiple_columns_mixed_in_orderBy() {
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
    fun should_order_by_descending_then_ascending_in_orderBy() {
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
    fun should_order_by_ascending_and_descending_columns_in_orderBy() {
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
    fun should_order_by_ascending_with_limit_in_orderBy() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10)
            .build()
        query.orderBy(OrderBy.Asc("name"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY name ASC LIMIT 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_order_by_descending_with_limit_and_offset_in_orderBy() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(5, 10)
            .build()
        query.orderBy(OrderBy.Desc("created_at"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY created_at DESC LIMIT 5 OFFSET 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_return_same_instance_for_chaining_in_orderBy() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        val instance = query.orderBy(OrderBy.Asc("name"))
        assertEquals(query, instance)
    }

    @Test
    fun should_replace_previous_order_in_orderBy() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Desc("created_at"))
        query.orderBy(OrderBy.Asc("name"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY name ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_remove_ordering_when_null_provided_in_orderBy() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(null)
        val expectedSql = "SELECT * FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_order_by_three_columns_mixed_in_orderBy() {
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
    fun should_handle_special_characters_in_column_names_in_orderBy() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .build()
        query.orderBy(OrderBy.Asc("first name"))
        val expectedSql = "SELECT * FROM users WHERE status = 'active' ORDER BY first name ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_multiple_special_character_column_names_in_orderBy() {
        val query = QuerySelect.builder("user table")
            .where(SQLOperator.Equals("user id", 1))
            .build()
        query.orderBy(OrderBy.Multiple(listOf(
            OrderBy.Asc("first name"),
            OrderBy.Desc("last name")
        )))
        val expectedSql = "SELECT * FROM user table WHERE user id = 1 ORDER BY first name ASC, last name DESC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_order_ascending_with_multiple_logical_operations_in_orderBy() {
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
    fun should_order_descending_with_multiple_logical_operations_in_orderBy() {
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
    fun should_order_multiple_with_specific_fields_in_orderBy() {
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
    fun should_handle_single_field_alias_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("name AS full_name")
            .build()
        val expectedSql = "SELECT name AS full_name FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_multiple_field_aliases_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("name AS full_name", "email AS user_email", "created_at AS registration_date")
            .build()
        val expectedSql = "SELECT name AS full_name, email AS user_email, created_at AS registration_date FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_mixed_fields_and_aliases_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("id", "name AS full_name", "email")
            .build()
        val expectedSql = "SELECT id, name AS full_name, email FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_function_with_alias_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("COUNT(*) AS total_users", "name AS user_name")
            .build()
        val expectedSql = "SELECT COUNT(*) AS total_users, name AS user_name FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_uppercase_alias_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("name AS NAME", "email AS EMAIL")
            .build()
        val expectedSql = "SELECT name AS NAME, email AS EMAIL FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_backtick_quoted_alias_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("name AS full name", "email AS user email")
            .build()
        val expectedSql = "SELECT name AS full name, email AS user email FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_table_prefix_and_alias_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("users.id", 1))
            .setFields("users.name AS full_name", "users.email AS user_email")
            .build()
        val expectedSql = "SELECT users.name AS full_name, users.email AS user_email FROM users WHERE users.id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_work_with_aliases_and_orderBy_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("name AS full_name", "created_at AS registration_date")
            .build()
        query.orderBy(OrderBy.Asc("full_name"))
        val expectedSql = "SELECT name AS full_name, created_at AS registration_date FROM users WHERE status = 'active' ORDER BY full_name ASC"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_work_with_aliases_and_limit_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("name AS full_name", "email AS user_email")
            .limit(10)
            .build()
        val expectedSql = "SELECT name AS full_name, email AS user_email FROM users WHERE status = 'active' LIMIT 10"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_work_with_aliases_orderBy_and_limit_combined_in_setFields() {
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
    fun should_handle_CASE_statement_with_alias_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .setFields("CASE WHEN status = 'active' THEN 'Active User' ELSE 'Inactive' END AS user_status")
            .build()
        val expectedSql = "SELECT CASE WHEN status = 'active' THEN 'Active User' ELSE 'Inactive' END AS user_status FROM users WHERE id = 1"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_aggregate_functions_with_aliases_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("COUNT(*) AS total", "SUM(salary) AS total_salary", "AVG(salary) AS average_salary")
            .build()
        val expectedSql = "SELECT COUNT(*) AS total, SUM(salary) AS total_salary, AVG(salary) AS average_salary FROM users WHERE status = 'active'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_handle_mathematical_expression_with_alias_in_setFields() {
        val query = QuerySelect.builder("products")
            .where(SQLOperator.Equals("category", "electronics"))
            .setFields("name", "price", "price * 0.1 AS discount_amount")
            .build()
        val expectedSql = "SELECT name, price, price * 0.1 AS discount_amount FROM products WHERE category = 'electronics'"
        assertEquals(expectedSql, query.asSql().trim())
    }

    @Test
    fun should_maintain_immutability_when_using_aliases_in_setFields() {
        val originalQuery = QuerySelect.builder("users")
            .where(SQLOperator.Equals("id", 1))
            .build()
        val originalSql = originalQuery.asSql()

        originalQuery.setFields("name AS full_name", "email AS user_email")

        assertEquals(originalSql, originalQuery.asSql())
    }

    @Test
    fun should_replace_previous_fields_with_aliases_in_setFields() {
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .setFields("id", "name")
            .build()
        val newQuery = query.setFields("name AS full_name", "email AS user_email", "created_at AS registration_date")
        val expectedSql = "SELECT name AS full_name, email AS user_email, created_at AS registration_date FROM users WHERE status = 'active'"
        assertEquals(expectedSql, newQuery.asSql().trim())
    }

    @Test
    fun should_handle_multiple_aliases_using_builder_in_setFields() {
        val query = QuerySelect.builder("employees")
            .where(SQLOperator.Equals("department", "sales"))
            .setFields("employee_id AS id", "first_name AS fname", "last_name AS lname", "salary AS monthly_salary")
            .build()
        val expectedSql = "SELECT employee_id AS id, first_name AS fname, last_name AS lname, salary AS monthly_salary FROM employees WHERE department = 'sales'"
        assertEquals(expectedSql, query.asSql().trim())
    }
}
