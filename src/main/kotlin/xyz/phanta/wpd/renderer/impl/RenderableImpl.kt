package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.AssetResolver
import xyz.phanta.wpd.model.NameResolver
import xyz.phanta.wpd.model.Renderable
import xyz.phanta.wpd.model.ResolutionType

class RenderableLiteral(private val value: String) : Renderable {

    override fun render(ctx: NameResolver, deps: AssetResolver): String = value

}

class RenderableExpression(private val expression: String) : Renderable {

    override fun render(ctx: NameResolver, deps: AssetResolver): String =
            ctx.resolveExpression(ResolutionType.ANY, expression)?.render(ctx, deps) ?: ""

}

class RenderableStrictExpression(private val expression: String) : Renderable {

    override fun render(ctx: NameResolver, deps: AssetResolver): String =
            ctx.ensureExpression(ResolutionType.ANY, expression).render(ctx, deps)

}

class RenderableImport(private val key: String) : Renderable {

    override fun render(ctx: NameResolver, deps: AssetResolver): String = deps.resolveAsset(key).render(ctx, deps)

}
