package com.blipblipcode.query.operator

interface Copyable<T : Copyable<T>> {
    fun clone(): T
}