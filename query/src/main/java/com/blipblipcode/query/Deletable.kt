package com.blipblipcode.query

/**
 * Represents a queryable object that can be converted into a [QueryDelete].
 *
 * Implementing classes expose a `toQueryDelete()` operation that builds a
 * [QueryDelete] targeting the same data source described by the object and
 * reusing its filtering conditions.
 *
 * Classes that aggregate multiple tables (such as [UnionQuery]) may need
 * additional parameters to resolve which table to delete from and which
 * column should be matched against the sub-query result. Those classes
 * should provide extra overloads of `toQueryDelete` while keeping the
 * parameter-less one defined here (typically throwing an informative
 * [IllegalArgumentException]) so the contract remains uniform.
 */
interface Deletable {

    /**
     * Converts this object into a [QueryDelete] instance.
     *
     * @return A new [QueryDelete] built from the filters of this object.
     * @throws IllegalArgumentException if the conversion cannot be performed
     * without additional context (e.g. a target table or a join column).
     */
    fun toQueryDelete(): QueryDelete
}