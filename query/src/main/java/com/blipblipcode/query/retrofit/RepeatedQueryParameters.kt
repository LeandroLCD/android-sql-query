package com.blipblipcode.query.retrofit


/**
 * A custom HashMap implementation that handles repeated query parameters for Retrofit.
 *
 * This class allows you to pass lists as values in @QueryMap, and they will be
 * automatically expanded into multiple query parameters with the same key.
 *
 * Example:
 * ```
 * val params = RepeatedQueryParameters.create(
 *     "terminal" to 2,
 *     "tire_status" to listOf(3, 4),
 *     "category" to "active"
 * )
 * // Results in: ?terminal=2&tire_status=3&tire_status=4&category=active
 * ```
 */
class RepeatedQueryParameters private constructor(
    m: MutableMap<String, Any>
) : LinkedHashMap<String, Any>(m) {

    companion object {
        /**
         * Creates a RepeatedQueryParameters instance with the given key-value pairs.
         *
         * @param pairs Variable number of key-value pairs where values can be:
         *              - Single values (String, Int, Boolean, etc.)
         *              - Lists (will be expanded into multiple parameters)
         */
        fun create(vararg pairs: Pair<String, Any>): RepeatedQueryParameters {
            return RepeatedQueryParameters(linkedMapOf(*pairs))
        }

        /**
         * Creates a RepeatedQueryParameters instance from an existing map.
         */
        fun fromMap(map: MutableMap<String, Any>): RepeatedQueryParameters {
            return RepeatedQueryParameters(LinkedHashMap(map))
        }

        /**
         * Creates an empty RepeatedQueryParameters instance.
         */
        fun empty(): RepeatedQueryParameters {
            return RepeatedQueryParameters(LinkedHashMap())
        }
    }

    /**
     * Adds a single parameter to the query map.
     */
    fun addParameter(key: String, value: Any): RepeatedQueryParameters {
        this[key] = value
        return this
    }

    /**
     * Adds multiple values for the same parameter key.
     */
    fun addRepeatedParameter(key: String, values: List<*>): RepeatedQueryParameters {
        this[key] = values
        return this
    }

    /**
     * Overrides the entries property to handle list values by expanding them
     * into multiple entries with the same key.
     */
    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
        get() {
            val originSet: Set<Map.Entry<String?, Any?>> = super.entries
            val newSet: MutableSet<MutableMap.MutableEntry<String, Any>> = HashSet()

            for ((key, entryValue) in originSet) {
                val entryKey = key ?: throw IllegalArgumentException(
                    "Query map contained null key."
                )

                // Skip null values
                requireNotNull(entryValue) {
                    "Query map contained null value for key '$entryKey'."
                }

                when (entryValue) {
                    is List<*> -> {
                        // Expand list into multiple entries with the same key
                        for (arrayValue in entryValue) {
                            if (arrayValue != null) { // Skip null values in list
                                val newEntry: MutableMap.MutableEntry<String, Any> =
                                    SimpleEntry(entryKey, arrayValue)
                                newSet.add(newEntry)
                            }
                        }
                    }
                    else -> {
                        // Single value entry
                        val newEntry: MutableMap.MutableEntry<String, Any> =
                            SimpleEntry(entryKey, entryValue)
                        newSet.add(newEntry)
                    }
                }
            }

            return newSet
        }

    /**
     * Simple implementation of Map.Entry for creating new entries.
     */
    private class SimpleEntry<K, V>(
        private val keySimple: K,
        private var valueSimple: V
    ) : MutableMap.MutableEntry<K, V> {

        override val key: K get() = keySimple
        override val value: V get() = valueSimple

        override fun setValue(newValue: V): V {
            val oldValue = valueSimple
            valueSimple = newValue
            return oldValue
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Map.Entry<*, *>) return false
            return key == other.key && value == other.value
        }

        override fun hashCode(): Int {
            return (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
        }

        override fun toString(): String = "$key=$value"
    }
}