package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.*

class RenderableContext(override val nameResolver: NameResolver, private val children: List<Renderable>) : RenderingContext {

    override fun render(): String = children.joinToString(separator = "") { it.render() }

}

abstract class AbstractRenderingContextModel : RenderingContextModel {

    override val bindings: MutableMap<String, RenderingModel<Any>> = mutableMapOf()
    override val children: MutableList<RenderingModel<Renderable>> = mutableListOf()

}

class RenderableContextModel : AbstractRenderingContextModel() {

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderableContext {
        val resolver = ConjoinedNameResolver(bindings.mapValues { it.value.bake(ctx, deps) }, ctx)
        return RenderableContext(resolver, children.map { it.bake(resolver, deps) })
    }

}

class IterationContextModel(private val iterVar: String, private val iterableVar: String) : AbstractRenderingContextModel() {

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderableContext {
        val iterable = ctx.ensureExpression(ResolutionType.INDEXABLE, iterableVar)
        val resolver = ConjoinedNameResolver(bindings.mapValues { it.value.bake(ctx, deps) }, ctx)
        return RenderableContext(resolver, (0 until iterable.length).flatMap { index ->
            IterationNameResolver(iterable.ensureIndex(ResolutionType.ANY, index), resolver).let {
                children.map { child -> child.bake(it, deps) }
            }
        })
    }

    private inner class IterationNameResolver(private val value: Resolved, private val parent: NameResolver) : NameResolver {

        override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? =
                if (identifier == iterVar) type.ensure(identifier, value) else parent.resolveReference(type, identifier)

    }

}

class ConjoinedNameResolver(private val bindings: Map<String, Any>, private val fallback: NameResolver) : NameResolver {

    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? =
            bindings[identifier]?.let { type.ensure(identifier, it) } ?: fallback.resolveReference(type, identifier)

}
