package com.blipblipcode.query.retrofit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatedQueryParametersTest {

    @Test
    fun `addParameter with a new key`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()

        // When: Adding a new key-value pair
        params.addParameter("name", "John")

        // Then: The map should contain that exact pair
        assertEquals("John", params["name"])
        assertEquals(1, params.size)
    }

    @Test
    fun `addParameter overwriting an existing key`() {
        // Given: A RepeatedQueryParameters with an existing key
        val params = RepeatedQueryParameters.create("name" to "John")

        // When: Adding the same key with a different value
        params.addParameter("name", "Jane")

        // Then: The previous value should be overwritten
        assertEquals("Jane", params["name"])
        assertEquals(1, params.size)
    }

    @Test
    fun `addParameter with various value types`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()

        // When: Adding different value types
        params.addParameter("name", "John")
        params.addParameter("age", 30)
        params.addParameter("height", 1.80)
        params.addParameter("active", true)

        // Then: All types should be stored correctly
        assertEquals("John", params["name"])
        assertEquals(30, params["age"])
        assertEquals(1.80, params["height"])
        assertEquals(true, params["active"])
        assertEquals(4, params.size)
    }

    @Test
    fun `addParameter with an empty key`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()

        // When: Adding a parameter with an empty key
        params.addParameter("", "value")

        // Then: The empty key should be handled correctly
        assertEquals("value", params[""])
        assertEquals(1, params.size)
    }

    @Test
    fun `addParameter with an empty value`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()

        // When: Adding a parameter with an empty value
        params.addParameter("key", "")

        // Then: The empty value should be stored correctly
        assertEquals("", params["key"])
        assertEquals(1, params.size)
    }

    @Test
    fun `addParameter returns the same instance`() {
        // Given: A RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()

        // When: Adding a parameter
        val result = params.addParameter("name", "John")

        // Then: The same instance should be returned for method chaining
        assertEquals(params, result)
    }

    @Test
    fun `addRepeatedParameter with a new key`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()
        val values = listOf("tag1", "tag2", "tag3")

        // When: Adding a list of values for a new key
        params.addRepeatedParameter("tags", values)

        // Then: The list should be stored correctly
        assertEquals(values, params["tags"])
        assertEquals(1, params.size)
    }

    @Test
    fun `addRepeatedParameter overwriting an existing key`() {
        // Given: A RepeatedQueryParameters with an existing key-value pair
        val params = RepeatedQueryParameters.create("tags" to "single_tag")
        val newValues = listOf("tag1", "tag2")

        // When: Adding a list for the same key
        params.addRepeatedParameter("tags", newValues)

        // Then: The previous value should be overwritten with the new list
        assertEquals(newValues, params["tags"])
        assertEquals(1, params.size)
    }

    @Test
    fun `addRepeatedParameter with an empty list`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()
        val emptyList = emptyList<String>()

        // When: Adding an empty list for a key
        params.addRepeatedParameter("tags", emptyList)

        // Then: The empty list should be stored correctly
        assertEquals(emptyList, params["tags"])
        assertEquals(1, params.size)
    }

    @Test
    fun `addRepeatedParameter with a list of mixed types`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()
        val mixedList = listOf("string", 42, true, 3.14)

        // When: Adding a list with mixed data types
        params.addRepeatedParameter("mixed", mixedList)

        // Then: The mixed list should be stored correctly
        assertEquals(mixedList, params["mixed"])
        assertEquals(1, params.size)
    }

    @Test
    fun `addRepeatedParameter with a list containing null values`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()
        val listWithNulls = listOf("value1", null, "value2", null)

        // When: Adding a list containing null elements
        params.addRepeatedParameter("tags", listWithNulls)

        // Then: The list with nulls should be stored correctly in the map
        assertEquals(listWithNulls, params["tags"])
        assertEquals(1, params.size)
    }

    @Test
    fun `addRepeatedParameter with an empty key`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()
        val values = listOf("value1", "value2")

        // When: Adding a repeated parameter with an empty key
        params.addRepeatedParameter("", values)

        // Then: The empty key should be handled correctly
        assertEquals(values, params[""])
        assertEquals(1, params.size)
    }

    @Test
    fun `addRepeatedParameter returns the same instance`() {
        // Given: A RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()
        val values = listOf("value1", "value2")

        // When: Adding a repeated parameter
        val result = params.addRepeatedParameter("tags", values)

        // Then: The same instance should be returned for method chaining
        assertEquals(params, result)
    }

    @Test
    fun `getEntries on an empty map`() {
        // Given: An empty RepeatedQueryParameters instance
        val params = RepeatedQueryParameters.empty()

        // When: Getting the entries
        val entries = params.entries

        // Then: An empty set should be returned
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `getEntries with single value parameters`() {
        // Given: A RepeatedQueryParameters with single-value entries
        val params = RepeatedQueryParameters.create(
            "name" to "John",
            "age" to 30,
            "active" to true
        )

        // When: Getting the entries
        val entries = params.entries

        // Then: The entries should match the original map
        assertEquals(3, entries.size)
        assertTrue(entries.any { it.key == "name" && it.value == "John" })
        assertTrue(entries.any { it.key == "age" && it.value == 30 })
        assertTrue(entries.any { it.key == "active" && it.value == true })
    }

    @Test
    fun `getEntries with a repeated parameter`() {
        // Given: A RepeatedQueryParameters with a list value
        val params = RepeatedQueryParameters.create("tags" to listOf("tag1", "tag2", "tag3"))

        // When: Getting the entries
        val entries = params.entries

        // Then: Multiple entries with the same key should be created
        assertEquals(3, entries.size)
        assertTrue(entries.any { it.key == "tags" && it.value == "tag1" })
        assertTrue(entries.any { it.key == "tags" && it.value == "tag2" })
        assertTrue(entries.any { it.key == "tags" && it.value == "tag3" })
    }

    @Test
    fun `getEntries with a mix of single and repeated parameters`() {
        // Given: A RepeatedQueryParameters with both single values and lists
        val params = RepeatedQueryParameters.create(
            "name" to "John",
            "tags" to listOf("tag1", "tag2"),
            "active" to true
        )

        // When: Getting the entries
        val entries = params.entries

        // Then: Single values should remain as single entries, lists should be expanded
        assertEquals(4, entries.size)
        assertTrue(entries.any { it.key == "name" && it.value == "John" })
        assertTrue(entries.any { it.key == "tags" && it.value == "tag1" })
        assertTrue(entries.any { it.key == "tags" && it.value == "tag2" })
        assertTrue(entries.any { it.key == "active" && it.value == true })
    }

    @Test
    fun `getEntries with a repeated parameter containing nulls`() {
        // Given: A RepeatedQueryParameters with a list containing nulls
        val params = RepeatedQueryParameters.empty()
        params.addRepeatedParameter("tags", listOf("tag1", null, "tag2", null))

        // When: Getting the entries
        val entries = params.entries

        // Then: Null values should be skipped
        assertEquals(2, entries.size)
        assertTrue(entries.any { it.key == "tags" && it.value == "tag1" })
        assertTrue(entries.any { it.key == "tags" && it.value == "tag2" })
    }

    @Test
    fun `getEntries with an empty list value`() {
        // Given: A RepeatedQueryParameters with an empty list
        val params = RepeatedQueryParameters.create("tags" to emptyList<String>())

        // When: Getting the entries
        val entries = params.entries

        // Then: No entries should be created for the empty list
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `getEntries throws exception for a null value`() {
        // Given: A RepeatedQueryParameters with a null value (using reflection to force it)
        val params = RepeatedQueryParameters.empty()
        @Suppress("UNCHECKED_CAST")
        val map = params as MutableMap<String, Any?>
        map["key"] = null

        // When/Then: Getting entries should throw IllegalArgumentException
        assertThrows(IllegalArgumentException::class.java) {
            params.entries
        }
    }

    @Test
    fun `getEntries with special character keys`() {
        // Given: A RepeatedQueryParameters with special character keys
        val params = RepeatedQueryParameters.create(
            "key&with&ampersand" to "value1",
            "key=with=equals" to "value2",
            "key?with?question" to "value3"
        )

        // When: Getting the entries
        val entries = params.entries

        // Then: Special character keys should be handled correctly
        assertEquals(3, entries.size)
        assertTrue(entries.any { it.key == "key&with&ampersand" && it.value == "value1" })
        assertTrue(entries.any { it.key == "key=with=equals" && it.value == "value2" })
        assertTrue(entries.any { it.key == "key?with?question" && it.value == "value3" })
    }

    @Test
    fun `getEntries return set is mutable`() {
        // Given: A RepeatedQueryParameters with some entries
        val params = RepeatedQueryParameters.create("key" to "value")

        // When: Getting the entries
        val entries = params.entries

        // Then: The returned set should contain the expected entry and be of correct size
        assertEquals(1, entries.size)
        assertTrue(entries.any { it.key == "key" && it.value == "value" })
    }

    @Test
    fun `getEntries entry values are mutable`() {
        // Given: A RepeatedQueryParameters with an entry
        val params = RepeatedQueryParameters.create("key" to "original")

        // When: Getting an entry and changing its value
        val entries = params.entries
        val entry = entries.first()
        val oldValue = entry.setValue("modified")

        // Then: The entry value should be changed, but original map should remain unchanged
        assertEquals("original", oldValue)
        assertEquals("modified", entry.value)
        assertEquals("original", params["key"]) // Original map should not be affected
    }

}