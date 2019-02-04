package xyz.phanta.wpd.util

import xyz.phanta.wpd.model.*

class ContextStackNode<T : RenderingContext> private constructor(
        private val context: RenderingContextModel<*>, private val parent: ContextStackNode<T>?) {

    constructor(root: RenderingContextModel<T>) : this(root, null)

    val bindings: MutableMap<String, RenderingModel<Any>>
        get() = context.bindings

    val children: MutableList<RenderingModel<Renderable>>
        get() = context.children

    fun push(context: RenderingContextModel<T>): ContextStackNode<T> = ContextStackNode(context, this)

    fun pop(): ContextStackNode<T> = parent ?: throw RenderingStateException("Tried to exit root context!")

    @Suppress("UNCHECKED_CAST")
    fun getContextAsRoot(): RenderingContextModel<T> = if (parent == null) {
        context as RenderingContextModel<T>
    } else {
        throw RenderingStateException("Not at root context!")
    }

}
