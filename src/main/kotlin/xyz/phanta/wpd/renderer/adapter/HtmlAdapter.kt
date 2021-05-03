package xyz.phanta.wpd.renderer.adapter

import xyz.phanta.wpd.model.*
import xyz.phanta.wpd.renderer.impl.*
import xyz.phanta.wpd.util.ContextStackNode
import java.nio.charset.StandardCharsets

class HtmlAdapter : AbstractFileTypeAdapter("text/html", ".html", ".htm", ".xhtml") {

    companion object {

        private val TAG_PATTERN: Regex = Regex("""<\?(.)(?:\s*(.+?))?\s*\?>""")

    }

    override fun parse(key: String, data: ByteArray, deps: AssetResolver): Asset {
        val dataStr = data.toString(StandardCharsets.UTF_8)
        val wpdTags = TAG_PATTERN.findAll(dataStr)
        try {
            val parseState = ParseState()
            var textStart = 0
            for (tag in wpdTags) {
                if (tag.range.first > textStart) {
                    parseState.consumeText(dataStr.substring(textStart, tag.range.first))
                }
                parseState.consumeWpdTag(tag.groupValues[1], tag.groupValues[2])
                textStart = tag.range.last + 1
            }
            if (textStart < dataStr.length) {
                parseState.consumeText(dataStr.substring(textStart))
            }
            return parseState.finish(key)
        } catch (e: RenderingException) {
            throw AssetParsingException(key, e)
        }
    }

    private class ParseState {

        private var contextStack: ContextStackNode = ContextStackNode(RenderableContextModel())

        fun consumeText(text: String) {
            contextStack.children.add(RenderableLiteral(text))
        }

        fun consumeWpdTag(opcode: String, operand: String) {
            when (opcode) {
                "=" -> contextStack.children.add(StrictExpressionModel(operand))
                "~" -> contextStack.children.add(ExpressionModel(operand))
                ":" -> RenderableContextModel().let {
                    contextStack.bindings[operand] = it
                    contextStack = contextStack.push(it)
                }
                "." -> {
                    val operands = operand.split("=", limit = 2)
                    if (operands.size != 2) {
                        throw MalformationException("Malformed let-in expression!")
                    }
                    val letModel = ExpressionBindingContextModel(operands[0].trimEnd(), operands[1].trimStart())
                    contextStack.children.add(letModel)
                    contextStack = contextStack.push(letModel)
                }
                "|" -> ConditionalContextModel(operand).let {
                    contextStack.children.add(it)
                    contextStack = contextStack.push(it)
                }
                "," -> {
                    val newContext = if (operand.isEmpty()) {
                        RenderableContextModel()
                    } else {
                        ConditionalContextModel(operand)
                    }
                    (contextStack.context as? ConditionalContextModel
                        ?: throw MalformationException("Conditional fallthrough block without conditional!"))
                        .fallthrough = newContext
                    contextStack = contextStack.pop().push(newContext)
                }
                "[" -> {
                    val operands = operand.split("<-", limit = 2)
                    if (operands.size != 2) {
                        throw MalformationException("Malformed indexable for-each expression!")
                    }
                    val iterModel = IterationContextModel(operands[0].trimEnd(), operands[1].trimStart())
                    contextStack.children.add(iterModel)
                    contextStack = contextStack.push(iterModel)
                }
                "{" -> {
                    val operands = operand.split("<-", limit = 2)
                    if (operands.size != 2) {
                        throw MalformationException("Malformed resolver data-import expression!")
                    }
                    val iterVars = operands[0].split(",", limit = 2)
                    if (iterVars.size != 2) {
                        throw MalformationException("Malformed resolver data-import expression!")
                    }
                    val iterModel = ResolverIterationContextModel(
                        iterVars[0].trimEnd(),
                        iterVars[1].trim(),
                        operands[1].trimStart()
                    )
                    contextStack.children.add(iterModel)
                    contextStack = contextStack.push(iterModel)
                }
                "/" -> contextStack = contextStack.pop()
                "+" -> contextStack.children.add(ImportModel(operand))
                "d" -> {
                    val operands = operand.split("=", limit = 2)
                    if (operands.size != 2) {
                        throw MalformationException("Malformed data-import expression!")
                    }
                    val importModel = ImportContextModel(operands[0].trimEnd(), operands[1].trimStart())
                    contextStack.children.add(importModel)
                    contextStack = contextStack.push(importModel)
                }
                else -> throw RenderingStateException("Invalid operator '$opcode'")
            }
        }

        fun finish(key: String): RenderableAsset {
            if (contextStack.hasParent) {
                throw RenderingStateException("Not at root context!")
            }
            return RenderableAsset(key, contextStack.context)
        }

    }

}
