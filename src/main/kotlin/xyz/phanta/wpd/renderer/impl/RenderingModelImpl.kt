package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.*

object RenderableNil : Renderable {

    override fun render(): String = ""

}

class RenderableLiteral(private val value: String) : Renderable {

    override fun render(): String = value

}

class ExpressionModel(private val expression: String) : RenderingModel<Renderable> {

    override fun bake(ctx: NameResolver, deps: AssetResolver): Renderable =
        ctx.resolveExpression(ResolutionType.ANY, expression)?.bake(ctx, deps) ?: RenderableNil

}

class StrictExpressionModel(private val expression: String) : RenderingModel<Renderable> {

    override fun bake(ctx: NameResolver, deps: AssetResolver): Renderable =
        ctx.ensureExpression(ResolutionType.ANY, expression).bake(ctx, deps)

}

class ImportModel(private val key: String) : RenderingModel<Renderable> {

    override fun bake(ctx: NameResolver, deps: AssetResolver): Renderable = deps.resolveAsset(key).bake(ctx, deps)

}
