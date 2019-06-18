package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.*
import xyz.phanta.wpd.util.throwTypeMismatch

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

class ExpressionBindingContextModel(private val varName: String, private val valueExpr: String) : AbstractRenderingContextModel() {

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderingContext {
        val resolver = buildResolver(ctx, deps).let {
            SingletonResolver(varName, it.ensureExpression(ResolutionType.ANY, valueExpr), it)
        }
        return RenderableContext(resolver, children.map { it.bake(resolver, deps) })
    }

}

class ConditionalContextModel(private val condition: String) : AbstractRenderingContextModel() {

    var fallthrough: RenderingContextModel? = null

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderingContext {
        val resolver = buildResolver(ctx, deps)
        return if (ctx.resolveExpression(ResolutionType.BOOLEAN, condition)?.booleanValue == true) {
            RenderableContext(resolver, children.map { it.bake(resolver, deps) })
        } else {
            fallthrough?.bake(ctx, deps) ?: RenderableContext(resolver, emptyList())
        }
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

class ResolverIterationContextModel(private val keyVar: String, private val valueVar: String, private val mapVar: String)
    : AbstractRenderingContextModel() {

    init {
        if (keyVar == valueVar) {
            if (keyVar != "_") {
                throw MalformationException("Duplicate key and value bindings in resolvable for-each: $keyVar")
            } else {
                throw MalformationException("Resolvable for-each with no key or value bindings!")
            }
        }
    }

    override fun bake(ctx: NameResolver, deps: AssetResolver): RenderableContext {
        val iterable = ctx.ensureExpression(ResolutionType.RESOLVER, mapVar)
        val resolver = buildResolver(ctx, deps)
        return RenderableContext(resolver, iterable.keySet().flatMap { key ->
            when {
                keyVar == "_" -> SingletonResolver(valueVar, iterable.ensureReference(ResolutionType.ANY, key), resolver)
                valueVar == "_" -> SingletonResolver(keyVar, StringData.Of(key), resolver)
                else -> KeyValueResolver(key, iterable.ensureReference(ResolutionType.ANY, key), resolver)
            }.let {
                children.map { child -> child.bake(it, deps) }
            }
        })
    }

    private inner class KeyValueResolver(private val key: String, private val value: Resolved, private val fallback: NameResolver)
        : NameResolver {

        @Suppress("UNCHECKED_CAST")
        override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? = when (identifier) {
            keyVar -> if (ResolutionType.STRING conformsTo type) {
                StringData.Of(key) as T
            } else {
                throwTypeMismatch(type, ResolutionType.STRING)
            }
            valueVar -> type.ensure(identifier, value)
            else -> fallback.resolveReference(type, identifier)
        }

        override fun keySet(): List<String> = fallback.keySet() + key

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

    override fun keySet(): List<String> = fallback.keySet() + key

}

class AppendingNameResolver(private val bindings: Map<String, Any>, private val fallback: NameResolver) : NameResolver {

    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? =
            bindings[identifier]?.let { type.ensure(identifier, it) } ?: fallback.resolveReference(type, identifier)

    override fun keySet(): List<String> = fallback.keySet() + bindings.keys

}
