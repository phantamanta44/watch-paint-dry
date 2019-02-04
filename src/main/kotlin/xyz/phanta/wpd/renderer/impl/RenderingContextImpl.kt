package xyz.phanta.wpd.renderer.impl

import xyz.phanta.wpd.model.*

class RenderableContext(private val bindings: Map<String, Any>, private val children: List<Renderable>) : RenderingContext {

    override fun <T> resolveReference(type: ResolutionType<T>, identifier: String): T? = type.ensure(identifier, bindings[identifier])

    override fun render(ctx: NameResolver, deps: AssetResolver): String {
        return children.joinToString(separator = "") { it.render(ConjoinedNameResolver(this, ctx), deps) }
    }

}

class ConjoinedNameResolver(private val primary: NameResolver, private val fallback: NameResolver) : NameResolver {

    override fun <T> resolveReference(type: ResolutionType<T>, identifier: String): T? = primary.resolveReference(type, identifier)
            ?: fallback.resolveReference(type, identifier)

}
