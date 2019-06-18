package xyz.phanta.wpd.util

import xyz.phanta.wpd.model.Renderable
import xyz.phanta.wpd.model.RenderingContextModel
import xyz.phanta.wpd.model.RenderingModel
import xyz.phanta.wpd.model.RenderingStateException

class ContextStackNode private constructor(val context: RenderingContextModel, private val parent: ContextStackNode?) {

    constructor(root: RenderingContextModel) : this(root, null)

    val bindings: MutableMap<String, RenderingModel<Any>>
        get() = context.bindings

    val children: MutableList<RenderingModel<Renderable>>
        get() = context.children

    val hasParent: Boolean
        get() = parent != null

    fun push(context: RenderingContextModel): ContextStackNode = ContextStackNode(context, this)

    fun pop(): ContextStackNode = parent ?: throw RenderingStateException("Tried to exit root context!")

}
