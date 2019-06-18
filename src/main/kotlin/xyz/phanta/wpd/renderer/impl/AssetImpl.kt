package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.*

class RenderableAsset(override val assetKey: String, private val root: RenderingModel<RenderingContext>) : Asset {

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderingContext = root.bake(ctx, deps)

}

class DataAsset(override val assetKey: String, private val resolver: NameResolver) : Asset {

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderingContext = AssetRenderingContext(resolver)

    private class AssetRenderingContext(override val nameResolver: NameResolver) : RenderingContext {

        override fun render(): String = nameResolver.stringify()

    }

}
