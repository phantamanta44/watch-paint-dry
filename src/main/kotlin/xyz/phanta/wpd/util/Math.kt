package xyz.phanta.wpd.util

import kotlin.math.absoluteValue

infix fun Double.fpEq(other: Double): Boolean = (this - other).absoluteValue < 1e-8

infix fun Int.toThe(power: Int): Int = when (power) {
    0 -> 1
    1 -> this
    else -> when (power % 2) {
        0 -> (this * this) toThe (power / 2)
        1 -> this * (this * this) toThe ((power - 1) / 2)
        else -> throw ArithmeticException("Negative powers yield non-integrals!")
    }
}
