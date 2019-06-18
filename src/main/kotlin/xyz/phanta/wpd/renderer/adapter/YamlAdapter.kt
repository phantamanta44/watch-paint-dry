package xyz.phanta.wpd.renderer.adapter

import com.amihaiemil.eoyaml.Yaml
import com.amihaiemil.eoyaml.YamlMapping
import com.amihaiemil.eoyaml.YamlSequence
import xyz.phanta.wpd.model.*
import xyz.phanta.wpd.renderer.impl.DataAsset
import java.nio.charset.StandardCharsets

class YamlAdapter : AbstractFileTypeAdapter("application/x-yaml", ".yaml", ".yml") {

    companion object {

        private fun <T : Resolved> wrapScalar(type: ResolutionType<T>, data: String): Resolved = when {
            ResolutionType.BOOLEAN conformsTo type -> when (data) {
                "true" -> BooleanData.TRUE
                "false" -> BooleanData.FALSE
                else -> throw MalformationException("Cannot parse YAMl scalar as boolean: $data")
            }
            ResolutionType.STRING conformsTo type -> StringData.Of(data)
            ResolutionType.INTEGRAL conformsTo type -> IntegralData.Of(data.toInt())
            ResolutionType.F_POINT conformsTo type || ResolutionType.NUMERAL conformsTo type -> FloatData.Of(data.toDouble())
            else -> throw MalformationException("Cannot parse YAML scalar as type: $type")
        }

    }

    override fun parse(key: String, data: ByteArray, deps: AssetResolver): Asset =
            DataAsset(key, MappingWrapper(Yaml.createYamlInput(data.toString(StandardCharsets.UTF_8)).readYamlMapping()))

    private class MappingWrapper(private val map: YamlMapping) : NameResolver {

        override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? = try {
            ensureReference(type, identifier)
        } catch (e: RenderingException) {
            null
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Resolved> ensureReference(type: ResolutionType<T>, identifier: String): T = when {
            ResolutionType.RESOLVER conformsTo type -> MappingWrapper(map.yamlMapping(identifier))
            ResolutionType.INDEXABLE conformsTo type -> SequenceWrapper(map.yamlSequence(identifier))
            else -> wrapScalar(type, map.string(identifier))
        } as T

        override fun stringify(): String = map.toString()

    }

    private class SequenceWrapper(private val seq: YamlSequence) : Indexable {

        override fun <T : Resolved> resolveIndex(type: ResolutionType<T>, index: Int): T? = try {
            ensureIndex(type, index)
        } catch (e: RenderingException) {
            null
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Resolved> ensureIndex(type: ResolutionType<T>, index: Int): T = when {
            ResolutionType.RESOLVER conformsTo type -> MappingWrapper(seq.yamlMapping(index))
            ResolutionType.INDEXABLE conformsTo type -> SequenceWrapper(seq.yamlSequence(index))
            else -> wrapScalar(type, seq.string(index))
        } as T

        override val length: Int
            get() = seq.size()

        override fun stringify(): String = seq.toString()

    }

}
