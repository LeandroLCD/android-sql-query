package com.blipblipcode.query.operator

/**
 * Represents how a column expression should be collated when used in SQL ORDER BY.
 * Implementations append a COLLATE clause (e.g. COLLATE NOCASE) or wrap the
 * expression in a recognised SQL collation function (e.g. RTRIM(column)).
 *
 * Arbitrary transformations such as REPLACE or LOWER should be passed via the
 * `transform` parameter of [OrderBy.Asc] / [OrderBy.Desc] instead.
 */
sealed interface Collation {
    /** Apply this collation to the given SQL expression. */
    fun apply(expression: String): String

    /**
     * Indicates whether this Collation produces a COLLATE suffix (e.g. " COLLATE NOCASE").
     * Wrapper-style collations (like RTRIM) return false so they are
     * applied before any COLLATE suffixes. Default is false.
     */
    fun isCollateSuffix(): Boolean = false

    object NONE : Collation {
        override fun apply(expression: String): String = expression
        override fun toString(): String = "NONE"
    }

    object NOCASE : Collation {
        override fun apply(expression: String): String = "$expression COLLATE NOCASE"
        override fun isCollateSuffix(): Boolean = true
        override fun toString(): String = "NOCASE"
    }

    object BINARY : Collation {
        override fun apply(expression: String): String = "$expression COLLATE BINARY"
        override fun isCollateSuffix(): Boolean = true
        override fun toString(): String = "BINARY"
    }

    object RTRIM : Collation {
        override fun apply(expression: String): String = "RTRIM($expression)"
        override fun toString(): String = "RTRIM"
    }

    data class CustomCollate(val collateName: String) : Collation {
        override fun apply(expression: String): String = "$expression COLLATE $collateName"
        override fun isCollateSuffix(): Boolean = true
        override fun toString(): String = collateName
    }

    /**
     * Compose multiple Collation instances and apply them sequentially.
     * Wrapper-style collations (e.g. RTRIM) are always applied before
     * COLLATE-suffix collations (e.g. NOCASE, BINARY) regardless of
     * the order they appear in [parts].
     *
     * Example: Composite(listOf(Collation.RTRIM, Collation.NOCASE))
     * produces RTRIM(expr) COLLATE NOCASE
     */
    data class Composite(val parts: List<Collation>) : Collation {
        constructor(vararg parts: Collation) : this(parts.toList())
        override fun apply(expression: String): String {
            val (wrappers, suffixes) = parts.partition { !it.isCollateSuffix() }
            val afterWrappers = wrappers.fold(expression) { acc, c -> c.apply(acc) }
            return suffixes.fold(afterWrappers) { acc, c -> c.apply(acc) }
        }
        override fun toString(): String = parts.joinToString(" -> ")
    }
}

/** Infix helper to compose two Collations into a Composite (left-to-right). */
infix fun Collation.and(other: Collation): Collation = when (this) {
    is Collation.Composite -> when (other) {
        is Collation.Composite -> Collation.Composite(this.parts + other.parts)
        else -> Collation.Composite(this.parts + other)
    }
    else -> when (other) {
        is Collation.Composite -> Collation.Composite(listOf(this) + other.parts)
        else -> Collation.Composite(listOf(this, other))
    }
}

/** Convenience factory for building composites. */
fun collationsOf(vararg parts: Collation): Collation = Collation.Composite(parts.toList())
