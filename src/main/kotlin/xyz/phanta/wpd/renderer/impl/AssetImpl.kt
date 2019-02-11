package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.*

class BaseAsset(override val assetKey: String, private val root: RenderableContext) : Asset {

    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? = root.resolveReference(type, identifier)

    override fun render(ctx: NameResolver, deps: AssetResolver): String = root.render(ctx, deps)

}
