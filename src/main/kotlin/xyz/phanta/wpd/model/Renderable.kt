package xyz.phanta.wpd.model

interface Renderable : RenderingModel<Renderable> {

    fun render(ctx: NameResolver, deps: AssetResolver): String

    override fun bake(): Renderable = this

}
