package xyz.phanta.wpd.model

import xyz.phanta.wpd.util.fpEq
import xyz.phanta.wpd.util.throwTypeMismatch

interface Indexable : NameResolver, Resolved {

    companion object {

        private val KEY_SET: List<String> = listOf("length")

    }

    fun <T : Resolved> resolveIndex(type: ResolutionType<T>, index: Int): T?

    fun <T : Resolved> ensureIndex(type: ResolutionType<T>, index: Int): T = resolveIndex(type, index)
            ?: throw UnresolvableReferenceException("[$index]")

    val length: Int

    @Suppress("UNCHECKED_CAST")
    override fun <T : Resolved> resolveReference(type: ResolutionType<T>, identifier: String): T? =
            if (identifier == "length") {
                if (type conformsTo ResolutionType.INTEGRAL) {
                    IntegralData.Of(length) as T
                } else {
                    throwTypeMismatch(type, ResolutionType.INTEGRAL)
                }
            } else {
                null
            }

    override fun keySet(): List<String> = KEY_SET

    override fun isEq(other: Resolved): Boolean = this == other

}

interface NumeralData : Resolved {

    infix fun plus(other: NumeralData): NumeralData

    infix fun minus(other: NumeralData): NumeralData

    infix fun times(other: NumeralData): NumeralData

    infix fun divBy(other: NumeralData): NumeralData

    infix fun modulo(other: IntegralData): NumeralData

    infix fun lt(other: NumeralData): BooleanData

    infix fun gt(other: NumeralData): BooleanData

    infix fun lte(other: NumeralData): BooleanData = this lt other or { BooleanData.of(this isEq other) }

    infix fun gte(other: NumeralData): BooleanData = this gt other or { BooleanData.of(this isEq other) }

    fun negative(): NumeralData

}

interface IntegralData : NumeralData {

    val integerValue: Int

    class Of(override val integerValue: Int) : IntegralData

    override fun plus(other: NumeralData): NumeralData = when (other) {
        is IntegralData -> Of(integerValue + other.integerValue)
        is FloatData -> FloatData.Of(integerValue + other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun minus(other: NumeralData): NumeralData = when (other) {
        is IntegralData -> Of(integerValue - other.integerValue)
        is FloatData -> FloatData.Of(integerValue - other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun times(other: NumeralData): NumeralData = when (other) {
        is IntegralData -> Of(integerValue * other.integerValue)
        is FloatData -> FloatData.Of(integerValue * other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun divBy(other: NumeralData): NumeralData = when (other) {
        is IntegralData -> Of(integerValue / other.integerValue)
        is FloatData -> FloatData.Of(integerValue / other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun modulo(other: IntegralData): IntegralData = Of(integerValue % other.integerValue)

    override fun lt(other: NumeralData): BooleanData = when (other) {
        is IntegralData -> BooleanData.of(integerValue < other.integerValue)
        is FloatData -> BooleanData.of(integerValue < other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun gt(other: NumeralData): BooleanData = when (other) {
        is IntegralData -> BooleanData.of(integerValue > other.integerValue)
        is FloatData -> BooleanData.of(integerValue > other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun negative(): IntegralData = Of(-integerValue)

    fun promote(): FloatData = FloatData.Of(integerValue.toDouble())

    override fun isEq(other: Resolved): Boolean = when (other) {
        is IntegralData -> integerValue == other.integerValue
        is FloatData -> integerValue.toDouble() fpEq other.floatValue
        else -> false
    }

    override fun stringify(): String = integerValue.toString()

}

interface FloatData : NumeralData {

    val floatValue: Double

    class Of(override val floatValue: Double) : FloatData

    override fun plus(other: NumeralData): NumeralData = when (other) {
        is IntegralData -> Of(floatValue + other.integerValue)
        is FloatData -> Of(floatValue + other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun minus(other: NumeralData): NumeralData = when (other) {
        is IntegralData -> Of(floatValue - other.integerValue)
        is FloatData -> Of(floatValue - other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun times(other: NumeralData): NumeralData = when (other) {
        is IntegralData -> Of(floatValue * other.integerValue)
        is FloatData -> Of(floatValue * other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun divBy(other: NumeralData): NumeralData = when (other) {
        is IntegralData -> Of(floatValue / other.integerValue)
        is FloatData -> Of(floatValue / other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun modulo(other: IntegralData): FloatData = Of(floatValue % other.integerValue)

    override fun lt(other: NumeralData): BooleanData = when (other) {
        is IntegralData -> BooleanData.of(floatValue < other.integerValue)
        is FloatData -> BooleanData.of(floatValue < other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun gt(other: NumeralData): BooleanData = when (other) {
        is IntegralData -> BooleanData.of(floatValue > other.integerValue)
        is FloatData -> BooleanData.of(floatValue > other.floatValue)
        else -> throw IllegalStateException(other::class.java.canonicalName)
    }

    override fun negative(): FloatData = Of(-floatValue)

    override fun isEq(other: Resolved): Boolean = when (other) {
        is IntegralData -> floatValue fpEq other.integerValue.toDouble()
        is FloatData -> floatValue fpEq other.floatValue
        else -> false
    }

    override fun stringify(): String = floatValue.toString()

}

interface BooleanData : Resolved {

    val booleanValue: Boolean

    companion object {

        val TRUE: BooleanData = object : BooleanData {
            override val booleanValue: Boolean
                get() = true
        }

        val FALSE: BooleanData = object : BooleanData {
            override val booleanValue: Boolean
                get() = false
        }

        fun of(value: Boolean): BooleanData = if (value) TRUE else FALSE

    }

    infix fun or(other: () -> BooleanData): BooleanData = of(booleanValue || other().booleanValue)

    override fun isEq(other: Resolved): Boolean = if (other is BooleanData) {
        booleanValue == other.booleanValue
    } else {
        false
    }

    override fun stringify(): String = booleanValue.toString()

}

interface StringData : Indexable, Resolved {

    val stringValue: String

    class Of(override val stringValue: String) : StringData

    override fun isEq(other: Resolved): Boolean = if (other is StringData) {
        stringValue == other.stringValue
    } else {
        false
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Resolved> resolveIndex(type: ResolutionType<T>, index: Int): T? =
            if (ResolutionType.STRING conformsTo type) {
                Of(stringValue[index].toString()) as T
            } else {
                throwTypeMismatch(type, ResolutionType.STRING)
            }

    override val length: Int
        get() = stringValue.length

    override fun stringify(): String = stringValue

}
