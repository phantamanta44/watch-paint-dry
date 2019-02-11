package xyz.phanta.wpd.util

import xyz.phanta.wird.parser.Grammar
import xyz.phanta.wird.parser.Parser
import xyz.phanta.wird.parser.ParserConfig
import xyz.phanta.wird.parser.finalizer.Finalizers
import xyz.phanta.wird.parsetree.ParseTreeParentNode
import xyz.phanta.wpd.model.*
import xyz.phanta.wpd.model.ResolutionType.Companion.ANY
import xyz.phanta.wpd.model.ResolutionType.Companion.BOOLEAN
import xyz.phanta.wpd.model.ResolutionType.Companion.F_POINT
import xyz.phanta.wpd.model.ResolutionType.Companion.INDEXABLE
import xyz.phanta.wpd.model.ResolutionType.Companion.INTEGRAL
import xyz.phanta.wpd.model.ResolutionType.Companion.NUMERAL
import xyz.phanta.wpd.model.ResolutionType.Companion.RESOLVER
import xyz.phanta.wpd.model.ResolutionType.Companion.STRING
import kotlin.math.pow

private val GRAMMAR: Grammar = loadGrammar("/expression.wird")
private val PARSER: Parser = GRAMMAR.newParser("expr", ParserConfig.Builder()
        .withFinalizers("expr", 0, Finalizers.omit(1, 3))
        .withFinalizers("expr", Finalizers.flatten(0))

        .binaryOp("conditional_or_expr")
        .binaryOp("conditional_and_expr")
        .binaryOp("inclusive_or_expr")
        .binaryOp("exclusive_or_expr")
        .binaryOp("and_expr")
        .binaryOp("equality_expr", 2)
        .binaryOp("relational_expr", 6)
        .binaryOp("shift_expr", 3)
        .binaryOp("additive_expr", 2)
        .binaryOp("multiplicative_expr", 3)
        .binaryOp("exponentiation_expr")

        .withFinalizers("sign_expr", 0, Finalizers.omit(0), Finalizers.wrap(0, 1, 0, Finalizers.flatten(0)))
        .withFinalizers("sign_expr", 1, Finalizers.omit(0), Finalizers.wrap(0, 1, 1, Finalizers.flatten(0)))
        .withFinalizers("sign_expr", 2, Finalizers.flatten(0))
        .withFinalizers("unary_expr", 2, Finalizers.omit(0), Finalizers.wrap(0, 1, 2, Finalizers.flatten(0)))
        .withFinalizers("unary_expr", 3, Finalizers.omit(0), Finalizers.wrap(0, 1, 3, Finalizers.flatten(0)))

        .withFinalizers("primary", 0, Finalizers.flatten(0))
        .withFinalizers("primary", 1, Finalizers.omit(0, 2))
        .withFinalizers("primary", 2, Finalizers.omit(1))
        .withFinalizers("primary", 3, Finalizers.flatten(1))
        .withFinalizers("primary", 4, Finalizers.flatten(1))

        .withFinalizers("literal", 3, Finalizers.omit(0, 2))
        .withFinalizers("floating_point_literal", Finalizers.join())

        .withFinalizers("string_literal", Finalizers.flatten(0), Finalizers.join())
        .withFinalizers("string_literal", 0, Finalizers.flatten(1))
        .withFinalizers("string_seg", Finalizers.unescape())
        .withFinalizers("string_seg", 0, Finalizers.flatten(0))

        .withFinalizers("expr_name", 1, Finalizers.omit(1), Finalizers.flatten(1))
        .withFinalizers("expr_name", Finalizers.flatten(0))
        .withFinalizers("array_index", Finalizers.omit(0, 2))
        .build())

private fun ParserConfig.Builder.binaryOp(identifier: String, count: Int = 1): ParserConfig.Builder {
    for (i in 1..count) {
        withFinalizers(identifier, i, Finalizers.omit(1), Finalizers.flatten(1),
                Finalizers.wrap(0, 2, i, Finalizers.flatten(0)))
    }
    return withFinalizers(identifier, 0, Finalizers.flatten(0))
}

fun <T : Resolved> NameResolver.parseExpression(type: ResolutionType<T>, expression: String): T = marshal(type, PARSER.parse(expression))

