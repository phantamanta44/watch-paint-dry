package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.*

class RenderableContext(override val nameResolver: NameResolver, private val children: List<Renderable>) : RenderingContext {

    override fun render(): String = children.joinToString(separator = "") { it.render() }

}

abstract class AbstractRenderingContextModel : RenderingContextModel {

    override val bindings: MutableMap<String, RenderingModel<Any>> = mutableMapOf()
    override val children: MutableList<RenderingModel<Renderable>> = mutableListOf()

    internal fun buildResolver(ctx: NameResolver, deps: AssetResolver): NameResolver =
            AppendingNameResolver(bindings.mapValues { it.value.bake(ctx, deps) }, ctx)

}

class RenderableContextModel : AbstractRenderingContextModel() {

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderableContext {
        val resolver = buildResolver(ctx, deps)
        return RenderableContext(resolver, children.map { it.bake(resolver, deps) })
    }

}

class IterationContextModel(private val iterVar: String, private val iterableVar: String) : AbstractRenderingContextModel() {

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderableContext {
        val iterable = ctx.ensureExpression(ResolutionType.INDEXABLE, iterableVar)
        val resolver = buildResolver(ctx, deps)
        return RenderableContext(resolver, (0 until iterable.length).flatMap { index ->
            SingletonResolver(iterVar, iterable.ensureIndex(ResolutionType.ANY, index), resolver).let {
                children.map { child -> child.bake(it, deps) }
            }
        })
    }

}

class ImportContextModel(private val importVar: String, private val importKey: String) : AbstractRenderingContextModel() {

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderingContext {
        val resolver = buildResolver(ctx, deps)
        val withImport = SingletonResolver(importVar, deps.resolveAsset(importKey).bake(resolver, deps), resolver)
        return RenderableContext(withImport, children.map { it.bake(withImport, deps) })
    }

}

class SingletonResolver(private val key: String, private val value: Resolved, private val fallback: NameResolver) : NameResolver {

    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? =
            if (identifier == key) type.ensure(identifier, value) else fallback.resolveReference(type, identifier)

}

class AppendingNameResolver(private val bindings: Map<String, Any>, private val fallback: NameResolver) : NameResolver {

    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? =
            bindings[identifier]?.let { type.ensure(identifier, it) } ?: fallback.resolveReference(type, identifier)

}
