package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.*

class RenderableAsset(override val assetKey: String, private val root: RenderableContext) : Asset {

    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? = root.resolveReference(type, identifier)

    override fun render(ctx: NameResolver, deps: AssetResolver): String = root.render(ctx, deps)

}

class DataAsset(override val assetKey: String, private val resolver: NameResolver) : Asset {

    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? =
            resolver.resolveReference(type, identifier)

    override fun <T : Resolved> ensureReference(type: ResolutionType<T>, identifier: String): T =
            resolver.ensureReference(type, identifier)
    
}
