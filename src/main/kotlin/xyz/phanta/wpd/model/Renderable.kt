package xyz.phanta.wpd.model

interface Renderable : RenderingModel<Renderable> {

    fun render(): String

    override fun bake(ctx: NameResolver, deps: AssetResolver): Renderable = this

}
