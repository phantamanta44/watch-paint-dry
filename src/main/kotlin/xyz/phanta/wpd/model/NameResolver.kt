package xyz.phanta.wpd.model

import xyz.phanta.wpd.util.parseExpression

interface NameResolver : Resolved {

    fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T?

    fun <T : Resolved> resolveExpression(type: ResolutionType<T>, expression: String): T? = try {
        ensureExpression(type, expression)
    } catch (e: RenderingException) {
        null
    }

    fun <T : Resolved> ensureReference(type: ResolutionType<T>, identifier: String): T = resolveReference(type, identifier)
            ?: throw UnresolvableReferenceException(identifier)

    fun <T : Resolved> ensureExpression(type: ResolutionType<T>, expression: String): T = parseExpression(type, expression)

    fun keySet(): List<String>

    override fun isEq(other: Resolved): Boolean = this == other

}

object NilNameResolver : NameResolver {

    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? = null

    override fun keySet(): List<String> = emptyList()

}

interface RenderingContext : Resolved, NameResolver {

    val nameResolver: NameResolver

    override fun isEq(other: Resolved): Boolean = this == other

    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? =
            nameResolver.resolveReference(type, identifier)

    override fun <T : Resolved> ensureReference(type: ResolutionType<T>, identifier: String): T =
            nameResolver.ensureReference(type, identifier)

    override fun keySet(): List<String> = nameResolver.keySet()

}

interface Resolved : Renderable {

    infix fun isEq(other: Resolved): Boolean

    fun stringify(): String = toString()

    override fun render(): String = stringify()

}

open class ResolutionType<T : Resolved>
private constructor(private val typeName: String, private val type: Class<T>, private val parent: ResolutionType<*>? = ANY) {

    companion object {

        private val TYPES: MutableMap<String, ResolutionType<*>> = mutableMapOf()

        fun resolve(name: String): ResolutionType<*>? = TYPES[name]

        val ANY: ResolutionType<*> = ResolutionType("Any", Resolved::class.java, null)
        val RESOLVER: ResolutionType<NameResolver> = ResolutionType("Map", NameResolver::class.java)
        val INDEXABLE: ResolutionType<Indexable> = ResolutionType("List", Indexable::class.java, RESOLVER)
        val NUMERAL: ResolutionType<NumeralData> = ResolutionType("Number", NumeralData::class.java)
        val INTEGRAL: ResolutionType<IntegralData> = ResolutionType("Integer", IntegralData::class.java, NUMERAL)
        val F_POINT: ResolutionType<FloatData> = ResolutionType("Float", FloatData::class.java, NUMERAL)
        val BOOLEAN: ResolutionType<BooleanData> = ResolutionType("Boolean", BooleanData::class.java)
        val STRING: ResolutionType<StringData> = ResolutionType("String", StringData::class.java, INDEXABLE)

    }

    init {
        @Suppress("LeakingThis")
        TYPES[typeName] = this
    }

    open infix fun conformsTo(other: ResolutionType<*>): Boolean {
        if (this == other) return true
        var type = parent
        while (type != null) {
            if (type == other) return true
            type = type.parent
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    open fun ensure(identifier: String, obj: Any?): T? = when {
        obj == null -> null
        type.isAssignableFrom(obj.javaClass) -> obj as T
        else -> throw ReferenceTypeException(identifier, type, obj.javaClass)
    }

    override fun toString(): String = typeName

}
