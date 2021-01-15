package me.dreamhopping.pml.gradle.util

import java.util.function.Predicate
import java.util.function.UnaryOperator

class ModificationCallbackList<T>(toClone: List<T>, private val onModification: () -> Unit) : ArrayList<T>(toClone) {
    constructor(onModification: () -> Unit) : this(emptyList(), onModification)

    override fun add(element: T): Boolean {
        return super.add(element).also { onModification() }
    }

    override fun add(index: Int, element: T) {
        super.add(index, element).also { onModification() }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return super.addAll(elements).also { onModification() }
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        return super.addAll(index, elements).also { onModification() }
    }

    override fun clear() {
        super.clear().also { onModification() }
    }

    override fun remove(element: T): Boolean {
        return super.remove(element).also { onModification() }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return super.removeAll(elements).also { onModification() }
    }

    override fun removeIf(filter: Predicate<in T>): Boolean {
        return super.removeIf(filter).also { onModification() }
    }

    override fun removeAt(index: Int): T {
        return super.removeAt(index).also { onModification() }
    }

    override fun set(index: Int, element: T): T {
        return super.set(index, element).also { onModification() }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        return ModificationCallbackList(super.subList(fromIndex, toIndex), onModification)
    }

    override fun replaceAll(operator: UnaryOperator<T>) {
        super.replaceAll(operator).also { onModification() }
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        super.removeRange(fromIndex, toIndex).also { onModification() }
    }
}