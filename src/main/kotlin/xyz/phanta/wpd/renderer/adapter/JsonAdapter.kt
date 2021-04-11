package xyz.phanta.wpd.renderer.adapter

import com.google.gson.*
import xyz.phanta.wpd.model.*
import xyz.phanta.wpd.renderer.impl.DataAsset
import xyz.phanta.wpd.util.throwTypeMismatch
import java.nio.charset.StandardCharsets

class JsonAdapter : AbstractFileTypeAdapter("application/json", ".json") {

    companion object {

        private val PARSER: JsonParser = JsonParser()
        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()

        private fun <T : Resolved> wrap(type: ResolutionType<T>, dto: JsonElement): Resolved = when {
            dto.isJsonObject -> if (ResolutionType.RESOLVER conformsTo type) {
                MapWrapper(dto.asJsonObject)
            } else {
                throwTypeMismatch(type, ResolutionType.RESOLVER)
            }
            dto.isJsonArray -> if (ResolutionType.INDEXABLE conformsTo type) {
                ListWrapper(dto.asJsonArray)
            } else {
                throwTypeMismatch(type, ResolutionType.INDEXABLE)
            }
            dto.isJsonPrimitive -> dto.asJsonPrimitive.let {
                when {
                    it.isBoolean -> if (ResolutionType.BOOLEAN conformsTo type) {
                        BooleanData.of(it.asBoolean)
                    } else {
                        throwTypeMismatch(type, ResolutionType.BOOLEAN)
                    }
                    it.isString -> if (ResolutionType.STRING conformsTo type) {
                        StringData.Of(it.asString)
                    } else {
                        throwTypeMismatch(type, ResolutionType.STRING)
                    }
                    it.isNumber -> when {
                        ResolutionType.INTEGRAL conformsTo type -> IntegralData.Of(it.asInt)
                        ResolutionType.F_POINT conformsTo type -> FloatData.Of(it.asDouble)
                        else -> throwTypeMismatch(type, ResolutionType.NUMERAL)
                    }
                    else -> throw MalformationException("Bad JSON primitive: $it")
                }
            }
            else -> throw MalformationException("Bad JSON value: $dto")
        }

    }

    override fun parse(key: String, data: ByteArray, deps: AssetResolver): Asset {
        val dto = PARSER.parse(String(data, StandardCharsets.UTF_8))
        if (!dto.isJsonObject) throw MalformationException("JSON data must be a map!")
        return DataAsset(key, MapWrapper(dto.asJsonObject))
    }

    private class MapWrapper(private val dto: JsonObject) : NameResolver {

        override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? = try {
            ensureReference(type, identifier)
        } catch (e: RenderingException) {
            null
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Resolved> ensureReference(type: ResolutionType<T>, identifier: String): T =
            wrap(type, dto.get(identifier) ?: throw UnresolvableAssetException(identifier)) as T

        override fun keySet(): List<String> = dto.keySet().toList()

        override fun stringify(): String = GSON.toJson(dto)

    }

    private class ListWrapper(private val dto: JsonArray) : Indexable {

        override fun <T : Resolved> resolveIndex(type: ResolutionType<T>, index: Int): T? = try {
            ensureIndex(type, index)
        } catch (e: RenderingException) {
            null
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Resolved> ensureIndex(type: ResolutionType<T>, index: Int): T =
            wrap(type, dto.get(index)) as T

        override val length: Int
            get() = dto.size()

        override fun stringify(): String = GSON.toJson(dto)

    }

}
