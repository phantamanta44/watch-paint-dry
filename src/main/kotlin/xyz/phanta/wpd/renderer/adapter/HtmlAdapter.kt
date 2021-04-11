package xyz.phanta.wpd.renderer.adapter

import xyz.phanta.wird.parser.Grammar
import xyz.phanta.wird.parser.Parser
import xyz.phanta.wird.parser.ParserConfig
import xyz.phanta.wird.parser.finalizer.Finalizers
import xyz.phanta.wird.parsetree.ParseTreeParentNode
import xyz.phanta.wpd.model.*
import xyz.phanta.wpd.renderer.impl.*
import xyz.phanta.wpd.util.ContextStackNode
import xyz.phanta.wpd.util.loadGrammar
import java.nio.charset.StandardCharsets

class HtmlAdapter : AbstractFileTypeAdapter("text/html", ".html", ".htm", ".xhtml") {

    companion object {

        private val GRAMMAR: Grammar = loadGrammar("/adapter/html.wird")
        private val PARSER: Parser = GRAMMAR.newParser(
            "html",
            ParserConfig.Builder()
                .withFinalizers("html", 0, Finalizers.flatten(0))
                .withFinalizers("html_entity", 0, Finalizers.omit(0, 3))
                .build()
        )

    }

    override fun parse(key: String, data: ByteArray, deps: AssetResolver): Asset {
        val astTokens = PARSER.parse(data.toString(StandardCharsets.UTF_8))
        var contextStack = ContextStackNode(RenderableContextModel())
        for (token in astTokens.subtrees) {
            try {
                when (token.bodyIndex) {
                    0 -> {
                        val op = token.getLeaf(0).content
                        when (op) {
                            "=" -> contextStack.children.add(StrictExpressionModel(token.operand))
                            "~" -> contextStack.children.add(ExpressionModel(token.operand))
                            ":" -> RenderableContextModel().let {
                                contextStack.bindings[token.operand] = it
                                contextStack = contextStack.push(it)
                            }
                            "." -> {
                                val operands = token.operand.split("=", limit = 2)
                                if (operands.size != 2) {
                                    throw MalformationException("Malformed let-in expression!")
                                }
                                val letModel = ExpressionBindingContextModel(operands[0].trim(), operands[1].trim())
                                contextStack.children.add(letModel)
                                contextStack = contextStack.push(letModel)
                            }
                            "|" -> ConditionalContextModel(token.operand).let {
                                contextStack.children.add(it)
                                contextStack = contextStack.push(it)
                            }
                            "," -> {
                                val condition = token.operand
                                val newContext = if (condition.isEmpty()) {
                                    RenderableContextModel()
                                } else {
                                    ConditionalContextModel(condition)
                                }
                                (contextStack.context as? ConditionalContextModel
                                    ?: throw MalformationException("Conditional fallthrough block without conditional!"))
                                    .fallthrough = newContext
                                contextStack = contextStack.pop().push(newContext)
                            }
                            "[" -> {
                                val operands = token.operand.split("<-", limit = 2)
                                if (operands.size != 2) {
                                    throw MalformationException("Malformed indexable for-each expression!")
                                }
                                val iterModel = IterationContextModel(operands[0].trim(), operands[1].trim())
                                contextStack.children.add(iterModel)
                                contextStack = contextStack.push(iterModel)
                            }
                            "{" -> {
                                val operands = token.operand.split("<-", limit = 2)
                                if (operands.size != 2) {
                                    throw MalformationException("Malformed resolver data-import expression!")
                                }
                                val iterVars = operands[0].split(",", limit = 2)
                                if (iterVars.size != 2) {
                                    throw MalformationException("Malformed resolver data-import expression!")
                                }
                                val iterModel = ResolverIterationContextModel(
                                    iterVars[0].trim(),
                                    iterVars[1].trim(),
                                    operands[1].trim()
                                )
                                contextStack.children.add(iterModel)
                                contextStack = contextStack.push(iterModel)
                            }
                            "/" -> contextStack = contextStack.pop()
                            "+" -> contextStack.children.add(ImportModel(token.operand))
                            "d" -> {
                                val operands = token.operand.split("=", limit = 2)
                                if (operands.size != 2) {
                                    throw MalformationException("Malformed data-import expression!")
                                }
                                val importModel = ImportContextModel(operands[0].trim(), operands[1].trim())
                                contextStack.children.add(importModel)
                                contextStack = contextStack.push(importModel)
                            }
                            else -> throw RenderingStateException("Invalid operator '$op'")
                        }
                    }
                    1, 2 -> contextStack.children.add(RenderableLiteral(token.getLeaf(0).content))
                    else -> throw IllegalStateException("Impossible state: ${token.bodyIndex}")
                }
            } catch (e: RenderingException) {
                throw AssetParsingException(key, e)
            }
        }
        if (contextStack.hasParent) {
            throw RenderingStateException("Not at root context!")
        }
        return RenderableAsset(key, contextStack.context)
    }

    private val ParseTreeParentNode.operand: String
        get() = this.getLeaf(1).content.trim()

}
