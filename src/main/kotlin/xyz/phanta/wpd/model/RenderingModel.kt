package xyz.phanta.wpd.model

interface RenderingModel<out T> {

    fun bake(): T

}

interface RenderingContextModel<T : RenderingContext> : RenderingModel<T> {

    val bindings: MutableMap<String, RenderingModel<Any>>

    val children: MutableList<RenderingModel<Renderable>>

}
