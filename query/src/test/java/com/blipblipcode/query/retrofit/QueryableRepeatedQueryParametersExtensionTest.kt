package com.blipblipcode.query.retrofit

import com.blipblipcode.query.QuerySelect
import com.blipblipcode.query.operator.SQLOperator
import com.blipblipcode.query.utils.asQueryRepeatedQueryParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryableRepeatedQueryParametersExtensionTest {

    @Test
    fun `asQueryRepeatedQueryParameters with simple operators`() {
        // Given: A QuerySelect with simple SQL operators
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("name", "John"))
            .and("age", SQLOperator.GreaterThan("age", 25))
            .and("status", SQLOperator.Equals("status", "active"))
            .build()

        // When: Converting to RepeatedQueryParameters
        val params = query.asQueryRepeatedQueryParameters()

        // Then: All operators should be converted to parameters
        assertEquals("John", params["name"])
        assertEquals(25, params["age"])
        assertEquals("active", params["status"])
        assertEquals(3, params.size)
    }

    @Test
    fun `asQueryRepeatedQueryParameters with list operators`() {
        // Given: A QuerySelect with operators that contain lists
        val query = QuerySelect.builder("products")
            .where(SQLOperator.In("category", listOf("Electronics", "Computers", "Phones")))
            .and("brand", SQLOperator.Equals("brand", "Apple"))
            .build()

        // When: Converting to RepeatedQueryParameters
        val params = query.asQueryRepeatedQueryParameters()

        // Then: Lists should be expanded as repeated parameters
        val categoryList = params["category"] as List<*>
        assertEquals(listOf("Electronics", "Computers", "Phones"), categoryList)
        assertEquals("Apple", params["brand"])
        assertEquals(2, params.size)
    }

    @Test
    fun `asQueryRepeatedQueryParameters with predicate filter`() {
        // Given: A QuerySelect with multiple operators
        val query = QuerySelect.builder("orders")
            .where(SQLOperator.Equals("customer_id", 123))
            .and("status", SQLOperator.In("status", listOf("pending", "processing")))
            .and("total", SQLOperator.GreaterThan("total", 100.0))
            .build()

        // When: Converting to RepeatedQueryParameters with a predicate that excludes "total"
        val params = query.asQueryRepeatedQueryParameters { (key, _) -> key != "total" }

        // Then: Only filtered parameters should be included
        assertEquals(123, params["customer_id"])
        val statusList = params["status"] as List<*>
        assertEquals(listOf("pending", "processing"), statusList)
        assertTrue(!params.containsKey("total"))
        assertEquals(2, params.size)
    }

    @Test
    fun `asQueryRepeatedQueryParameters with empty query`() {
        // Given: An empty QuerySelect with no operators
        val query = QuerySelect.builder("users").build()

        // When: Converting to RepeatedQueryParameters
        val params = query.asQueryRepeatedQueryParameters()

        // Then: An empty RepeatedQueryParameters should be returned
        assertTrue(params.isEmpty())
    }

    @Test
    fun `asQueryRepeatedQueryParameters ignores null values`() {
        // Given: A QuerySelect with operators that have null values (like IsNull with Unit)
        val query = QuerySelect.builder("users")
            .where(SQLOperator.Equals("name", "John"))
            .and("description", SQLOperator.IsNull("description"))
            .build()

        // When: Converting to RepeatedQueryParameters
        val params = query.asQueryRepeatedQueryParameters()

        // Then: Only non-null values should be included, null values should be ignored
        assertEquals("John", params["name"])
        // IsNull operator with Unit/null value should be completely ignored
        assertTrue("description key should not be present", !params.containsKey("description"))
        assertEquals(1, params.size)
    }

    @Test
    fun `asQueryRepeatedQueryParameters with mixed null and non-null values`() {
        // Given: A QuerySelect with mix of null and non-null operators
        val query = QuerySelect.builder("products")
            .where(SQLOperator.Equals("name", "Product1"))
            .and("category", SQLOperator.In("category", listOf("A", "B")))
            .and("description", SQLOperator.IsNull("description"))
            .and("price", SQLOperator.GreaterThan("price", 100))
            .build()

        // When: Converting to RepeatedQueryParameters
        val params = query.asQueryRepeatedQueryParameters()

        // Then: Only non-null values should be included
        assertEquals("Product1", params["name"])
        assertEquals(listOf("A", "B"), params["category"])
        assertEquals(100, params["price"])
        assertTrue("description key should not be present", !params.containsKey("description"))
        assertEquals(3, params.size)
    }

    @Test
    fun `asQueryRepeatedQueryParameters entries expansion`() {
        // Given: A QuerySelect with mixed single values and lists
        val query = QuerySelect.builder("products")
            .where(SQLOperator.Equals("brand", "Samsung"))
            .and("colors", SQLOperator.In("colors", listOf("red", "blue", "green")))
            .build()

        // When: Converting to RepeatedQueryParameters and getting entries
        val params = query.asQueryRepeatedQueryParameters()
        val entries = params.entries

        // Then: Lists should be expanded in entries while single values remain single
        assertEquals(4, entries.size) // 1 for brand + 3 for colors
        assertTrue(entries.any { it.key == "brand" && it.value == "Samsung" })
        assertTrue(entries.any { it.key == "colors" && it.value == "red" })
        assertTrue(entries.any { it.key == "colors" && it.value == "blue" })
        assertTrue(entries.any { it.key == "colors" && it.value == "green" })
    }
}
