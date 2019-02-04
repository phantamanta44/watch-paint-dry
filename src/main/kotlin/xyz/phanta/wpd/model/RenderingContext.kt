package xyz.phanta.wpd.model

interface NameResolver {

    fun <T> resolveReference(type: ResolutionType<T>, identifier: String): T?

    fun <T> ensureReference(type: ResolutionType<T>, identifier: String): T = resolveReference(type, identifier)
            ?: throw UnresolvableReferenceException(identifier)

}

interface RenderingContext : NameResolver, Renderable

class ResolutionType<T> private constructor(val type: Class<T>) {

    companion object {

        val RENDERABLE: ResolutionType<Renderable> = ResolutionType(Renderable::class.java)

    }

    @Suppress("UNCHECKED_CAST")
    fun ensure(identifier: String, obj: Any?): T? = when {
        obj == null -> null
        type.isAssignableFrom(obj.javaClass) -> obj as T
        else -> throw ReferenceTypeException(identifier, type, obj.javaClass)
    }

}
