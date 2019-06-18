package xyz.phanta.wpd.model

interface RenderingModel<out T> {

    fun bake(ctx: NameResolver, deps: AssetResolver): T

}

interface RenderingContextModel : RenderingModel<RenderingContext> {

    val bindings: MutableMap<String, RenderingModel<Any>>

    val children: MutableList<RenderingModel<Renderable>>

}
