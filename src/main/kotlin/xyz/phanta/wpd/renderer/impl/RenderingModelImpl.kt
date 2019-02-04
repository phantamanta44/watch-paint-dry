package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.Renderable
import xyz.phanta.wpd.model.RenderingContextModel
import xyz.phanta.wpd.model.RenderingModel

class RenderableContextModel : RenderingContextModel<RenderableContext> {

    override val bindings: MutableMap<String, RenderingModel<Any>> = mutableMapOf()
    override val children: MutableList<RenderingModel<Renderable>> = mutableListOf()

    override fun bake(): RenderableContext = RenderableContext(bindings.mapValues { it.value.bake() }, children.map { it.bake() })

}
