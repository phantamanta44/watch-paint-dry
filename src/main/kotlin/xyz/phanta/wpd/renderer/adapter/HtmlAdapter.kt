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
        private val PARSER: Parser = GRAMMAR.newParser("html", ParserConfig.Builder()
                .withFinalizers("html", 0, Finalizers.flatten(0))
                .withFinalizers("html_entity", 0, Finalizers.omit(0, 3))
                .build())

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
                            "$" -> {
                                val operands = token.operand.split("<-", limit = 2)
                                if (operands.size != 2) {
                                    throw MalformationException("Malformed for-each expression!")
                                }
                                val iterModel = IterationContextModel(operands[0].trim(), operands[1].trim())
                                contextStack.children.add(iterModel)
                                contextStack = contextStack.push(iterModel)
                            }
                            "/" -> contextStack = contextStack.pop()
                            "+" -> contextStack.children.add(ImportModel(token.operand))
                            "{" -> {
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
        return RenderableAsset(key, contextStack.getContextAsRoot())
    }

    private val ParseTreeParentNode.operand: String
        get() = this.getLeaf(1).content.trim()

}
