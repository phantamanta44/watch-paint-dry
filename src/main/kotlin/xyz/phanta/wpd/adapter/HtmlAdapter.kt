package xyz.phanta.wpd.adapter

import io.github.phantamanta44.resyn.parser.Syntax
import io.github.phantamanta44.resyn.parser.token.TokenContainer
import io.github.phantamanta44.resyn.parser.token.TokenNode
import xyz.phanta.wpd.renderer.*
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class HtmlAdapter : PageAdapter {

    companion object {

        private val SYNTAX: Syntax = Syntax.create(HtmlAdapter::class.java.getResource("/html.rsn").readText())

    }

    override fun canParse(path: Path, mimetype: String?): Boolean = mimetype == "text/html"
            || path.endsWith(".html") || path.endsWith(".htm") || path.endsWith(".xhtml")

    override fun parse(data: ByteArray, deps: PageResolver): PageDefinition {
        val astTokens = SYNTAX.parse(data.toString(StandardCharsets.UTF_8)).children
        val body = mutableListOf<PageToken>()
        val defs = mutableMapOf<String, PageToken>()
        var activeDef: String? = null
        var defBody: MutableList<PageToken>? = null
        for (token in astTokens) {
            when (token.name) {
                "literal" -> (defBody ?: body).add(LiteralToken((token as TokenNode).content))
                "block" -> {
                    val blockToken = (token as TokenContainer)
                    val op = (blockToken.children[0] as TokenNode).content
                    when (op) {
                        "=" -> (defBody ?: body).add(ResolvableToken((blockToken.children[1] as TokenNode).content))
                        ":" -> {
                            if (activeDef != null) {
                                throw IllegalStateException("Already in def block (${token.line}:${token.pos})")
                            }
                            activeDef = (blockToken.children[1] as TokenNode).content
                            defBody = mutableListOf()
                        }
                        "/" -> {
                            if (activeDef == null) {
                                throw IllegalStateException("Not in def block (${token.line}:${token.pos})")
                            }
                            defs[activeDef] = ParentToken(defBody!!)
                            activeDef = null
                            defBody = null
                        }
                        "+" -> {
                            (defBody ?: body).add(ImportableToken((blockToken.children[1] as TokenNode).content))
                        }
                        else -> throw IllegalStateException("Invalid operator '$op' (${token.line}:${token.pos})")
                    }
                }
                else -> throw IllegalStateException(token.name)
            }
        }
        return SimplePageDefinition(ParentToken(body), defs)
    }

}
