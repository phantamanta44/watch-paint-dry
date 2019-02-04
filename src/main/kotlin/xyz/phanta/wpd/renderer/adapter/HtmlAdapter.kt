package xyz.phanta.wpd.renderer.adapter

import io.github.phantamanta44.resyn.parser.Syntax
import io.github.phantamanta44.resyn.parser.token.TokenContainer
import io.github.phantamanta44.resyn.parser.token.TokenNode
import xyz.phanta.wpd.model.Asset
import xyz.phanta.wpd.model.AssetAdapter
import xyz.phanta.wpd.model.AssetResolver
import xyz.phanta.wpd.model.RenderingStateException
import xyz.phanta.wpd.renderer.impl.*
import xyz.phanta.wpd.util.ContextStackNode
import xyz.phanta.wpd.util.childNode
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class HtmlAdapter : AssetAdapter {

    companion object {

        private val SYNTAX: Syntax = Syntax.create(HtmlAdapter::class.java.getResource("/html.rsn").readText())

    }

    override fun canParse(path: Path, mimetype: String?): Boolean = mimetype == "text/html"
            || path.endsWith(".html") || path.endsWith(".htm") || path.endsWith(".xhtml")

    override fun parse(key: String, data: ByteArray, deps: AssetResolver): Asset {
        val astTokens = SYNTAX.parse(data.toString(StandardCharsets.UTF_8)).children
        var contextStack = ContextStackNode(RenderableContextModel())
        for (token in astTokens) {
            when (token.name) {
                "literal" -> contextStack.children.add(RenderableLiteral((token as TokenNode).content))
                "block" -> {
                    val blockToken = token as TokenContainer
                    val op = blockToken.childNode(0)
                    when (op) {
                        "=" -> contextStack.children.add(RenderableReference(blockToken.childNode(1)))
                        "~" -> contextStack.children.add(RenderableSilentReference(blockToken.childNode(1)))
                        ":" -> RenderableContextModel().let {
                            contextStack.bindings.put(blockToken.childNode(1), it)
                            contextStack = contextStack.push(it)
                        }
                        "/" -> contextStack = contextStack.pop()
                        "+" -> contextStack.children.add(RenderableImport(blockToken.childNode(1)))
                        else -> throw RenderingStateException("Invalid operator '$op' (${token.line}:${token.pos})")
                    }
                }
                else -> throw IllegalStateException(token.name)
            }
        }
        return BaseAsset(key, contextStack.getContextAsRoot().bake())
    }

}