@Suppress("UNCHECKED_CAST")
private fun <T : Resolved> NameResolver.marshal(type: ResolutionType<T>, node: ParseTreeParentNode): T = when (node.classification.identifier) {
    "expr" -> when (node.bodyIndex) {
        0 -> if (boolAt(node, 0)) marshal(type, node.getSubtree(1)) else marshal(type, node.getSubtree(2))
        1 -> marshal(type, node.getSubtree(0))
        else -> throw IllegalStateException("Bad expr: ${node.bodyIndex}")
    }

    "conditional_or_expr" -> if (BOOLEAN conformsTo type) {
        BooleanData.of(boolAt(node, 0) || boolAt(node, 1)) as T
    } else {
        throwTypeMismatch(type, BOOLEAN)
    }

    "conditional_and_expr" -> if (BOOLEAN conformsTo type) {
        BooleanData.of(boolAt(node, 0) && boolAt(node, 1)) as T
    } else {
        throwTypeMismatch(type, BOOLEAN)
    }

    "inclusive_or_expr" -> if (INTEGRAL conformsTo type) {
        IntegralData.Of(intAt(node, 0) or intAt(node, 1)) as T
    } else {
        throwTypeMismatch(type, INTEGRAL)
    }

    "exclcusive_or_expr" -> if (INTEGRAL conformsTo type) {
        IntegralData.Of(intAt(node, 0) xor intAt(node, 1)) as T
    } else {
        throwTypeMismatch(type, INTEGRAL)
    }

    "and_expr" -> if (INTEGRAL conformsTo type) {
        IntegralData.Of(intAt(node, 0) and intAt(node, 1)) as T
    } else {
        throwTypeMismatch(type, INTEGRAL)
    }

    "equality_expr" -> if (BOOLEAN conformsTo type) {
        BooleanData.of(when (node.bodyIndex) {
            1 -> marshal(ANY, node.getSubtree(0)) isEq marshal(ANY, node.getSubtree(1))
            2 -> !(marshal(ANY, node.getSubtree(0)) isEq marshal(ANY, node.getSubtree(1)))
            else -> throw IllegalStateException("Bad equality expr: ${node.bodyIndex}")
        }) as T
    } else {
        throwTypeMismatch(type, BOOLEAN)
    }

    "relational_expr" -> if (BOOLEAN conformsTo type) {
        when (node.bodyIndex) {
            1 -> marshal(NUMERAL, node.getSubtree(0)) lt marshal(NUMERAL, node.getSubtree(1))
            2 -> marshal(NUMERAL, node.getSubtree(0)) gt marshal(NUMERAL, node.getSubtree(1))
            3 -> marshal(NUMERAL, node.getSubtree(0)) lte marshal(NUMERAL, node.getSubtree(1))
            4 -> marshal(NUMERAL, node.getSubtree(0)) gte marshal(NUMERAL, node.getSubtree(1))
            else -> throw IllegalStateException("Bad relational expr: ${node.bodyIndex}")
        } as T
    } else {
        throwTypeMismatch(type, BOOLEAN)
    }

    "shift_expr" -> if (INTEGRAL conformsTo type) {
        IntegralData.Of(when (node.bodyIndex) {
            1 -> intAt(node, 0) shl intAt(node, 1)
            2 -> intAt(node, 0) shr intAt(node, 1)
            3 -> intAt(node, 0) ushr intAt(node, 1)
            else -> throw IllegalStateException("Bad shift expr: ${node.bodyIndex}")
        }) as T
    } else {
        throwTypeMismatch(type, INTEGRAL)
    }

    "additive_expr" -> when {
        INTEGRAL conformsTo type -> when (node.bodyIndex) {
            1 -> marshal(INTEGRAL, node.getSubtree(0)) plus marshal(INTEGRAL, node.getSubtree(1))
            2 -> marshal(INTEGRAL, node.getSubtree(0)) minus marshal(INTEGRAL, node.getSubtree(1))
            else -> throw IllegalStateException("Bad additive expr: ${node.bodyIndex}")
        }
        F_POINT conformsTo type -> when (node.bodyIndex) {
            1 -> marshal(F_POINT, node.getSubtree(0)) plus marshal(F_POINT, node.getSubtree(1))
            2 -> marshal(F_POINT, node.getSubtree(0)) minus marshal(F_POINT, node.getSubtree(1))
            else -> throw IllegalStateException("Bad additive expr: ${node.bodyIndex}")
        }
        STRING conformsTo type -> if (node.bodyIndex == 1) {
            StringData.Of(marshal(ANY, node.getSubtree(0)).stringify() + marshal(ANY, node.getSubtree(1)).stringify())
        } else {
            throwTypeMismatch(STRING, NUMERAL)
        }
        else -> throwTypeMismatch(type, NUMERAL)
    } as T

    "multiplicative_expr" -> when {
        INTEGRAL conformsTo type -> when (node.bodyIndex) {
            1 -> marshal(INTEGRAL, node.getSubtree(0)) times marshal(INTEGRAL, node.getSubtree(1))
            2 -> marshal(INTEGRAL, node.getSubtree(0)) divBy marshal(INTEGRAL, node.getSubtree(1))
            3 -> marshal(INTEGRAL, node.getSubtree(0)) modulo marshal(INTEGRAL, node.getSubtree(1))
            else -> throw IllegalStateException("Bad additive expr: ${node.bodyIndex}")
        }
        F_POINT conformsTo type -> when (node.bodyIndex) {
            1 -> marshal(F_POINT, node.getSubtree(0)) times marshal(F_POINT, node.getSubtree(1))
            2 -> marshal(F_POINT, node.getSubtree(0)) divBy marshal(F_POINT, node.getSubtree(1))
            3 -> marshal(F_POINT, node.getSubtree(0)) modulo marshal(INTEGRAL, node.getSubtree(1))
            else -> throw IllegalStateException("Bad additive expr: ${node.bodyIndex}")
        }
        else -> throwTypeMismatch(type, NUMERAL)
    } as T

    "exponentiation_expr" -> when {
        INTEGRAL conformsTo type -> IntegralData.Of(intAt(node, 0) toThe intAt(node, 1))
        F_POINT conformsTo type -> FloatData.Of(fpAt(node, 0).pow(fpAt(node, 1)))
        else -> throwTypeMismatch(type, NUMERAL)
    } as T

    "sign_expr" -> when {
        INTEGRAL conformsTo type -> when (node.bodyIndex) {
            0 -> marshal(INTEGRAL, node.getSubtree(0))
            1 -> marshal(INTEGRAL, node.getSubtree(0)).negative()
            else -> throw IllegalStateException("Bad sign expr: ${node.bodyIndex}")
        }
        F_POINT conformsTo type -> when (node.bodyIndex) {
            0 -> marshal(F_POINT, node.getSubtree(0))
            1 -> marshal(F_POINT, node.getSubtree(0)).negative()
            else -> throw IllegalStateException("Bad sign expr: ${node.bodyIndex}")
        }
        else -> throwTypeMismatch(type, NUMERAL)
    } as T

    "unary_expr" -> when (node.bodyIndex) {
        2 -> if (INTEGRAL conformsTo type) {
            IntegralData.Of(intAt(node, 0).inv())
        } else {
            throwTypeMismatch(type, INTEGRAL)
        }
        3 -> if (BOOLEAN conformsTo type) {
            BooleanData.of(!boolAt(node, 0))
        } else {
            throwTypeMismatch(type, BOOLEAN)
        }
        else -> throw IllegalStateException("Bad unary expr: ${node.bodyIndex}")
    } as T

    "primary" -> when (node.bodyIndex) {
        0 -> node.getSubtree(0).let {
            when (it.classification.identifier) {
                "floating_point_literal" -> if (F_POINT conformsTo type) {
                    FloatData.Of(it.getLeaf(0).content.toDouble())
                } else {
                    throwTypeMismatch(type, F_POINT)
                }
                "integer_literal" -> when {
                    INTEGRAL conformsTo type -> IntegralData.Of(it.getLeaf(0).content.toInt())
                    F_POINT conformsTo type -> FloatData.Of(it.getLeaf(0).content.toDouble())
                    else -> throwTypeMismatch(type, INTEGRAL)
                }
                "boolean_literal" -> if (BOOLEAN conformsTo type) {
                    BooleanData.of(it.bodyIndex == 0)
                } else {
                    throwTypeMismatch(type, BOOLEAN)
                }
                "string_literal" -> if (STRING conformsTo type) {
                    StringData.Of(it.getLeaf(0).content)
                } else {
                    throwTypeMismatch(type, STRING)
                }
                else -> throw IllegalStateException("Bad literal: ${it.classification.identifier}")
            }
        }
        1 -> marshal(type, node.getSubtree(0))
        2 -> marshal(RESOLVER, node.getSubtree(0)).ensureReference(type, node.getSubtree(1).getLeaf(0).content)
        3, 4 -> marshal(INDEXABLE, node.getSubtree(0)).ensureIndex(type, intAt(node, 1))
        else -> throw IllegalStateException("Bad primary expr: ${node.bodyIndex}")
    } as T

    "expr_name" -> when (node.bodyIndex) {
        0 -> ensureReference(type, node.getLeaf(0).content)
        1 -> marshal(RESOLVER, node.getSubtree(0)).ensureReference(type, node.getLeaf(1).content)
        else -> throw IllegalStateException("Bad expr name: ${node.bodyIndex}")
    }

    else -> throw IllegalStateException("Bad classification: ${node.classification.identifier}")
}

private fun NameResolver.intAt(node: ParseTreeParentNode, index: Int): Int = marshal(INTEGRAL, node.getSubtree(index)).integerValue
private fun NameResolver.fpAt(node: ParseTreeParentNode, index: Int): Double = marshal(F_POINT, node.getSubtree(index)).floatValue
private fun NameResolver.boolAt(node: ParseTreeParentNode, index: Int): Boolean = marshal(BOOLEAN, node.getSubtree(index)).booleanValue

class ExpressionException(msg: String) : RenderingException(msg)

fun throwTypeMismatch(expected: ResolutionType<*>, actual: ResolutionType<*>): Nothing =
        throw ExpressionException("Expected type $expected but found $actual!")
